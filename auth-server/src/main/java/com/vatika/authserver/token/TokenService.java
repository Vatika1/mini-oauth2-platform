package com.vatika.authserver.token;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.vatika.authserver.config.SigningProperties;
import com.vatika.authserver.key.KeyProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
public class TokenService {

    private final KeyProvider keyProvider;
    private final SigningProperties properties;

    public TokenService(KeyProvider keyProvider, SigningProperties properties) {
        this.keyProvider = keyProvider;
        this.properties = properties;
    }

    public String issueToken(String subject) throws JOSEException {
        RSAKey activeKey = keyProvider.activeSigningKey();
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer(properties.issuer())
                .audience(properties.audience())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(properties.tokenTtl())))
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(activeKey.getKeyID())
                .build();

        SignedJWT signedJWT = new SignedJWT(header, claims);
        signedJWT.sign(new RSASSASigner(activeKey.toPrivateKey()));

        return signedJWT.serialize();
    }
}
