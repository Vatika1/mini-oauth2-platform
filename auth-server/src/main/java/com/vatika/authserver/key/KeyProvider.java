package com.vatika.authserver.key;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.vatika.authserver.config.SigningProperties;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class KeyProvider {

    private final Map<String, RSAKey> keys = new HashMap<>();
    private final SigningProperties properties;

    public KeyProvider(SigningProperties properties) throws Exception {
        this.properties = properties;
        RSAKey key1 = generateRsaKey("key-1");
        RSAKey key2 = generateRsaKey("key-2");

        keys.put(key1.getKeyID(), key1);
        keys.put(key2.getKeyID(), key2);
        if (!keys.containsKey(properties.activeKeyId())) {
            throw new IllegalStateException("Unknown active key id: " + properties.activeKeyId());
        }
    }

    private RSAKey generateRsaKey(String kid) throws Exception {
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

    public RSAKey activeSigningKey(){
        return keys.get(properties.activeKeyId());
    }

    public JWKSet publicJwkSet(){
        List<JWK> publicKeys  = new ArrayList<>();

        for(RSAKey key: keys.values()){
            JWK obj = key.toPublicJWK();
            publicKeys.add(obj);
        }
        return new JWKSet(publicKeys);
    }
}
