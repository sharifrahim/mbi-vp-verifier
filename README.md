# MBI VP Verifier

A Spring Boot application for verifying Verifiable Presentations (VP) via MBI VC Service APIs.

## Features

- Verify verifiable presentations through MBI VC Service APIs
- Automatic OAuth2 token management
- Health monitoring endpoints

## Prerequisites

- Java 17 or higher
- Redis server
- Maven 3.6+

## Setup

### 1. Set Environment Variables

Make sure you have your client ID and client secret from MBI VC Provider, then set:

```bash
export TOKEN_CLIENT_ID=your-client-id
export TOKEN_CLIENT_SECRET=your-client-secret
```

### 2. Start Redis Server

```bash
# Using Docker
docker run -d -p 6379:6379 redis:alpine

# Or install locally
redis-server
```

### 3. Update Configuration

Update the provider base URL in `src/main/resources/application.yml`:

```yaml
provider:
  base-url: http://your-provider-url
```

## Running the Application

```bash
# Development
mvn spring-boot:run

# Production
mvn clean package
java -jar target/mbi-vp-verifier-0.0.1-SNAPSHOT.jar
```

## Usage

### Verify VP
```bash
GET /verify/{vpId}
```

Example:
```bash
curl http://localhost:8080/verify/v2-93aec186-0f8d-4e54-953c-f1fae97b0a9b
```

Response:
```json
{
    "status": 200,
    "message": "Success",
    "data": {
        "vcId": "did:zid:9d018aad2b5f049ee0725c55d0863a4fcb136a3e1835291174e9519834374669",
        "expiryDate": "2025-09-05T00:00:00Z",
        "holder": "did:zid:57579499be87f64546e574c409fb59ae77fbb64113fc438bb7dd17fc4ed35ec4",
        "issuer": "did:zid:d545dc623b0562e9b02a0b4f280b32bd060c9ff1b3582290e6d760e3cc3bfd15",
        "details": "{\"id\":\"did:zid:57579499be87f64546e574c409fb59ae77fbb64113fc438bb7dd17fc4ed35ec4\",\"mykad\":{\"name\":\"Ali bin Abuq\",\"dob\":\"1990-01-01\",\"gender\":\"Male\",\"nationality\":\"Malaysian\",\"mykadNo\":\"901231-14-5678\",\"citizenType\":\"Warganegara\",\"address\":\"No. 123, Jalan Merdeka\",\"postcode\":\"43000\",\"city\":\"Kajang\",\"state\":\"Selangor\",\"photo\":\"base64-photo-string\",\"securityImage\":\"base64-security-image\"}}",
        "verified": true
    },
    "timestamp": "2025-09-04T18:33:00.7847397"
}
```

### Check Health
```bash
curl http://localhost:8080/actuator/health
```

### Test Token Status
```bash
curl http://localhost:8080/test/token/status
```

## Built With

- Spring Boot 3.5.5
- Java 17
- Redis
- Maven