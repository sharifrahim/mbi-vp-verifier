# Token Management System

This application includes a comprehensive token management system that automatically handles OAuth2 token lifecycle for API authentication.

## Features

- ✅ Automatic token acquisition and refresh
- ✅ Redis-based distributed token storage
- ✅ Distributed locking to prevent race conditions
- ✅ Retry mechanism with exponential backoff
- ✅ Health checks for token and Redis status
- ✅ Scheduled token renewal
- ✅ REST client interceptor for automatic authentication
- ✅ Configurable token expiration buffers

## Configuration

### 1. Environment Variables

Set the following environment variables:

```bash
TOKEN_CLIENT_ID=your-actual-client-id
TOKEN_CLIENT_SECRET=your-actual-client-secret
TOKEN_CREATE_URL=https://your-provider.com/oauth2/token
TOKEN_REFRESH_URL=https://your-provider.com/oauth2/refresh
```

### 2. Redis Setup

Ensure Redis is running on localhost:6379 or update the configuration in `application.yml`:

```yaml
spring:
  data:
    redis:
      host: your-redis-host
      port: 6379
      password: your-redis-password  # if needed
```

### 3. Application Properties

The `application.yml` contains all token management configurations:

- **Buffer Settings**: Refresh tokens before they expire
- **Retry Settings**: Configure retry attempts and backoff
- **Schedule Settings**: Automated token refresh intervals
- **Redis Settings**: Key prefixes and storage configuration

## How It Works

### 1. Token Service (`TokenService.java`)

- **Initialization**: Gets tokens on startup
- **Token Retrieval**: Provides access tokens to other services
- **Token Refresh**: Handles both access and refresh token renewal
- **Distributed Locking**: Prevents multiple instances from refreshing simultaneously
- **Redis Storage**: Stores tokens with automatic TTL

### 2. Token Interceptor (`TokenInterceptor.java`)

- **Automatic Authentication**: Adds Bearer token to all REST calls
- **401 Handling**: Automatically retries failed requests after token refresh
- **Provider URL Exclusion**: Skips token injection for OAuth endpoints

### 3. Token Scheduler (`TokenRefreshScheduler.java`)

- **Access Token Refresh**: Every 10 minutes (configurable)
- **Refresh Token Renewal**: Every 5 days (configurable)
- **Proactive Renewal**: Prevents token expiration

### 4. Health Monitoring (`TokenHealthIndicator.java`)

- **Health Endpoint**: `/actuator/health`
- **Redis Connectivity**: Checks Redis connection
- **Token Availability**: Verifies access token presence

## Usage

### Using RestTemplate (Automatic)

```java
@Service
public class MyService {
    
    private final RestTemplate restTemplate;
    
    public MyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate; // Pre-configured with token interceptor
    }
    
    public String callExternalApi() {
        // Token automatically added by TokenInterceptor
        return restTemplate.getForObject("https://external-api.com/data", String.class);
    }
}
```

### Manual Token Retrieval

```java
@Service
public class MyService {
    
    private final TokenService tokenService;
    
    public MyService(TokenService tokenService) {
        this.tokenService = tokenService;
    }
    
    public void makeApiCall() {
        String accessToken = tokenService.getAccessToken();
        // Use token manually
    }
}
```

## Monitoring

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "tokenHealthIndicator": {
      "status": "UP",
      "details": {
        "redis": "UP",
        "accessToken": "AVAILABLE"
      }
    }
  }
}
```

### Metrics

Access application metrics at:
```bash
curl http://localhost:8080/actuator/metrics
```

## Security Considerations

1. **Environment Variables**: Store client credentials as environment variables
2. **Redis Security**: Use Redis AUTH and TLS in production
3. **Log Safety**: Tokens are never logged in plain text
4. **Network Security**: Ensure HTTPS for all token provider communications

## Troubleshooting

### Common Issues

1. **Redis Connection Failed**
   - Verify Redis is running
   - Check connection settings in application.yml

2. **Token Provider Unreachable**
   - Verify provider URLs are correct
   - Check network connectivity
   - Review retry configuration

3. **Invalid Credentials**
   - Verify CLIENT_ID and CLIENT_SECRET environment variables
   - Check provider configuration

### Logs

Monitor token operations:
```bash
# Enable debug logging
logging.level.com.mbi.vpverifier.token: DEBUG
```

Key log patterns:
- `Token initialization started`
- `Tokens stored in Redis with TTL`
- `Scheduled access token refresh completed`
- `Received 401 Unauthorized, attempting token refresh`

## Development

### Running Locally

1. Start Redis:
   ```bash
   docker run -d -p 6379:6379 redis:alpine
   ```

2. Set environment variables:
   ```bash
   export TOKEN_CLIENT_ID=test-client
   export TOKEN_CLIENT_SECRET=test-secret
   ```

3. Run application:
   ```bash
   mvn spring-boot:run
   ```

### Testing

The system includes automatic token refresh and retry mechanisms. Test scenarios:

- Token expiration handling
- Redis connectivity issues
- Provider API downtime
- Concurrent token refresh requests

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Application   │────│  TokenService   │────│   Redis Cache   │
│                 │    │                 │    │                 │
│                 │    │  - Get Token    │    │  - Access Token │
│                 │    │  - Refresh      │    │  - Refresh Token│
│                 │    │  - Initialize   │    │  - TTL Management│
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │
         │                       │
         ▼                       ▼
┌─────────────────┐    ┌─────────────────┐
│ TokenInterceptor│    │ TokenScheduler  │
│                 │    │                 │
│ - Auto Auth     │    │ - Scheduled     │
│ - 401 Handling  │    │   Refresh       │
│ - Retry Logic   │    │ - Proactive     │
└─────────────────┘    └─────────────────┘
```