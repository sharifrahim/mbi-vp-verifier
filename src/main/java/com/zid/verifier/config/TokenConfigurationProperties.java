package com.zid.verifier.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Configuration properties for OAuth2 token management system.
 * 
 * This record-based configuration class binds to properties prefixed with "token"
 * and provides type-safe access to all token management settings including:
 * - OAuth2 client credentials
 * - Provider endpoint URLs
 * - Token refresh timing and buffer settings
 * - Redis storage configuration
 * - Retry and scheduling parameters
 * 
 * All properties are validated using Bean Validation annotations to ensure
 * proper configuration at application startup.
 * 
 * @author Sharif Rahim
 * @see <a href="https://github.com/sharifrahim">GitHub Profile</a>
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "token")
@Validated
public record TokenConfigurationProperties(
    @Valid @NotNull Provider provider
) {
    
    public record Provider(
        @NotBlank String clientId,
        @NotBlank String clientSecret,
        @NotBlank String createTokenUrl,
        @NotBlank String refreshTokenUrl,
        @Min(1) int accessTokenBufferMinutes,
        @Min(1) int refreshTokenBufferDays,
        @Valid @NotNull Redis redis,
        @Valid @NotNull Retry retry,
        @Valid @NotNull Schedule schedule
    ) {}
    
    public record Redis(
        @NotBlank String keyPrefix,
        @NotBlank String accessTokenKey,
        @NotBlank String refreshTokenKey
    ) {}
    
    public record Retry(
        @Min(1) int maxAttempts,
        @Min(100) long backoffDelayMs,
        @Min(1000) long maxBackoffDelayMs
    ) {}
    
    public record Schedule(
        @NotBlank String accessToken,
        @NotBlank String refreshToken
    ) {}
}