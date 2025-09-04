package com.zid.verifier.controller;

import com.zid.verifier.service.TokenService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/test/token")
public class TokenTestController {
    
    private final TokenService tokenService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    public TokenTestController(TokenService tokenService, RedisTemplate<String, Object> redisTemplate) {
        this.tokenService = tokenService;
        this.redisTemplate = redisTemplate;
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getTokenStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            String accessToken = tokenService.getAccessToken();
            status.put("accessTokenAvailable", accessToken != null);
            status.put("accessTokenLength", accessToken != null ? accessToken.length() : 0);
            
            // Check Redis keys
            Set<String> keys = redisTemplate.keys("mbi:vp:token:*");
            status.put("redisKeysFound", keys != null ? keys.size() : 0);
            status.put("redisKeys", keys);
            
            // Check TTL
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    Long ttl = redisTemplate.getExpire(key);
                    status.put(key + "_ttl_seconds", ttl);
                }
            }
            
            status.put("status", "SUCCESS");
        } catch (Exception e) {
            status.put("status", "ERROR");
            status.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(status);
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> forceRefresh() {
        Map<String, String> result = new HashMap<>();
        
        try {
            tokenService.refreshTokens();
            result.put("status", "SUCCESS");
            result.put("message", "Token refresh completed");
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
}