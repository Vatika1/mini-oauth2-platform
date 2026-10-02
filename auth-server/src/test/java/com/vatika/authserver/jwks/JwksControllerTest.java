package com.vatika.authserver.jwks;

import com.vatika.authserver.config.SigningProperties;
import com.vatika.authserver.key.KeyProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(KeyProvider.class)
@EnableConfigurationProperties(SigningProperties.class)
@WebMvcTest(JwksController.class)
public class JwksControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void jwks_returnsBothKeys() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys", hasSize(2)));
    }

    @Test
    void jwks_keyIdsAreKey1AndKey2() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[*].kid", containsInAnyOrder("key-1", "key-2")));
    }

    @Test
    void jwks_keysHaveRfcFields() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[*].kty", everyItem(is("RSA"))))
                .andExpect(jsonPath("$.keys[*].alg", everyItem(is("RS256"))))
                .andExpect(jsonPath("$.keys[*].use", everyItem(is("sig"))))
                .andExpect(jsonPath("$.keys[*].n", everyItem(notNullValue())))
                .andExpect(jsonPath("$.keys[*].e", everyItem(notNullValue())));
    }

    @Test
    void jwks_hasNoPrivateMaterial() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[*].d").doesNotExist());
    }

}
