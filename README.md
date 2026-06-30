# Spider

> [**中文**](README_CN.md) | English

Declarative Service Invocation Governance Middleware for Java Microservices.

Spider = **Declarative Remote Call** + **Elastic Governance** + **Contract Protection** + **Observability**

## Features

- Declarative HTTP client via Java annotations (`@SpiderGet`, `@SpiderPost`, `@SpiderPut`, `@SpiderDelete`)
- JDK Dynamic Proxy — no ByteBuddy, no CGLIB, interfaces only
- **Filter chain pipeline** — pluggable invocation filters for request building, discovery, retry, transport, decode, metrics, fallback
- Pluggable transport — OkHttp (HTTP), gRPC, extensible via SPI
- JSON codec — Jackson-based with generic type support, String responses auto-detect JSON vs plain text
- Retry — configurable attempts, exponential backoff, smart skip (4xx/config/circuit-breaker errors never retried)
- Circuit breaker — built-in counting breaker + Resilience4j integration
- Rate limiting — annotation-driven, Resilience4j backed
- Fallback — interface-level graceful degradation, `FallbackFactory` for cause inspection
- Interceptors — before/after/onError hooks
- Timeout — per-request timeout honored by transport
- **Service discovery** — SPI with in-memory and Nacos implementations
- **Load balancing** — round-robin (default), random, extensible via SPI
- **Typed exception hierarchy** — 11 exception types with `ErrorCategory` for precise retry and metrics decisions
- **Configuration priority** — method annotation > interface annotation > Spring properties > builder > framework defaults
- **Per-client configuration** — `spider.clients.<name>.*` in `application.yml`
- Metrics — Micrometer integration with `error_type` tag (`spider.client.requests`, `retries`, `fallbacks`, `duration`)
- Contract validation — response validation interceptor
- Spring Boot starter — `@EnableSpiderClients`, auto-scan, `application.yml` config
- Standalone Console — monitoring dashboard at `/spider`, method-level client summaries
- Actuator endpoints — `/actuator/spider`, `/actuator/spider/clients/{name}`, health indicator with per-client stats
- No Spring required — core modules run with plain Java 8

## Quick Start

### Maven

```xml
<!-- Spring Boot: one dependency -->
<dependency>
    <groupId>io.github.hdkjcom.spider</groupId>
    <artifactId>spider-spring-boot-starter</artifactId>
    <version>0.3.0</version>
</dependency>

<!-- Plain Java: three dependencies -->
<dependency><groupId>io.github.hdkjcom.spider</groupId><artifactId>spider-core</artifactId><version>0.3.0</version></dependency>
<dependency><groupId>io.github.hdkjcom.spider</groupId><artifactId>spider-http</artifactId><version>0.3.0</version></dependency>
<dependency><groupId>io.github.hdkjcom.spider</groupId><artifactId>spider-jackson</artifactId><version>0.3.0</version></dependency>
```

### Define a client

```java
@SpiderClient(name = "user-service", url = "http://localhost:8081")
public interface UserClient {

    @SpiderGet("/users/{id}")
    @Timeout(800)
    @Retry(maxAttempts = 3, backoffMillis = 100)
    UserDTO getUser(@Path("id") Long id);

    @SpiderPost("/users")
    UserDTO createUser(@Body CreateUserRequest request);
}
```

### Create a proxy and call

```java
SpiderClientFactory factory = SpiderClientFactory.builder()
        .transport(new OkHttpSpiderTransport())
        .decoder(new JacksonSpiderDecoder())
        .encoder(new JacksonSpiderEncoder())
        .build();

UserClient client = factory.create(UserClient.class);
UserDTO user = client.getUser(1L);  // GET http://localhost:8081/users/1
```

### Service discovery and load balancing

`url` is optional when a `SpiderServiceDiscovery` is configured. Spider resolves instances by `@SpiderClient.name` and uses round-robin load balancing by default.

```java
@SpiderClient(name = "user-service")
public interface UserClient {
    @SpiderGet("/users/{id}")
    UserDTO getUser(@Path("id") Long id);
}

SpiderClientFactory factory = SpiderClientFactory.builder()
        .transport(new OkHttpSpiderTransport())
        .decoder(new JacksonSpiderDecoder())
        .encoder(new JacksonSpiderEncoder())
        .serviceDiscovery(new SimpleServiceDiscovery()
                .register("user-service",
                        "http://localhost:8081",
                        "http://localhost:8082"))
        .build();
```

### Spring Boot

```java
@SpringBootApplication
@EnableSpiderClients
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}

@RestController
public class UserController {
    private final UserClient client;
    public UserController(UserClient c) { this.client = c; }

    @GetMapping("/users/{id}")
    public UserDTO get(@PathVariable Long id) { return client.getUser(id); }
}
```

### Governance

```java
@SpiderClient(name = "pay-service", url = "http://...",
              fallback = PayFallback.class)             // fallback
@SpiderCircuitBreaker(failureRateThreshold = 50)        // circuit breaker
public interface PayClient {

    @SpiderPost("/pay")
    @Retry(maxAttempts = 3, backoffStrategy = EXPONENTIAL,
           ignoreStatus = {422})                        // retry
    @RateLimit(permits = 200)                           // rate limit
    @Timeout(1500)                                      // timeout
    PayResult pay(@Body PayRequest req);
}
```

### Observability

| Metric (Micrometer) | Type | Tags |
|---|---|---|
| `spider.client.requests` | Counter | client, method, outcome (success/failure), error_type |
| `spider.client.retries` | Counter | client, method, error_type |
| `spider.client.fallbacks` | Counter | client, method |
| `spider.client.duration` | Timer | client, method |

### Error Handling

Spider provides 11 typed exceptions under `SpiderException` with precise `ErrorCategory`:

```java
try {
    userClient.getUser(1L);
} catch (SpiderHttpClientException e) {
    // 4xx — never retried, check request parameters
} catch (SpiderHttpServerException e) {
    // 5xx — may be retried
} catch (SpiderCircuitBreakerOpenException e) {
    // Circuit breaker is OPEN — fast-fail
} catch (SpiderRateLimitException e) {
    // Rate limit exceeded
} catch (SpiderException e) {
    // Other Spider errors
    log.error("category={}", e.category());
}
```

See [Core Concepts](docs/concepts.md) for the full hierarchy.

### Configuration

Spider resolves configuration from multiple sources with a clear priority:

```text
method annotation > interface annotation > Spring properties > builder defaults > framework defaults
```

Per-client overrides in `application.yml`:

```yaml
spider:
  default-timeout: 5000
  clients:
    user-service:
      url: http://user:8081
      timeout: 2000
      retry:
        max-attempts: 5
```

See [Configuration Reference](docs/configuration.md) for all options.

### Monitoring Console

**Single service (default):** No configuration needed. Access `http://your-port/spider` — data is read directly from the local runtime. Dashboard shows client metrics, circuit breaker states, recent snapshots, and tracing status.

**Multi-service:** Deploy a central console and configure each service to report:

```yaml
spider:
  console:
    url: http://spider-console:18080
    service-name: order-service
```

```bash
mvn exec:java -pl spider-console -Dexec.mainClass=io.github.spider.console.SpiderConsoleApplication
```

See [Observability](docs/observability.md) for metrics, tracing, actuator endpoints and dashboard details.

## Architecture

```
@SpiderClient interface -> JDK Dynamic Proxy -> SpiderInvocationHandler
  -> SpiderInvocationContext -> SpiderFilterChain
  -> Filters: ResponseContext -> ServiceDiscovery -> RequestBuild ->
     Interceptor -> Fallback -> Retry -> Transport -> Decode -> Metrics
  -> SpiderTransport (HTTP / gRPC) -> Remote Service
```

The invocation pipeline is a pluggable filter chain. Each governance concern (retry, circuit breaker, rate limit, metrics, tracing, fallback) is a discrete, testable filter that can be reordered or replaced.

### Modules

| Module | Description |
|---|---|
| `spider-core` | Annotations, proxy, metadata, pipeline, transport SPI |
| `spider-http` | OkHttp transport |
| `spider-jackson` | Jackson JSON codec |
| `spider-metrics` | Micrometer integration |
| `spider-resilience` | Resilience4j circuit breaker + rate limiter |
| `spider-contract` | Contract validation interceptor |
| `spider-grpc` | gRPC transport (JSON/DynamicMessage) |
| `spider-nacos` | Nacos service discovery |
| `spider-console` | Standalone monitoring console |
| `spider-codegen` | OpenAPI to SpiderClient generator |
| `spider-config` | Dynamic configuration SPI |
| `spider-messaging` | Message queue transport SPI |
| `spider-benchmark` | JMH benchmarks |
| `spider-spring-boot-starter` | All-in-one Spring Boot starter |
| `spider-demo` | Self-contained demo |

## Build

```bash
mvn compile
mvn test
mvn package -DskipTests
mvn exec:java -pl spider-demo -Dexec.mainClass=io.github.spider.demo.SpiderDemo
```

Requires JDK 8+, Maven 3.6+.

## vs OpenFeign

| Feature | Spider | OpenFeign |
|---|---|---|
| Annotations | Yes | Yes |
| Retry | Built-in, smart skip by exception type | Spring Retry |
| Circuit breaker | Built-in | Sentinel/Hystrix |
| Rate limiting | Built-in | No |
| Load balancing | Round-robin, random, SPI | Ribbon/Spring Cloud LoadBalancer |
| Fallback with cause | Built-in, FallbackFactory | Spring Cloud |
| Interceptors | before/after/onError | RequestInterceptor |
| Exception hierarchy | 11 typed exceptions with category | FeignException only |
| Configuration priority | Documented, enforced | Ad-hoc |
| Per-client config | `spider.clients.<name>.*` | Per-@FeignClient properties |
| Metrics | `spider.client.*` with error_type tag | Manual |
| gRPC | Supported | No |
| Filter chain | Pluggable, reorderable filters | InvocationHandlerFactory |
| Spring dependency | Zero (core) | Required |
| Java | 8+ | 8+ |

## Design Principles

- Interface declaration separated from execution logic
- Governance policies separated from transport protocol
- Every component is replaceable via SPI
- Core modules do not depend on Spring
- Never reinvent low-level wheels — OkHttp, Jackson, Micrometer, Resilience4j, OpenTelemetry

## Docs

- [Quick Start](docs/quickstart.md)
- [Configuration Reference](docs/configuration.md)
- [Core Concepts](docs/concepts.md)
- [SPI Extension Guide](docs/spi.md)
- [Observability](docs/observability.md)

## License

Apache 2.0
