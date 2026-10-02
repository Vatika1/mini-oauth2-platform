package com.vatika.authserver.token;

import com.nimbusds.jose.JOSEException;
import com.vatika.authserver.config.SigningProperties;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping
public class TokenController {

    private final TokenService tokenService;
    private final SigningProperties properties;

    public TokenController(TokenService tokenService, SigningProperties properties) {
        this.tokenService = tokenService;
        this.properties = properties;
    }

    @PostMapping("/token")
    public TokenResponse issueToken(@RequestParam("sub") @NotBlank String sub) throws JOSEException {
        String token = tokenService.issueToken(sub);
        return new TokenResponse(token, "Bearer", properties.tokenTtl().toSeconds());
    }


}
