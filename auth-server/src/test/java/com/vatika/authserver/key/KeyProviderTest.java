package com.vatika.authserver.key;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.vatika.authserver.config.SigningProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class KeyProviderTest {
    private SigningProperties signingProperties;
    private KeyProvider keyProvider;

    @BeforeEach
    void setUp() throws Exception {
        signingProperties = new SigningProperties(
                "auth-server",
                "resource-server",
                Duration.ofMinutes(5),
                "key-1");

        keyProvider = new KeyProvider(signingProperties);
    }

    @Test
    void activeSigningKey_returnsConfiguredKid(){
        RSAKey key = keyProvider.activeSigningKey();
        assertEquals(signingProperties.activeKeyId(), key.getKeyID());
    }

    @Test
    void activeSigningKey_hasPrivateHalf(){
        RSAKey key = keyProvider.activeSigningKey();
        assertTrue(key.isPrivate());
    }

    @Test
    void publicJwkSet_containsBothKeys() throws JOSEException {
        JWKSet jwkSet = keyProvider.publicJwkSet();
        assertEquals(2, jwkSet.getKeys().size());
        assertNotNull(keyProvider.publicJwkSet().getKeyByKeyId("key-1"));
        assertNotNull(keyProvider.publicJwkSet().getKeyByKeyId("key-2"));
    }

    @Test
    void publicJwkSet_hasNoPrivateMaterial(){
        JWKSet jwkSet = keyProvider.publicJwkSet();
        for(JWK jwk: jwkSet.getKeys()){
            assertFalse(jwk.isPrivate());
        }
    }

    @Test
    void constructor_failsOnUnknownActiveKeyId(){
        SigningProperties badProperties = new SigningProperties(
                "auth-server",
                "resource-server",
                Duration.ofMinutes(5),
                "key-3");

        assertThrows(IllegalStateException.class, ()-> new KeyProvider(badProperties));
    }
}
