# Spider 🕷️
> **破茧计划 (Cocoon Breaking Plan)** — Declarative Service Invocation Governance Middleware for Java Microservices.

[![Java](https://img.shields.io/badge/Java-8%2B-blue)](https://adoptium.net)
[![Maven](https://img.shields.io/badge/Maven-3.6%2B-orange)](https://maven.apache.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-green)](LICENSE)

Spider = **Declarative Remote Call** + **Elastic Governance** + **Contract Protection** + **Observability**

## ✨ Features

- **Declarative HTTP Client** — Define remote service calls via Java annotations (`@SpiderGet`, `@SpiderPost`)
- **JDK Dynamic Proxy** —No ByteBuddy/CGLIB, interfaces only
- **Pluggable Transport** —OkHttp (HTTP), gRPC, extensible via SPI
- **JSON Codec** —Jackson-based encoder/decoder with generic type support
- **Retry Policy** —GET idempotent retry, configurable attempts and backoff (skip 4xx)
- **Circuit Breaker** —Built-in counting breaker + Resilience4j integration (`@SpiderCircuitBreaker`)
- **Fallback** —Interface-level graceful degradation on failure
- **Interceptors** —Request/response hooks (tracing, logging, auth)
- **Timeout Control** —Method-level and client-level timeout configuration
- **Metrics** —Micrometer integration (success/failure/retry/fallback/duration)
- **Service Discovery** —SPI + Simple (in-memory) + Nacos implementations
- **Contract Validation** —Response validation interceptor
- **Spring Boot Starter** —`@EnableSpiderClients`, auto-scan, `application.yml` config
- **No Spring Required** —Core modules are framework-agnostic, Java 8 compatible

## 🚀 Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>io.github.spider</groupId>
    <artifactId>spider-core</artifactId>
    <version>0.1.0</version>
</dependency>
<dependency>
    <groupId>io.github.spider</groupId>
    <artifactId>spider-http</artifactId>
    <version>0.1.0</version>
</dependency>
<dependency>
    <groupId>io.github.spider</groupId>
    <artifactId>spider-jackson</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Define a Client Interface

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

### Create a Proxy and Call

```java
SpiderClientFactory factory = SpiderClientFactory.builder()
        .transport(new OkHttpSpiderTransport())
        .decoder(new JacksonSpiderDecoder())
        .encoder(new JacksonSpiderEncoder())
        .build();

UserClient client = factory.create(UserClient.class);
UserDTO user = client.getUser(1L);  // GET http://localhost:8081/users/1
```

### Fallback

```java
@SpiderClient(name = "user-service", url = "http://localhost:8081",
              fallback = UserClientFallback.class)
public interface UserClient { ... }

public class UserClientFallback implements UserClient {
    @Override
    public UserDTO getUser(Long id) {
        return UserDTO.empty(id);
    }
}
```

### Interceptors

```java
SpiderClientFactory factory = SpiderClientFactory.builder()
        .transport(new OkHttpSpiderTransport())
        .decoder(new JacksonSpiderDecoder())
        .addInterceptor(new SpiderInterceptor() {
            @Override
            public SpiderRequest beforeRequest(SpiderRequest request) {
                request.addHeader("X-Trace-Id", UUID.randomUUID().toString());
                return request;
            }
        })
        .build();
```

### Spring Boot Integration

**application.yml:**
```yaml
spider:
  default-timeout: 5000
  default-retry:
    max-attempts: 3
    backoff-millis: 100
  transport:
    connect-timeout: 10000
    read-timeout: 30000
```

**Enable Spider clients:**
```java
@SpringBootApplication
@EnableSpiderClients(basePackages = "com.example.client")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

**Inject and use:**
```java
@RestController
public class UserController {

    private final UserClient userClient;

    public UserController(UserClient userClient) {
        this.userClient = userClient;
    }

    @GetMapping("/users/{id}")
    public UserDTO getUser(@PathVariable Long id) {
        return userClient.getUser(id);
    }
}
```

### Metrics (Micrometer)

Metrics are automatically recorded when using the Spring Boot starter:

| Metric | Description |
|---|---|
| `spider.requests.success` | Counter of successful calls |
| `spider.requests.failure` | Counter of failed calls |
| `spider.requests.retry` | Counter of retry attempts |
| `spider.requests.fallback` | Counter of fallback triggers |
| `spider.requests.duration` | Timer of invocation duration |

Tags: `client` (service name), `method` (method name).

### Circuit Breaker

```java
// Option 1: annotation-based (uses built-in CountingCircuitBreaker)
@SpiderClient(name = "user-service", url = "http://localhost:8081")
@SpiderCircuitBreaker(failureRateThreshold = 50, slidingWindowSize = 10,
                      waitDurationInOpenStateMillis = 10000)
public interface UserClient { ... }

// Option 2: programmatic (Resilience4j)
SpiderCircuitBreaker cb = new ResilienceCircuitBreaker("user-svc", annotation);
SpiderClientFactory.builder()
    .transport(new OkHttpSpiderTransport())
    .circuitBreaker(cb)
    .build();
```

### Service Discovery

```java
// Simple in-memory discovery
SimpleServiceDiscovery sd = new SimpleServiceDiscovery()
    .register("user-service", "http://192.168.1.10:8081", "http://192.168.1.11:8081");

SpiderClientFactory.builder()
    .serviceDiscovery(sd)
    .transport(new OkHttpSpiderTransport())
    .build();

// Nacos discovery
SpiderClientFactory.builder()
    .serviceDiscovery(new NacosSpiderDiscovery("localhost:8848"))
    .transport(new OkHttpSpiderTransport())
    .build();
```

### Contract Validation

```java
SpiderClientFactory.builder()
    .addInterceptor(new ContractInterceptor(response -> {
        if (response.bodyBytes().length == 0) {
            throw new SpiderClientException("Response body must not be empty");
        }
    }))
    .build();
```

### gRPC Transport

```java
ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:9090")
    .usePlaintext().build();
GrpcSpiderTransport transport = new GrpcSpiderTransport(channel);
transport.registerMethod("/greet", "greet.Greeter", "SayHello",
    methodDesc, requestDesc, responseDesc);
```

## 🏗 Architecture

```
@SpiderClient interface —JDK Dynamic Proxy —SpiderInvocationHandler
  —MethodMetadata parsing —RequestTemplate —SpiderRequest
  —Invocation Pipeline (interceptors —retry —timeout —transport —decode —fallback)
  —SpiderTransport (HTTP/gRPC/...) —Remote Service
  —SpiderResponse —Decode —Return to caller
```

### Module Layout

| Module | Description |
|---|---|
| `spider-core` | Annotations, proxy, metadata, pipeline, transport SPI |
| `spider-http` | OkHttp-based HTTP transport |
| `spider-jackson` | Jackson JSON encoder/decoder |
| `spider-metrics` | Micrometer-based invocation metrics |
| `spider-resilience` | Resilience4j-based circuit breaker |
| `spider-contract` | Request/response contract validation |
| `spider-grpc` | gRPC-based transport (JSON/DynamicMessage) |
| `spider-nacos` | Nacos-based service discovery |
| `spider-admin` | Admin REST API for runtime monitoring |
| `spider-codegen` | OpenAPI 3.0-to-SpiderClient code generator |
| `spider-benchmark` | JMH benchmarks (Spider vs OkHttp) |
| `spider-spring-boot-starter` | Spring Boot auto-configuration |
| `spider-demo` | Usage examples |

## 📋 Build

```bash
# Compile
mvn compile

# Test
mvn test

# Package
mvn package -DskipTests
```

**Requirements:** JDK 8+, Maven 3.6+, 12 modules

## 🗺 Roadmap

### Phase 1 —Current
- [x] Multi-module Maven project
- [x] Core annotations (`@SpiderClient`, `@SpiderGet`, `@SpiderPost`, etc.)
- [x] JDK Dynamic Proxy
- [x] Method metadata parsing
- [x] Request/Response model + Transport SPI
- [x] OkHttp transport
- [x] Jackson codec
- [x] GET/POST with path/query/header/body binding
- [x] Retry policy
- [x] Fallback support
- [x] Interceptor chain

### Phase 2 —Current
- [x] Spring Boot Starter (`@EnableSpiderClients`, auto-scan, auto-config)
- [x] Micrometer metrics integration (`spider.requests.success/failure/retry/fallback/duration`)
- [x] Configuration properties (`application.yml` support)

### Phase 3 —Current
- [x] Circuit breaker (Resilience4j + built-in CountingCircuitBreaker)
- [x] Service discovery SPI (SimpleServiceDiscovery)
- [x] Contract validation (ContractInterceptor)
- [x] `@SpiderCircuitBreaker` annotation with auto-wrapping transport

### Phase 4 —Current
- [x] gRPC transport (JSON→DynamicMessage→gRPC→JSON)
- [x] Nacos service discovery (NacosSpiderDiscovery)
- [x] Admin service (SpiderAdminService —runtime status/monitoring)

### Phase 5 🔜 Vision
- [ ] Dashboard for governance
- [ ] Dynamic policy updates
- [ ] OpenAPI code generation
- [ ] Multi-protocol unified calls
- [ ] Runtime topology and observability

## 🎯 Design Principles

- **Interface declaration separated from execution logic** —annotations on interfaces, not implementations
- **Governance policies separated from transport protocol** —retry/timeout/fallback in the pipeline, transport swappable via SPI
- **Core modules do NOT depend on Spring**
- **Never reinvent low-level wheels** —OkHttp, Jackson, Micrometer

## 🤝 Contributing

Spider aims to become an Apache top-level project. Contributions are welcome!

## 📄 License

Apache 2.0
