package com.zid.verifier.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * REST controller for Verifiable Presentation (VP) verification operations.
 * 
 * This controller provides endpoints for verifying VPs through an external provider service.
 * It automatically handles OAuth2 authentication via the configured TokenInterceptor, ensuring
 * all requests are properly authenticated with valid bearer tokens.
 * 
 * The controller acts as a proxy to the external VP verification service, forwarding
 * verification requests and returning the provider's response.
 * 
 * @author Sharif Rahim
 * @see <a href="https://github.com/sharifrahim">GitHub Profile</a>
 * @since 1.0.0
 */
@RestController
@RequestMapping("/verify")
public class VpVerificationController {
    
    private static final Logger logger = LoggerFactory.getLogger(VpVerificationController.class);
    
    /** RestTemplate with TokenInterceptor for authenticated requests to the provider */
    private final RestTemplate restTemplate;
    
    /** Base URL of the VP verification provider service */
    @Value("${provider.base-url}")
    private String providerBaseUrl;
    
    /**
     * Constructs a new VpVerificationController with the required dependencies.
     * 
     * @param restTemplate RestTemplate configured with token authentication interceptor
     */
    public VpVerificationController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Verifies a Verifiable Presentation (VP) by forwarding the request to the external provider.
     * 
     * This endpoint acts as a proxy to the external VP verification service. The request
     * is automatically authenticated using OAuth2 tokens managed by the TokenInterceptor.
     * 
     * @param vpId the unique identifier of the VP to verify (e.g., "v2-93aec186-0f8d-4e54-953c-f1fae97b0a9b")
     * @return ResponseEntity containing the verification result from the provider service
     *         Returns HTTP 500 with error message if verification fails
     */
    @GetMapping("/{vpId}")
    public ResponseEntity<String> verifyVp(@PathVariable String vpId) {
        try {
            logger.info("Verifying VP with ID: {}", vpId);
            
            String providerUrl = providerBaseUrl + "/v1/vp/verify/" + vpId;
            logger.debug("Calling provider URL: {}", providerUrl);
            
            // RestTemplate will automatically add Authorization header via TokenInterceptor
            ResponseEntity<String> response = restTemplate.getForEntity(providerUrl, String.class);
            
            logger.info("VP verification completed for ID: {}", vpId);
            return response;
            
        } catch (Exception e) {
            logger.error("VP verification failed for ID: {}", vpId, e);
            return ResponseEntity.internalServerError()
                .body("VP verification failed: " + e.getMessage());
        }
    }
}