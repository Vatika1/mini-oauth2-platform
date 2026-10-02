package com.vatika.resourceserver.hello;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@SpringBootTest
public class HelloSecurityTest {
    static MockWebServer mockWebServer;

    @AfterAll
    static void stopServer() throws IOException {
        mockWebServer.shutdown();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> mockWebServer.url("/.well-known/jwks.json").toString()
        );
    }

    @Autowired
    MockMvc mockMvc;

    static RSAKey key1;
    static RSAKey key2;
    static RSAKey strangerKey;

    @BeforeAll
    static void setup() throws Exception {
        key1 = generateRsaKey("key-1");
        key2 = generateRsaKey("key-2");
        strangerKey = generateRsaKey("key-3");

        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String jwksJson = new JWKSet(List.of(key1.toPublicJWK(), key2.toPublicJWK())).toString();

        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(jwksJson);
            }
        });
    }

    private static RSAKey generateRsaKey(String kid) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);

        KeyPair keyPair = generator.generateKeyPair();

        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(kid)
                .algorithm(JWSAlgorithm.RS256)
                .keyUse(KeyUse.SIGNATURE)
                .build();
    }

    private String mintToken(RSAKey signingKey, String kid, String issuer,
                             String audience, Instant expiresAt) throws Exception {

        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("alice")
                .issuer(issuer)
                .audience(audience)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiresAt))
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(kid)
                .build();

        SignedJWT signedJWT = new SignedJWT(header, claims);
        signedJWT.sign(new RSASSASigner(signingKey.toPrivateKey()));

        return signedJWT.serialize();
    }

    @Test
    void hello_noToken_returns401() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void hello_tokenSignedWithKey1_returns200() throws Exception {
        String token = mintToken(key1, key1.getKeyID(), "http://localhost:9000",
                "hello-api", Instant.now().plusSeconds(300));
        mockMvc.perform(get("/hello").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello, alice"));
    }

    @Test
    void hello_tokenSignedWithKey2_returns200() throws Exception {
        String token = mintToken(key2, key2.getKeyID(), "http://localhost:9000",
                "hello-api", Instant.now().plusSeconds(300));
        mockMvc.perform(get("/hello").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello, alice"));
    }

    @Test
    void hello_expiredToken_returns401() throws Exception {
        String token = mintToken(key2, key2.getKeyID(), "http://localhost:9000",
                "hello-api", Instant.now().minusSeconds(300));
        mockMvc.perform(get("/hello").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void hello_wrongIssuer_returns401() throws Exception {
        String token = mintToken(key1, key1.getKeyID(), "http://localhost:9999",
                "hello-api", Instant.now().plusSeconds(300));
        mockMvc.perform(get("/hello").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void hello_wrongAudience_returns401() throws Exception {
        String token = mintToken(key1, key1.getKeyID(), "http://localhost:9000",
                "wrong-audience", Instant.now().plusSeconds(300));
        mockMvc.perform(get("/hello").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void hello_tamperedSignature_returns401() throws Exception {
        String token = mintToken(strangerKey, key1.getKeyID(), "http://localhost:9000",
                "hello-api", Instant.now().plusSeconds(300));
        mockMvc.perform(get("/hello").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void hello_unknownKid_returns401() throws Exception {
        String token = mintToken(key1, strangerKey.getKeyID(), "http://localhost:9000",
                "hello-api", Instant.now().plusSeconds(300));
        mockMvc.perform(get("/hello").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }
}
