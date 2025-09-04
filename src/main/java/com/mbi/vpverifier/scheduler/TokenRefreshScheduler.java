package com.mbi.vpverifier.scheduler;

import com.mbi.vpverifier.service.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled component responsible for proactive OAuth2 token refresh operations.
 * 
 * This scheduler ensures tokens are refreshed before they expire by running
 * automated refresh tasks at configured intervals:
 * - Access tokens are refreshed every 10 minutes (configurable)
 * - Refresh tokens are renewed every 5 days (configurable)
 * 
 * The scheduler prevents token expiration during normal application operation
 * and reduces the likelihood of authentication failures during peak usage.
 * 
 * @author Sharif Rahim
 * @see <a href="https://github.com/sharifrahim">GitHub Profile</a>
 * @since 1.0.0
 */
@Component
public class TokenRefreshScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenRefreshScheduler.class);
    
    private final TokenService tokenService;
    
    /**
     * Constructs a new TokenRefreshScheduler with the required token service.
     * 
     * @param tokenService service for managing OAuth2 token lifecycle
     */
    public TokenRefreshScheduler(TokenService tokenService) {
        this.tokenService = tokenService;
    }
    
    @Scheduled(cron = "${token.provider.schedule.access-token}")
    public void refreshAccessToken() {
        try {
            logger.debug("Scheduled access token refresh started");
            tokenService.refreshTokens();
            logger.info("Scheduled access token refresh completed successfully");
        } catch (Exception e) {
            logger.error("Scheduled access token refresh failed", e);
        }
    }
    
    @Scheduled(cron = "${token.provider.schedule.refresh-token}")
    public void refreshRefreshToken() {
        try {
            logger.debug("Scheduled refresh token renewal started");
            // Force a complete token refresh by clearing existing tokens
            // This will trigger a new token request which should include a new refresh token
            tokenService.refreshTokens();
            logger.info("Scheduled refresh token renewal completed successfully");
        } catch (Exception e) {
            logger.error("Scheduled refresh token renewal failed", e);
        }
    }
}