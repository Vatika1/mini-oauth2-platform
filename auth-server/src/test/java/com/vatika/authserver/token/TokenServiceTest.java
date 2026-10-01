package com.vatika.authserver.token;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import com.vatika.authserver.config.SigningProperties;
import com.vatika.authserver.key.KeyProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Duration;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class TokenServiceTest {

    private TokenService tokenService;
    private KeyProvider keyProvider;
    private SigningProperties signingProperties;

    @BeforeEach
    void setUp() throws Exception {
        signingProperties = new SigningProperties(
                "auth-server",
                "resource-server",
                Duration.ofMinutes(5),
                "key-1");

        keyProvider = new KeyProvider(signingProperties);
        tokenService = new TokenService(keyProvider, signingProperties);
    }

    @Test
    void issueToken_containsExpectedClaims() throws JOSEException, ParseException {
        String token = tokenService.issueToken("Alice");
        SignedJWT jwt = SignedJWT.parse(token);

        assertEquals("Alice", jwt.getJWTClaimsSet().getSubject());
        assertEquals("auth-server", jwt.getJWTClaimsSet().getIssuer());
        assertTrue(jwt.getJWTClaimsSet().getAudience().contains("resource-server"));
    }

    @Test
    void issueToken_expiresAfterConfiguredTtl() throws JOSEException, ParseException {
        String token = tokenService.issueToken("Alice");
        SignedJWT jwt = SignedJWT.parse(token);

        Date iat = jwt.getJWTClaimsSet().getIssueTime();
        Date exp = jwt.getJWTClaimsSet().getExpirationTime();
        long seconds = (exp.getTime() - iat.getTime()) / 1000;

        assertEquals(signingProperties.tokenTtl().toSeconds(), seconds);
    }

    @Test
    void issueToken_headerHasActiveKidAndRs256() throws JOSEException, ParseException {
        String token = tokenService.issueToken("Alice");
        SignedJWT jwt = SignedJWT.parse(token);
        String expectedActiveKeyId = signingProperties.activeKeyId();
        String headerKey = jwt.getHeader().getKeyID();

        assertEquals(expectedActiveKeyId, headerKey);
        assertEquals(JWSAlgorithm.RS256, jwt.getHeader().getAlgorithm());
    }

    @Test
    void issueToken_signatureVerifiesWithActivePublicKey() throws JOSEException, ParseException {
        String token = tokenService.issueToken("Alice");
        RSAKey activeKey = keyProvider.activeSigningKey();
        RSAPublicKey publicKey = activeKey.toRSAPublicKey();
        JWSVerifier verifier = new RSASSAVerifier(publicKey);
        SignedJWT jwt = SignedJWT.parse(token);
        boolean valid = jwt.verify(verifier);

        assertTrue(valid);
    }

    @Test
    void issueToken_signatureFailsWithOtherKey() throws JOSEException, ParseException {
        String token = tokenService.issueToken("Alice");
        SignedJWT jwt = SignedJWT.parse(token);
        RSAKey key2 = (RSAKey) keyProvider.publicJwkSet().getKeyByKeyId("key-2");
        RSASSAVerifier verifier = new RSASSAVerifier(key2.toRSAPublicKey());
        assertFalse(jwt.verify(new RSASSAVerifier(key2.toRSAPublicKey())));
    }
}
