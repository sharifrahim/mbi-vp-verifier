package com.zid.verifier.interceptor;

import com.zid.verifier.service.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * HTTP request interceptor that automatically adds OAuth2 Bearer tokens to outbound requests.
 * 
 * This interceptor integrates with the RestTemplate to provide transparent OAuth2 authentication
 * for all HTTP requests. It automatically:
 * - Adds the Authorization header with a valid Bearer token
 * - Handles 401 Unauthorized responses by refreshing tokens and retrying
 * - Excludes token provider URLs to prevent circular authentication calls
 * 
 * The interceptor works in conjunction with the TokenService to ensure tokens are always
 * valid and handles token refresh transparently when authentication fails.
 * 
 * @author Sharif Rahim
 * @see <a href="https://github.com/sharifrahim">GitHub Profile</a>
 * @since 1.0.0
 */
@Component
public class TokenInterceptor implements ClientHttpRequestInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenInterceptor.class);
    
    /** HTTP header name for authorization */
    private static final String AUTHORIZATION_HEADER = "Authorization";
    
    /** Bearer token prefix for OAuth2 */
    private static final String BEARER_PREFIX = "Bearer ";
    
    private final TokenService tokenService;
    
    /**
     * Constructs a new TokenInterceptor with the required token service.
     * 
     * @param tokenService service for managing OAuth2 token lifecycle
     */
    public TokenInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }
    
    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, 
            byte[] body, 
            ClientHttpRequestExecution execution) throws IOException {
        
        // Skip token injection for token provider URLs to avoid circular calls
        if (isTokenProviderUrl(request.getURI().toString())) {
            logger.debug("Skipping token injection for token provider URL: {}", request.getURI());
            return execution.execute(request, body);
        }
        
        // Add authorization header
        String accessToken = tokenService.getAccessToken();
        request.getHeaders().set(AUTHORIZATION_HEADER, BEARER_PREFIX + accessToken);
        
        logger.debug("Added authorization header to request: {}", request.getURI());
        
        // Execute request
        ClientHttpResponse response = execution.execute(request, body);
        
        // Handle 401 Unauthorized - attempt token refresh and retry once
        if (response.getStatusCode().value() == 401) {
            logger.warn("Received 401 Unauthorized, attempting token refresh and retry");
            response.close(); // Close the failed response
            
            try {
                // Refresh tokens
                tokenService.refreshTokens();
                
                // Get new token and retry
                String newAccessToken = tokenService.getAccessToken();
                request.getHeaders().set(AUTHORIZATION_HEADER, BEARER_PREFIX + newAccessToken);
                
                logger.debug("Retrying request with new token: {}", request.getURI());
                return execution.execute(request, body);
                
            } catch (Exception e) {
                logger.error("Failed to refresh token for retry", e);
                // Return the original 401 response
                return execution.execute(request, body);
            }
        }
        
        return response;
    }
    
    private boolean isTokenProviderUrl(String url) {
        // This should be configured based on your token provider URLs
        return url.contains("/v1/token/create") || url.contains("/v1/token/refresh");
    }
}