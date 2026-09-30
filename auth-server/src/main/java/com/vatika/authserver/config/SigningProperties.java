package com.vatika.authserver.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "auth.signing")
public record SigningProperties(
        @NotBlank String issuer,
        @NotBlank String audience,
        @NotNull Duration tokenTtl,
        @NotBlank String activeKeyId
) {}