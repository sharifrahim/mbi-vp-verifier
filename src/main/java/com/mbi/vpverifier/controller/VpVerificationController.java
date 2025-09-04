package com.mbi.vpverifier.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/verify")
public class VpVerificationController {
    
    private static final Logger logger = LoggerFactory.getLogger(VpVerificationController.class);
    
    private final RestTemplate restTemplate;
    
    @Value("${provider.base-url}")
    private String providerBaseUrl;
    
    public VpVerificationController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
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