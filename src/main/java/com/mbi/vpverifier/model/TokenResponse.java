package com.mbi.vpverifier.model;

import com.fasterxml.jackson.annotation.JsonProperty;

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