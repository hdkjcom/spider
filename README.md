# Spider

> [**中文**](README_CN.md) | English

Declarative Service Invocation Governance Middleware for Java Microservices.

Spider = **Declarative Remote Call** + **Elastic Governance** + **Contract Protection** + **Observability**

## Features

- Declarative HTTP client via Java annotations (`@SpiderGet`, `@SpiderPost`, `@SpiderPut`, `@SpiderDelete`)
- JDK Dynamic Proxy — no ByteBuddy, no CGLIB, interfaces only
- Pluggable transport — OkHttp (HTTP), gRPC, extensible via SPI
- JSON codec — Jackson-based with generic type support
- Retry — GET idempotent by default, configurable attempts, exponential backoff, skip 4xx
- Circuit breaker — built-in counting breaker + Resilience4j integration
- Rate limiting — annotation-driven, Resilience4j backed
- Fallback — interface-level graceful degradation, `FallbackFactory` for cause inspection
- Interceptors — before/after/onError hooks
- Timeout — method-level and client-level
- Metrics — Micrometer integration (success, failure, retry, fallback, duration)
- Tracing — OpenTelemetry W3C trace-context auto-injection
- Service discovery — SPI with in-memory and Nacos implementations
- Contract validation — response validation interceptor
- Spring Boot starter — `@EnableSpiderClients`, auto-scan, `application.yml` config
- Standalone Console — monitoring dashboard for all Spider services (Nacos-style)
- Auto-reporting — services report metrics to Console via `spider.console.url` config
- No Spring required — core modules run with plain Java 8

## Quick Start

### Maven

```xml
<!-- Spring Boot: one dependency -->
<dependency>
    <groupId>io.github.hdkjcom.spider</groupId>
    <artifactId>spider-spring-boot-starter</artifactId>
    <version>0.1.2</version>
</dependency>

<!-- Plain Java: three dependencies -->
<dependency><groupId>io.github.hdkjcom.spider</groupId><artifactId>spider-core</artifactId><version>0.1.2</version></dependency>
<dependency><groupId>io.github.hdkjcom.spider</groupId><artifactId>spider-http</artifactId><version>0.1.2</version></dependency>
<dependency><groupId>io.github.hdkjcom.spider</groupId><artifactId>spider-jackson</artifactId><version>0.1.2</version></dependency>
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

| Metric (Micrometer) | Type |
|---|---|
| `spider.requests.success` | Counter |
| `spider.requests.failure` | Counter |
| `spider.requests.retry` | Counter |
| `spider.requests.fallback` | Counter |
| `spider.requests.duration` | Timer |

Tags: `client` (service name), `method` (method name).

Tracing: `TracingInterceptor` auto-injects W3C trace-context headers (requires `spider-telemetry` on classpath).

Startup banner: `SpiderClientFactory` prints an ASCII SPIDER banner on first use.

### Monitoring Console

Run the standalone console:

```bash
mvn exec:java -pl spider-console -Dexec.mainClass=io.github.hdkjcom.spider.console.SpiderConsoleApplication
```

Open `http://localhost:18080`. Services auto-report via config:

```properties
spider.console.url=http://localhost:18080
spider.console.service-name=my-service
```

Console shows: service overview, client metrics (QPS, p50/p90/p99), circuit breaker states, rate limiter status, retry counts, tracing status.

## Architecture

```
@SpiderClient interface -> JDK Dynamic Proxy -> SpiderInvocationHandler
  -> MethodMetadata -> RequestTemplate -> SpiderRequest
  -> Pipeline (interceptor -> rate limit -> circuit breaker -> retry -> timeout -> transport -> decode -> fallback)
  -> SpiderTransport (HTTP / gRPC) -> Remote Service
  -> SpiderResponse -> Decode -> return
```

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
| `spider-telemetry` | OpenTelemetry tracing |
| `spider-config` | Dynamic configuration SPI |
| `spider-messaging` | Message queue transport SPI |
| `spider-console` | Standalone monitoring console |
| `spider-spring-boot-starter` | All-in-one Spring Boot starter |
| `spider-demo` | Self-contained demo |

## Build

```bash
mvn compile
mvn test
mvn package -DskipTests
mvn exec:java -pl spider-demo -Dexec.mainClass=io.github.hdkjcom.spider.demo.SpiderDemo
```

Requires JDK 8+, Maven 3.6+.

## Roadmap

| Phase | Status |
|---|---|
| Declarative calls + retry + fallback + interceptors | Done |
| Spring Boot starter + Micrometer metrics | Done |
| Circuit breaker + rate limiter + contract + service discovery | Done |
| gRPC + Nacos + Dashboard + OpenTelemetry | Done |
| Maven Central release | Done |

## vs OpenFeign

| Feature | Spider | OpenFeign |
|---|---|---|
| Annotations | Yes | Yes |
| Retry | Built-in | Spring Retry |
| Circuit breaker | Built-in | Sentinel/Hystrix |
| Rate limiting | Built-in | No |
| Fallback with cause | Built-in | Spring Cloud |
| Interceptors | before/after/onError | RequestInterceptor |
| Metrics | 5 built-in Micrometer metrics | Manual |
| Tracing | OpenTelemetry built-in | Sleuth |
| gRPC | Supported | No |
| Spring dependency | Zero (core) | Required |
| Java | 8+ | 8+ |
| Performance | 22,877 QPS (+15%) | ~18,000 QPS (+45%) |

## Design Principles

- Interface declaration separated from execution logic
- Governance policies separated from transport protocol
- Core modules do not depend on Spring
- Never reinvent low-level wheels — OkHttp, Jackson, Micrometer, Resilience4j

## License

Apache 2.0
