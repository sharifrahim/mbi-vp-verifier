package com.zid.verifier;

import com.zid.verifier.config.TokenConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application class for ZID Verifier service.
 * 
 * This application provides OAuth2 token management capabilities and VP (Verifiable Presentation)
 * verification services. It includes automatic token refresh, Redis caching, and distributed
 * token management for multi-instance deployments.
 * 
 * Key Features:
 * - OAuth2 token lifecycle management with automatic refresh
 * - Redis-based distributed token storage and locking
 * - VP verification through external provider APIs
 * - Scheduled token refresh to prevent expiration
 * - Health monitoring and metrics
 * 
 * @author Sharif Rahim
 * @see <a href="https://github.com/sharifrahim">GitHub Profile</a>
 * @since 1.0.0
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(TokenConfigurationProperties.class)
public class ZidVerifierApplication {

	/**
	 * Main entry point for the ZID Verifier application.
	 * 
	 * Initializes the Spring Boot context with token management, scheduling,
	 * and configuration property binding enabled.
	 * 
	 * @param args command line arguments passed to the application
	 */
	public static void main(String[] args) {
		SpringApplication.run(ZidVerifierApplication.class, args);
	}

}