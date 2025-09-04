package com.zid.verifier.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class RetryUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryUtils.class);
    
    public static <T> T executeWithRetry(
            Supplier<T> operation,
            int maxAttempts,
            long initialDelayMs,
            long maxDelayMs,
            String operationName) {
        
        Exception lastException = null;
        long currentDelay = initialDelayMs;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                T result = operation.get();
                if (attempt > 1) {
                    logger.info("Operation '{}' succeeded on attempt {}", operationName, attempt);
                }
                return result;
            } catch (Exception e) {
                lastException = e;
                logger.warn("Operation '{}' failed on attempt {} of {}: {}", 
                    operationName, attempt, maxAttempts, e.getMessage());
                
                if (attempt < maxAttempts) {
                    try {
                        logger.debug("Waiting {}ms before retry...", currentDelay);
                        Thread.sleep(currentDelay);
                        
                        // Exponential backoff with max cap
                        currentDelay = Math.min(currentDelay * 2, maxDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry backoff", ie);
                    }
                }
            }
        }
        
        logger.error("Operation '{}' failed after {} attempts", operationName, maxAttempts);
        throw new RuntimeException("Operation failed after " + maxAttempts + " attempts", lastException);
    }
}