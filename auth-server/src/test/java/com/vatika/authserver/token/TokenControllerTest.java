package com.vatika.authserver.token;

import com.nimbusds.jose.JOSEException;
import com.vatika.authserver.config.SigningProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnableConfigurationProperties(SigningProperties.class)
@WebMvcTest(TokenController.class)
public class TokenControllerTest {

    @MockitoBean
    private TokenService tokenService;

    @Autowired
    private MockMvc mockMvc;


    @Test
    void issueToken_validSub_returns200WithTokenResponse() throws Exception{
        when(tokenService.issueToken(eq("alice")))
                .thenReturn("someToken");

        mockMvc.perform(
                        post("/token")
                                .param("sub", "alice")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("someToken"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(300));

    }

    @Test
    void issueToken_blankSub_returns400() throws Exception{
        mockMvc.perform(
                        post("/token")
                                .param("sub", "")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void issueToken_missingSub_returns400() throws Exception{
        mockMvc.perform(
                        post("/token")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void issueToken_signingFails_returns500() throws Exception{
        when(tokenService.issueToken(any())).thenThrow(new JOSEException("boom"));
        mockMvc.perform(
                        post("/token")
                                .param("sub", "alice")
                )
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));

    }
}
