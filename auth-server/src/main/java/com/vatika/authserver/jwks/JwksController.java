package com.vatika.authserver.jwks;

import com.vatika.authserver.key.KeyProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


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
