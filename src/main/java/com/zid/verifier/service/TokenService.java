package com.zid.verifier.service;

import com.zid.verifier.config.TokenConfigurationProperties;
import com.zid.verifier.exception.TokenException;
import com.zid.verifier.model.TokenResponse;
import com.zid.verifier.util.RetryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for OAuth2 token lifecycle management with Redis-based distributed storage.
 * 
 * This service handles:
 * - Token acquisition from the OAuth2 provider
 * - Automatic token refresh before expiration
 * - Redis-based token storage with TTL
 * - Distributed locking to prevent concurrent refresh operations
 * - Retry mechanisms with exponential backoff
 * - Token validation and error handling
 * 
 * The service is designed for multi-instance deployments with Redis as the shared token store.
 * It uses distributed locking to ensure only one instance refreshes tokens at a time.
 * 
 * @author Sharif Rahim
 * @see <a href="https://github.com/sharifrahim">GitHub Profile</a>
 * @since 1.0.0
 */
@Service
public class TokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);
    
    /** Suffix for Redis lock keys to prevent concurrent token refresh operations */
    private static final String LOCK_SUFFIX = "lock";
    
    /** Timeout in seconds for acquiring distributed lock */
    private static final int LOCK_TIMEOUT_SECONDS = 30;
    
    private final TokenConfigurationProperties properties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate tokenRestTemplate;
    
    /**
     * Constructs a new TokenService with the required dependencies.
     * 
     * @param properties configuration properties for token management
     * @param redisTemplate Redis template for token storage operations
     * @param tokenRestTemplate dedicated RestTemplate for token operations (without interceptors)
     */
    public TokenService(TokenConfigurationProperties properties, 
                       RedisTemplate<String, Object> redisTemplate,
                       @Qualifier("tokenRestTemplate") RestTemplate tokenRestTemplate) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.tokenRestTemplate = tokenRestTemplate;
    }
    
    /**
     * Initializes tokens on application startup.
     * 
     * This method is called automatically after bean construction. It checks for
     * existing tokens in Redis and requests new tokens if none are found.
     * 
     * @throws TokenException if token initialization fails
     */
    @PostConstruct
    public void initializeTokens() {
        logger.info("Initializing tokens on startup");
        try {
            String accessToken = getAccessTokenFromRedis();
            if (accessToken == null) {
                logger.info("No access token found, requesting new tokens");
                refreshTokens();
            } else {
                logger.info("Access token found in Redis");
            }
        } catch (Exception e) {
            logger.error("Failed to initialize tokens", e);
            throw new TokenException("Token initialization failed", e);
        }
    }
    
    /**
     * Retrieves a valid access token from Redis or refreshes tokens if needed.
     * 
     * This method first attempts to get an access token from Redis. If no token
     * is found, it triggers a token refresh and then attempts to retrieve the
     * token again.
     * 
     * @return a valid access token string
     * @throws TokenException if unable to obtain a valid access token
     */
    public String getAccessToken() {
        String accessToken = getAccessTokenFromRedis();
        if (accessToken == null) {
            logger.warn("Access token not found in Redis, attempting refresh");
            refreshTokens();
            accessToken = getAccessTokenFromRedis();
        }
        
        if (accessToken == null) {
            throw new TokenException("Unable to obtain access token");
        }
        
        return accessToken;
    }
    
    /**
     * Refreshes OAuth2 tokens using distributed locking to prevent race conditions.
     * 
     * This method uses Redis-based distributed locking to ensure only one instance
     * can refresh tokens at a time. If a lock cannot be acquired, it waits for
     * another instance to complete the refresh operation.
     * 
     * The refresh process:
     * 1. Acquires distributed lock
     * 2. Attempts to refresh using existing refresh token
     * 3. Falls back to requesting new tokens if refresh fails
     * 4. Releases the lock
     * 
     * @throws TokenException if token refresh fails after all retry attempts
     */
    public void refreshTokens() {
        String lockKey = getLockKey();
        boolean acquired = acquireDistributedLock(lockKey);
        
        if (!acquired) {
            logger.debug("Another instance is refreshing tokens, waiting...");
            waitForTokenRefresh();
            return;
        }
        
        try {
            logger.info("Acquired lock, refreshing tokens");
            String refreshToken = getRefreshTokenFromRedis();
            
            if (refreshToken != null) {
                refreshAccessToken(refreshToken);
            } else {
                requestNewTokens();
            }
        } finally {
            releaseDistributedLock(lockKey);
        }
    }
    
    private void refreshAccessToken(String refreshToken) {
        try {
            TokenResponse tokenResponse = RetryUtils.executeWithRetry(
                () -> {
                    logger.debug("Refreshing access token using refresh token");
                    
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                    
                    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                    body.add("grant_type", "refresh_token");
                    body.add("refresh_token", refreshToken);
                    body.add("client_id", properties.provider().clientId());
                    body.add("client_secret", properties.provider().clientSecret());
                    
                    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
                    
                    ResponseEntity<TokenResponse> response = tokenRestTemplate.exchange(
                        properties.provider().refreshTokenUrl(),
                        HttpMethod.POST,
                        request,
                        TokenResponse.class
                    );
                    
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        TokenResponse responseBody = response.getBody();
                        if (responseBody.status() == 200 && responseBody.data() != null) {
                            return responseBody;
                        } else {
                            throw new TokenException("Token refresh failed: " + responseBody.message());
                        }
                    } else {
                        throw new TokenException("Token refresh failed with HTTP status: " + response.getStatusCode());
                    }
                },
                properties.provider().retry().maxAttempts(),
                properties.provider().retry().backoffDelayMs(),
                properties.provider().retry().maxBackoffDelayMs(),
                "refresh-access-token"
            );
            
            storeTokens(tokenResponse);
            logger.info("Access token refreshed successfully");
            
        } catch (Exception e) {
            logger.error("Failed to refresh access token, requesting new tokens", e);
            requestNewTokens();
        }
    }
    
    private void requestNewTokens() {
        try {
            TokenResponse tokenResponse = RetryUtils.executeWithRetry(
                () -> {
                    logger.debug("Requesting new tokens from provider");
                    
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    
                    Map<String, String> body = new HashMap<>();
                    body.put("clientId", properties.provider().clientId());
                    body.put("clientSecret", properties.provider().clientSecret());
                    
                    HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
                    
                    ResponseEntity<TokenResponse> response = tokenRestTemplate.exchange(
                        properties.provider().createTokenUrl(),
                        HttpMethod.POST,
                        request,
                        TokenResponse.class
                    );
                    
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        TokenResponse responseBody = response.getBody();
                        if (responseBody.status() == 200 && responseBody.data() != null) {
                            return responseBody;
                        } else {
                            throw new TokenException("Failed to obtain new tokens: " + responseBody.message());
                        }
                    } else {
                        throw new TokenException("Failed to obtain new tokens with HTTP status: " + response.getStatusCode());
                    }
                },
                properties.provider().retry().maxAttempts(),
                properties.provider().retry().backoffDelayMs(),
                properties.provider().retry().maxBackoffDelayMs(),
                "request-new-tokens"
            );
            
            storeTokens(tokenResponse);
            logger.info("New tokens obtained successfully");
        } catch (Exception e) {
            logger.error("Failed to request new tokens", e);
            throw new TokenException("Token request failed", e);
        }
    }
    
    private void storeTokens(TokenResponse tokenResponse) {
        String accessTokenKey = getAccessTokenKey();
        String refreshTokenKey = getRefreshTokenKey();
        
        // Calculate TTL with buffer
        long accessTokenTtl = Math.max(1, 
            tokenResponse.expiresIn() - (properties.provider().accessTokenBufferMinutes() * 60L));
        
        // Store access token with TTL
        redisTemplate.opsForValue().set(accessTokenKey, tokenResponse.accessToken(), 
            Duration.ofSeconds(accessTokenTtl));
        
        // Store refresh token (if present) with longer TTL
        if (tokenResponse.refreshToken() != null) {
            long refreshTokenTtl = Math.max(1,
                (tokenResponse.refreshExpiresIn() != null ? tokenResponse.refreshExpiresIn() : 86400 * 7) - 
                (properties.provider().refreshTokenBufferDays() * 86400L));
            
            redisTemplate.opsForValue().set(refreshTokenKey, tokenResponse.refreshToken(),
                Duration.ofSeconds(refreshTokenTtl));
        }
        
        logger.debug("Tokens stored in Redis with TTL: access={}s, refresh={}s", 
            accessTokenTtl, tokenResponse.refreshExpiresIn());
    }
    
    private String getAccessTokenFromRedis() {
        return (String) redisTemplate.opsForValue().get(getAccessTokenKey());
    }
    
    private String getRefreshTokenFromRedis() {
        return (String) redisTemplate.opsForValue().get(getRefreshTokenKey());
    }
    
    private boolean acquireDistributedLock(String lockKey) {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
            lockKey, 
            LocalDateTime.now().toString(), 
            Duration.ofSeconds(LOCK_TIMEOUT_SECONDS)
        );
        return Boolean.TRUE.equals(acquired);
    }
    
    private void releaseDistributedLock(String lockKey) {
        redisTemplate.delete(lockKey);
    }
    
    private void waitForTokenRefresh() {
        int maxWaitSeconds = 10;
        int checkIntervalMs = 500;
        int attempts = maxWaitSeconds * 1000 / checkIntervalMs;
        
        for (int i = 0; i < attempts; i++) {
            if (getAccessTokenFromRedis() != null) {
                logger.debug("Token refresh completed by another instance");
                return;
            }
            
            try {
                Thread.sleep(checkIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TokenException("Interrupted while waiting for token refresh");
            }
        }
        
        logger.warn("Timeout waiting for token refresh by another instance");
        throw new TokenException("Timeout waiting for token refresh");
    }
    
    private String getAccessTokenKey() {
        return properties.provider().redis().keyPrefix() + properties.provider().redis().accessTokenKey();
    }
    
    private String getRefreshTokenKey() {
        return properties.provider().redis().keyPrefix() + properties.provider().redis().refreshTokenKey();
    }
    
    private String getLockKey() {
        return properties.provider().redis().keyPrefix() + LOCK_SUFFIX;
    }
}