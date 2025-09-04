package com.zid.verifier.exception;

/**
 * Runtime exception thrown when OAuth2 token operations fail.
 * 
 * This exception is used throughout the token management system to indicate
 * failures in token-related operations such as:
 * - Token acquisition from the OAuth2 provider
 * - Token refresh attempts
 * - Redis storage operations
 * - Token validation failures
 * - Network connectivity issues with the token provider
 * 
 * @author Sharif Rahim
 * @see <a href="https://github.com/sharifrahim">GitHub Profile</a>
 * @since 1.0.0
 */
public class TokenException extends RuntimeException {
    
    /**
     * Constructs a new TokenException with the specified detail message.
     * 
     * @param message the detail message explaining the cause of the exception
     */
    public TokenException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new TokenException with the specified detail message and cause.
     * 
     * @param message the detail message explaining the cause of the exception
     * @param cause the underlying cause of this exception
     */
    public TokenException(String message, Throwable cause) {
        super(message, cause);
    }
}