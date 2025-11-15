# repository-api

Branch take home project that creates an endpoint to retrieve a GitHub user's profile and list of repositories.

## Architecture

This project follows **Hexagonal Architecture** (Ports and Adapters) with a feature-based package structure:

```
com.branch.repository.api/
├── {feature}/
│   ├── domain/               # Core business logic (the hexagon)
│   │   ├── model/           # Domain models
│   │   └── service/         # Business logic implementation
│   ├── application/
│   │   └── port/
│   │       ├── in/          # Input ports (use cases)
│   │       └── out/         # Output ports (dependencies)
│   └── adapter/             # External adapters
│       ├── in/
│       │   └── rest/        # REST controllers (driving adapters)
│       └── out/             # External integrations (driven adapters)
└── infrastructure/          # Cross-cutting concerns
    ├── cache/              # Caching configuration
    ├── exception/          # Global exception handling
    ├── ratelimit/          # Rate limiting (Bucket4j)
    ├── rest/               # REST client configuration
    ├── retry/              # Retry mechanism
    └── security/           # Security configuration
```

### Key Design Patterns

- **Hexagonal Architecture**: Clean separation between business logic and infrastructure
- **CQRS**: Query-focused use cases (QueryDeveloperProfileUseCase)
- **Dependency Inversion**: Domain depends on abstractions (ports), not implementations
- **Port Naming Convention**:
  - Input Ports: `*UseCase` (e.g., `QueryDeveloperProfileUseCase`)
  - Output Ports: `*Port` (e.g., `LoadDeveloperProfilePort`)

### Example: Developer Profile Feature

- **Domain Layer**: `DeveloperProfile` and `DeveloperRepository` models
- **Application Ports**:
  - Input: `QueryDeveloperProfileUseCase` - what the application offers
  - Output: `LoadDeveloperProfilePort` - what the application needs
- **Domain Service**: `DeveloperProfileService` implements the use case
- **Adapters**:
  - Input: `DeveloperRepositoryController` (REST API)
  - Output: `QueryDeveloperProfileAdapter` (GitHub API integration)

## Technologies

### Core Framework
- **Java 17** - Modern LTS version with records, switch expressions, and enhanced pattern matching for cleaner, more maintainable code
- **Spring Boot 3.2.1** - Industry-standard framework providing auto-configuration, embedded server, and production-ready features out of the box
- **Gradle 8.5** - Modern build tool with better performance than Maven and Kotlin DSL support

### Web & REST
- **Spring Web** - Battle-tested REST framework with comprehensive HTTP support, content negotiation, and Spring MVC architecture
- **SpringDoc OpenAPI 2.3.0** - Auto-generates OpenAPI 3 spec and Swagger UI from code annotations, eliminating manual API documentation maintenance

### Resilience & Performance
- **Spring Retry** - Declarative retry logic with exponential backoff; reduces boilerplate compared to manual retry implementation
- **Spring AOP** - Enables cross-cutting concerns (retry, caching) without polluting business logic; proxy-based, non-invasive
- **Spring Cache** - Abstraction over caching providers allowing easy cache implementation swapping without code changes
- **Caffeine Cache** - Superior to Guava Cache with W-TinyLFU eviction algorithm achieving higher hit rates; async loading support
- **Bucket4j 8.10.1** - Industry-standard token bucket rate limiting; thread-safe, high-performance, configurable refill strategies

### Security
- **Spring Security** - Comprehensive security framework; used here for HTTPS enforcement and HTTP→HTTPS redirection with minimal configuration

### Data Transformation
- **MapStruct 1.5.5** - Compile-time code generation for type-safe object mapping; zero reflection overhead unlike runtime mappers (ModelMapper, Dozer); generates readable, debuggable code

### Testing
- **JUnit 5** - Modern testing framework with parameterized tests, nested tests, and better extension model than JUnit 4
- **Mockito** - Industry-standard mocking framework with intuitive API and excellent Spring integration for isolated unit testing

## Features

### Caching

Response caching with Caffeine for improved performance:

- **GitHub user profiles** cached for 60 minutes
- **GitHub repositories** cached for 60 minutes
- **W-TinyLFU eviction algorithm** for optimal cache hit rates
- **Maximum cache size**: 1000 entries per cache
- **Cache statistics**: Enabled via `.recordStats()` for programmatic monitoring
- **Debug logging**: Cache evictions logged at DEBUG level
- **Configurable** via `application.properties`:
  ```properties
  cache.github.ttl-minutes=60
  cache.github.max-size=1000

  # Enable cache debug logging
  logging.level.com.branch.repository.api.infrastructure.cache=DEBUG
  ```

**Example cache log output**:
```
2025-11-14 10:30:00 - Caffeine cache manager initialized with TTL=60min, maxSize=1000, caches: [githubUserProfile, githubUserRepositories]
2025-11-14 11:30:01 - Cache entry removed - key: octocat, cause: EXPIRED
```

**Note**: Cache hit/miss logging is not implemented because Spring's `@Cacheable` abstraction short-circuits method execution on cache hits, making it difficult to log without significant performance overhead. Cache statistics can be accessed programmatically via Caffeine's stats API if needed.

### Retry Mechanism

Automatic retry with exponential backoff for transient failures:

- **Retryable errors**: HTTP 5xx server errors and 429 (rate limiting)
- **Max attempts**: 3 (configurable)
- **Exponential backoff**: 500ms → 1000ms → 2000ms
- **AOP-based**: Automatically applied to GitHubService via proxy
- **Configurable** via `application.properties`:
  ```properties
  retry.max-attempts=3
  retry.initial-interval=500
  retry.multiplier=2.0
  retry.max-interval=5000
  ```

### Rate Limiting

Token bucket rate limiting to protect endpoints from abuse:

- **Algorithm**: Token bucket with Bucket4j
- **Scope**: Per-IP address + per-endpoint
- **Health endpoint**: 1 request per 5 minutes
- **Developer endpoint**: 10 requests per minute
- **SpEL support**: All limits configurable via properties
- **Response**: HTTP 429 with RFC 7807 Problem Detail
- **Headers**: Supports X-Forwarded-For and X-Real-IP
- **Configurable** via `application.properties`:
  ```properties
  # Health endpoint: 1 request per 5 minutes
  rate-limit.health.capacity=1
  rate-limit.health.refillTokens=1
  rate-limit.health.refillPeriod=5
  rate-limit.health.refillUnit=MINUTES

  # Developer endpoint: 10 requests per minute
  rate-limit.developer.capacity=10
  rate-limit.developer.refillTokens=10
  rate-limit.developer.refillPeriod=1
  rate-limit.developer.refillUnit=MINUTES
  ```

**Rate Limit Error Response**:
```json
{
  "type": "about:blank",
  "title": "Rate Limit Exceeded",
  "status": 429,
  "detail": "Too many requests. Please try again later.",
  "timestamp": "Thu, 14 Nov 2025 10:30:00 GMT"
}
```

### Error Handling

Global exception handling with RFC 7807 Problem Details:

- **Validation errors**: 400 Bad Request with constraint violations
- **Client errors (4xx)**: Proper error messages and status codes
- **Server errors (5xx)**: 502 Bad Gateway for external service failures
- **Retry exhaustion**: 503 Service Unavailable with original status preserved
- **Generic errors**: 500 Internal Server Error without exposing internals
- **Timestamps**: All error responses include ISO-8601 timestamps

### API Documentation

Interactive API documentation with Swagger UI:

- **Swagger UI**: Available at `/swagger-ui.html`
- **OpenAPI 3 Spec**: Available at `/v3/api-docs`
- **@Tag and @Operation** annotations for clear documentation
- **@ApiResponses** for documented error responses
- **Try it out** feature works correctly via HTTPS

## Configuration

Key configuration properties in `application.properties`:

```properties
# Server Configuration
server.port=8443
server.ssl.enabled=true

# GitHub API
github.api.base-url=https://api.github.com

# Retry Configuration
retry.max-attempts=3
retry.initial-interval=500
retry.multiplier=2.0
retry.max-interval=5000

# Cache Configuration (Caffeine)
cache.github.ttl-minutes=60
cache.github.max-size=1000
```

## Building the Project

```bash
./gradlew build
```

## Running Tests

Run unit tests:
```bash
./gradlew test
```

Run all tests:
```bash
./gradlew check
```

### Test Coverage

The project includes comprehensive unit and integration tests:

**Unit Tests:**
- **Domain Services**: `DeveloperProfileServiceTest`, `HealthCheckServiceTest`
- **Mappers**: `GitHubMapperTest` (using JSON resource files)
- **GitHub Service**: `GitHubServiceTest` (RestTemplate parsing verification)
- **Retry Mechanism**: `RetryConfigTest` (actual RetryTemplate bean testing)
- **Exception Handling**: `GlobalExceptionHandlerTest` (RFC 7807 Problem Details)
- **Rate Limiting**: `Bucket4jRateLimitingFilterTest` (12 tests including token refill verification)
- **Controllers**: `DeveloperRepositoryControllerTest`, `HealthControllerTest`

**Integration Tests:**
- **Developer API**: `DeveloperRepositoryIntegrationTest` (end-to-end with real GitHub API)
- **Health API**: `HealthIntegrationTest` (including rate limit enforcement)

All tests use the **BaseResourceTest** utility class for loading JSON test resources.

## Running the Application

### Prerequisites: SSL Certificate Setup

The application uses HTTPS and requires an SSL certificate. The SSL keystore is **not checked into version control** for security reasons. You must generate it locally **before running the application for the first time**:

```bash
# Create keystore directory
mkdir -p src/main/resources/keystore

# Generate self-signed certificate (valid for 10 years)
keytool -genkeypair \
  -alias repository-api \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore src/main/resources/keystore/keystore.p12 \
  -validity 3650 \
  -storepass changeit \
  -keypass changeit \
  -dname "CN=localhost, OU=Development, O=Branch, L=San Francisco, ST=California, C=US"
```

This only needs to be done **once**. The keystore will be created at `src/main/resources/keystore/keystore.p12`.

**Note**: For production deployments, replace the self-signed certificate with a proper certificate from a trusted Certificate Authority.

### Start the Application

```bash
./gradlew bootRun
```

The application will start on:
- **HTTPS**: `https://localhost:8443`
- **HTTP**: `http://localhost:8080` (automatically redirects to HTTPS)

**Security Warning**: Since the application uses a self-signed certificate, your browser will show a security warning. You can safely proceed for local development by accepting the certificate.

## API Documentation

### Swagger UI

Interactive API documentation is available via Swagger UI:

- **Swagger UI**: `https://localhost:8443/swagger-ui/index.html` or `https://localhost:8443/swagger-ui.html`
- **OpenAPI JSON**: `https://localhost:8443/v3/api-docs`

**Note**: Access Swagger UI via HTTPS to avoid mixed content issues. Your browser will show a security warning due to the self-signed certificate - accept it to proceed. The "Try it out" feature in Swagger UI will work correctly when accessed via HTTPS.

## API Endpoints

### Developer Profile API

- `GET /api/developers/{username}` - Retrieve GitHub user profile and repositories

**Response Format**:
```json
{
  "user_name": "octocat",
  "display_name": "The Octocat",
  "avatar": "https://avatars.githubusercontent.com/u/583231?v=4",
  "geo_location": "San Francisco",
  "email": null,
  "url": "https://api.github.com/users/octocat",
  "created_at": "Tue, 25 Jan 2011 18:44:36 GMT",
  "repos": [
    {
      "name": "Hello-World",
      "url": "https://api.github.com/repos/octocat/Hello-World"
    }
  ]
}
```

**Response Caching**: Responses are cached for 60 minutes per username.

**Retry Behavior**: Automatically retries on 5xx errors and 429 (rate limiting) with exponential backoff.

**Example using curl** (with self-signed certificate):
```bash
curl -k https://localhost:8443/api/developers/octocat
```

**Error Responses** (RFC 7807 Problem Details):
```json
{
  "type": "about:blank",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "The requested resource was not found",
  "instance": null,
  "timestamp": "2025-11-13T12:00:00.000Z"
}
```

### Health Check

- `GET /actuator/health` - Spring Boot Actuator health check endpoint

```bash
curl -k https://localhost:8443/actuator/health
```

## Testing the API

### Using curl

Comprehensive curl examples for command-line testing (the `-k` flag allows self-signed certificates):

**Get Developer Profile:**
```bash
# Basic request
curl -k https://localhost:8443/api/developers/octocat

# Pretty-print JSON output
curl -k https://localhost:8443/api/developers/octocat | jq

# Include response headers
curl -k -i https://localhost:8443/api/developers/octocat

# Verbose output (shows request/response details)
curl -k -v https://localhost:8443/api/developers/octocat
```

**Test Error Responses:**
```bash
# 404 Not Found - nonexistent user
curl -k https://localhost:8443/api/developers/this-user-does-not-exist-12345

# 404 Not Found - empty username
curl -k https://localhost:8443/api/developers/
```

**Test Rate Limiting:**
```bash
# Hit the developer endpoint 11 times quickly to trigger rate limit (limit: 10/min)
for i in {1..11}; do
  echo "Request $i:"
  curl -k -w "\nHTTP Status: %{http_code}\n" https://localhost:8443/api/developers/octocat
  echo "---"
done
```

**Test Caching:**
```bash
# First request (cache MISS - slower)
time curl -k https://localhost:8443/api/developers/octocat > /dev/null

# Second request (cache HIT - much faster)
time curl -k https://localhost:8443/api/developers/octocat > /dev/null
```

**Health Check:**
```bash
# Basic health check
curl -k https://localhost:8443/actuator/health

# Pretty-print
curl -k https://localhost:8443/actuator/health | jq

# Test health endpoint rate limiting (limit: 1 per 5 minutes)
curl -k https://localhost:8443/actuator/health
curl -k https://localhost:8443/actuator/health  # Should return 429
```

**Access Swagger UI:**
```bash
# Open in browser (macOS)
open https://localhost:8443/swagger-ui.html

# Open in browser (Linux)
xdg-open https://localhost:8443/swagger-ui.html

# Open in browser (Windows)
start https://localhost:8443/swagger-ui.html
```

### Using IntelliJ HTTP Client

The project includes an **`api-tests.http`** file located in the project root with comprehensive test cases including:

- Developer profile requests (valid users)
- Error scenarios (404, 429 rate limiting)
- Health check endpoint
- Rate limit testing

**To use:**
1. Open `api-tests.http` in IntelliJ IDEA
2. Click the green play button (▶) next to any request
3. View responses in the Run window

This provides an interactive way to test all endpoints without leaving your IDE.

## Project Structure

```
src/
├── main/
│   ├── java/com/branch/repository/api/
│   │   ├── developer/                    # Developer profile feature
│   │   │   ├── adapter/
│   │   │   │   ├── in/rest/             # REST controllers
│   │   │   │   └── out/github/          # GitHub API integration
│   │   │   │       ├── mapper/          # MapStruct mappers
│   │   │   │       ├── model/           # GitHub DTOs
│   │   │   │       └── service/         # GitHub API client
│   │   │   ├── application/port/
│   │   │   │   ├── in/                  # Use cases (input ports)
│   │   │   │   └── out/                 # Dependencies (output ports)
│   │   │   └── domain/
│   │   │       ├── model/               # Domain entities
│   │   │       └── service/             # Business logic
│   │   ├── health/                       # Health check feature
│   │   └── infrastructure/               # Cross-cutting concerns
│   │       ├── cache/                   # Caffeine cache config
│   │       ├── exception/               # Global exception handler
│   │       ├── ratelimit/               # Rate limiting (Bucket4j)
│   │       ├── rest/                    # RestTemplate & error handling
│   │       ├── retry/                   # Spring Retry config
│   │       └── security/                # HTTPS configuration
│   └── resources/
│       ├── keystore/                    # SSL certificates (not in git)
│       └── application.properties       # Configuration
└── test/
    ├── java/com/branch/repository/api/
    │   ├── common/                      # BaseResourceTest utility
    │   ├── developer/                   # Feature tests
    │   ├── health/                      # Health check tests
    │   └── infrastructure/              # Infrastructure tests
    └── resources/
        ├── github-*.json                # Test resource files
        └── application-test.properties  # Test configuration
```

## CI/CD

This project includes a GitHub Actions workflow that:
- Builds the project
- Runs unit tests
- Runs integration tests
- Uploads test results and build artifacts

The workflow triggers on pushes and pull requests to `main` and `develop` branches.

## Development Notes

### Adding a New Feature

This project follows **Hexagonal Architecture** (Ports and Adapters). Here's how to add a new feature following this pattern:

#### Step-by-Step Guide

**Example**: Adding a "User Statistics" feature that retrieves GitHub user contribution stats

1. **Create Feature Package Structure**
   ```
   com.branch.repository.api.userstats/
   ├── domain/
   │   ├── model/
   │   │   └── UserStatistics.java          # Domain entity
   │   └── service/
   │       └── UserStatisticsService.java   # Business logic
   ├── application/port/
   │   ├── in/
   │   │   └── QueryUserStatisticsUseCase.java  # Input port (what app offers)
   │   └── out/
   │       └── LoadUserStatisticsPort.java      # Output port (what app needs)
   └── adapter/
       ├── in/rest/
       │   └── UserStatisticsController.java    # REST endpoint
       └── out/github/
           └── QueryUserStatisticsAdapter.java  # GitHub API client
   ```

2. **Define Domain Model** (`domain/model/UserStatistics.java`)
   ```java
   public record UserStatistics(
       String username,
       int totalCommits,
       int totalPullRequests,
       int totalIssues
   ) {}
   ```

3. **Create Input Port (Use Case)** (`application/port/in/QueryUserStatisticsUseCase.java`)
   ```java
   public interface QueryUserStatisticsUseCase {
       UserStatistics getUserStatistics(String username);
   }
   ```

4. **Create Output Port** (`application/port/out/LoadUserStatisticsPort.java`)
   ```java
   public interface LoadUserStatisticsPort {
       UserStatistics loadUserStatistics(String username);
   }
   ```

5. **Implement Domain Service** (`domain/service/UserStatisticsService.java`)
   ```java
   @Service
   public class UserStatisticsService implements QueryUserStatisticsUseCase {
       private final LoadUserStatisticsPort loadUserStatisticsPort;

       // Constructor injection
       public UserStatisticsService(LoadUserStatisticsPort loadUserStatisticsPort) {
           this.loadUserStatisticsPort = loadUserStatisticsPort;
       }

       @Override
       public UserStatistics getUserStatistics(String username) {
           // Business logic here
           return loadUserStatisticsPort.loadUserStatistics(username);
       }
   }
   ```

6. **Create REST Controller** (`adapter/in/rest/UserStatisticsController.java`)
   ```java
   @RestController
   @RequestMapping("/api/users")
   public class UserStatisticsController {
       private final QueryUserStatisticsUseCase queryUserStatisticsUseCase;

       @GetMapping("/{username}/statistics")
       @RateLimit(capacity = "${rate-limit.stats.capacity}", ...)
       public ResponseEntity<UserStatistics> getUserStats(@PathVariable String username) {
           UserStatistics stats = queryUserStatisticsUseCase.getUserStatistics(username);
           return ResponseEntity.ok(stats);
       }
   }
   ```

7. **Implement Output Adapter** (`adapter/out/github/QueryUserStatisticsAdapter.java`)
   ```java
   @Component
   public class QueryUserStatisticsAdapter implements LoadUserStatisticsPort {
       private final RestTemplate restTemplate;

       @Override
       @Cacheable(value = "userStatistics", key = "#username")
       @Retryable(...)
       public UserStatistics loadUserStatistics(String username) {
           // Call GitHub API and map response
       }
   }
   ```

8. **Add Unit Tests**
   - `UserStatisticsServiceTest` - Test business logic
   - `UserStatisticsControllerTest` - Test REST layer
   - `QueryUserStatisticsAdapterTest` - Test GitHub integration

9. **Add Integration Test**
   - `UserStatisticsIntegrationTest` - End-to-end test with real GitHub API

#### Key Principles

- **Domain is isolated**: No dependencies on infrastructure (Spring, REST, etc.)
- **Dependency flow**: Adapters depend on ports, not vice versa
- **Ports define contracts**: Clear interfaces between layers
- **Adapters are replaceable**: Can swap GitHub for another source without changing domain
- **Test each layer**: Unit tests for domain, integration tests for adapters

#### Configuration

Add any new configuration to `application.properties`:
```properties
# Rate limiting for new endpoint
rate-limit.stats.capacity=5
rate-limit.stats.refillTokens=5
rate-limit.stats.refillPeriod=1
rate-limit.stats.refillUnit=MINUTES
```

### Naming Conventions

- **Properties**: kebab-case (e.g., `retry.max-attempts`)
- **Input Ports**: `*UseCase` (e.g., `QueryDeveloperProfileUseCase`)
- **Output Ports**: `*Port` (e.g., `LoadDeveloperProfilePort`)
- **Services**: `*Service` (e.g., `DeveloperProfileService`)
- **Tests**: `*Test` (e.g., `DeveloperProfileServiceTest`)

### Best Practices

- Always use `@Cacheable` for expensive external API calls
- Use `RetryableHttpException` for retryable HTTP errors
- Extend `BaseResourceTest` for tests requiring JSON resource loading
- Follow hexagonal architecture boundaries - no direct dependencies from domain to infrastructure
- Use MapStruct for object mapping between layers
- Return RFC 7807 Problem Details for all errors

## Outstanding Items

The following enhancements and improvements have been identified but are not yet implemented:

### Security & Production Readiness (High Priority)

1. **GitHub API Authentication**
   - **Current**: Unauthenticated API calls (60 requests/hour per IP)
   - **Recommended**: Add GitHub Personal Access Token support (5,000 requests/hour)
   - **Implementation**:
     - Add `github.api.token` property
     - Configure `Authorization: Bearer ${token}` header in RestTemplate
     - Store token in environment variable for security

2. **Health Check Endpoint**
   - **Current**: No health check endpoint
   - **Recommended**: Add Spring Boot Actuator for production monitoring
   - **Implementation**:
     - Add `spring-boot-starter-actuator` dependency
     - Configure `/actuator/health` endpoint
     - Add custom health indicators for GitHub API connectivity
     - Essential for Kubernetes/Docker deployments and load balancers

3. **SSL Configuration - Production Settings**
   - **Current**: SSL password hardcoded in `application.properties`
   - **Recommended**: Move to environment variable
   - **Additional**: Configure client authentication, cipher suites, TLS version

### Code Quality & Maintainability (Medium Priority)

4. **Request/Response Logging**
   - **Current**: No structured request/response logging
   - **Recommended**: Add MDC logging with request IDs, timing, status codes
   - **Benefits**: Easier debugging in production environments

5. **Integration Tests - Mock External APIs**
   - **Current**: Integration tests call real GitHub API
   - **Recommended**: Use WireMock for stubbing GitHub responses
   - **Benefits**:
     - Faster tests (~50ms vs 3-5 seconds)
     - Offline development support
     - No rate limit issues in CI/CD
     - Ability to test error scenarios reliably

6. **Environment-Specific SSL Configuration**
   - **Current**: Same SSL settings for all environments
   - **Recommended**: Disable SSL for local dev, enforce stricter settings in production

### API Maturity & Features (Low Priority)

7. **API Versioning**
   - **Current**: No version management
   - **Recommended**: Use custom HTTP header for versioning (e.g., `X-API-Version: 1`)
   - **Benefits**: Allows backward-compatible API evolution without changing URL structure
   - **Note**: Path-based versioning (`/api/v1/...`) intentionally avoided in favor of header-based approach

8. **Pagination Support**
   - **Current**: Returns all repositories (unbounded)
   - **Recommended**: Add pagination query parameters (`?page=1&size=20`)
   - **Benefits**: Better performance for users with many repositories

9. **OpenAPI Examples**
   - **Current**: Basic Swagger documentation
   - **Recommended**: Add `@Schema` examples for all request/response models
   - **Benefits**: Better API documentation and auto-generated client SDKs

10. **HTTP Caching Headers**
    - **Current**: No caching headers (ETag, Cache-Control)
    - **Recommended**: Add RFC 7234 HTTP caching support
    - **Benefits**: Client-side caching, reduced bandwidth

### Observability & Performance (Low Priority)

11. **Performance Metrics**
    - **Recommended**: Add Micrometer metrics for request duration, cache hit rates, GitHub API latency
    - **Implementation**: Spring Boot Actuator + Prometheus/Grafana integration

12. **Distributed Tracing**
    - **Recommended**: Add Spring Cloud Sleuth for request tracing across service boundaries
    - **Benefits**: Track request flow through system, correlate logs

13. **Async API Calls**
    - **Current**: Synchronous RestTemplate calls
    - **Recommended**: Consider `CompletableFuture` or WebClient for parallel GitHub API calls
    - **Benefits**: Fetch user profile and repositories concurrently

### Notes

- **Priority levels** are recommendations based on production-readiness and security
- Items 1-3 (Security & Production Readiness) should be addressed before deploying to production
- Items 4-6 (Code Quality) improve maintainability and developer experience
- Items 7-13 (API Maturity & Observability) are enhancements for future iterations