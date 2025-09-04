package com.zid.verifier.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data model representing OAuth2 token response from the external provider.
 * 
 * This record maps the JSON response structure returned by the OAuth2 token provider.
 * It includes both access and refresh tokens along with their expiration information.
 * 
 * The response follows the provider's specific format with nested data structure.
 * Helper methods are provided for backward compatibility with standard OAuth2 response formats.
 * 
 * @author Sharif Rahim
 * @see <a href="https://github.com/sharifrahim">GitHub Profile</a>
 * @since 1.0.0
 */
public record TokenResponse(
    @JsonProperty("status") Integer status,
    @JsonProperty("message") String message,
    @JsonProperty("data") TokenData data,
    @JsonProperty("timestamp") String timestamp
) {
    public record TokenData(
        @JsonProperty("accessToken") String accessToken,
        @JsonProperty("refreshToken") String refreshToken,
        @JsonProperty("accessTokenExpiresIn") Long accessTokenExpiresIn,
        @JsonProperty("refreshTokenExpiresIn") Long refreshTokenExpiresIn
    ) {}
    
    // Helper methods to maintain compatibility with existing code
    public String accessToken() {
        return data != null ? data.accessToken() : null;
    }
    
    public String refreshToken() {
        return data != null ? data.refreshToken() : null;
    }
    
    public Long expiresIn() {
        return data != null ? data.accessTokenExpiresIn() : null;
    }
    
    public Long refreshExpiresIn() {
        return data != null ? data.refreshTokenExpiresIn() : null;
    }
}