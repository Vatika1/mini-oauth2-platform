package com.vatika.authserver.jwks;

import com.nimbusds.jose.jwk.JWKSet;
import com.vatika.authserver.key.KeyProvider;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Validated
@RequestMapping
@RestController
public class JwksController {

    private final KeyProvider keyProvider;

    public JwksController(KeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> getPublicKeys(){
        return keyProvider.publicJwkSet().toJSONObject();
    }
}
