# Changelog

## 0.1.0 (2026-06-25)

First stable release.

### Core
- `@SpiderClient`, `@SpiderGet`, `@SpiderPost`, `@SpiderPut`, `@SpiderDelete`
- `@Path`, `@Query`, `@Body`, `@Header` parameter binding
- `@Timeout`, `@Retry`, `@RateLimit`, `@SpiderCircuitBreaker` governance annotations
- `@SpiderStream` (gRPC server-streaming)
- JDK Dynamic Proxy via `SpiderClientFactory`
- `MethodMetadata` + `RequestTemplate` + `SpiderRequest/Response`
- `SpiderTransport`, `SpiderEncoder`, `SpiderDecoder` SPIs
- `SpiderInterceptor` chain (before/after/onError)
- `SpiderMetrics` SPI + `SpiderRuntime` registry
- `SpiderServiceDiscovery` SPI + `SimpleServiceDiscovery`
- `FallbackFactory<T>` for cause-aware fallback
- `CountingCircuitBreaker` (built-in)
- Exponential backoff (`@Retry(backoffStrategy = EXPONENTIAL)`)
- Fine-grained retry (`retryOn`, `ignoreStatus`)
- `SpiderResponseContext` for response header access
- `SpiderReporter` auto-reports metrics to Console

### Modules
- `spider-http`: OkHttp transport (GET/POST/PUT/DELETE)
- `spider-jackson`: Jackson JSON codec with generic type support (String passthrough)
- `spider-metrics`: Micrometer integration (5 metrics)
- `spider-resilience`: Resilience4j CircuitBreaker + RateLimiter
- `spider-contract`: Contract validation interceptor
- `spider-grpc`: gRPC transport (JSON/DynamicMessage, streaming, proto E2E)
- `spider-nacos`: Nacos service discovery
- `spider-console`: Standalone monitoring console (Thymeleaf, H2, Nacos-style UI)
- `spider-codegen`: OpenAPI 3.0 to @SpiderClient code generator
- `spider-telemetry`: OpenTelemetry trace propagation (TracingInterceptor)
- `spider-config`: Dynamic configuration SPI + InMemoryConfigCenter
- `spider-messaging`: Message queue transport SPI + InMemoryMessageTransport
- `spider-spring-boot-starter`: All-in-one Spring Boot starter with auto-config for all modules
- `spider-demo`: Self-contained demo

### Compliance
- Apache 2.0 LICENSE, NOTICE, CONTRIBUTING.md
- GitHub Actions CI (Java 8/11/17 matrix)
- Benchmark: Spider 22,877 QPS, +15% vs raw OkHttp
- Bilingual README (English + Chinese)
- CONFIGURATION.md, ERROR_HANDLING.md, COMPARISON.md, RELEASE.md

### Core
- `@SpiderClient`, `@SpiderGet`, `@SpiderPost`, `@SpiderPut`, `@SpiderDelete`
- `@Path`, `@Query`, `@Body`, `@Header` parameter binding
- `@Timeout`, `@Retry`, `@RateLimit`, `@SpiderCircuitBreaker` governance annotations
- `@SpiderStream` (gRPC server-streaming)
- JDK Dynamic Proxy via `SpiderClientFactory`
- `MethodMetadata` + `RequestTemplate` + `SpiderRequest/Response`
- `SpiderTransport`, `SpiderEncoder`, `SpiderDecoder` SPIs
- `SpiderInterceptor` chain (before/after/onError)
- `SpiderMetrics` SPI + `SpiderRuntime` registry
- `SpiderServiceDiscovery` SPI + `SimpleServiceDiscovery`
- `FallbackFactory<T>` for cause-aware fallback
- `CountingCircuitBreaker` (built-in)
- Exponential backoff (`@Retry(backoffStrategy = EXPONENTIAL)`)
- Fine-grained retry (`retryOn`, `ignoreStatus`)

### Modules (17 total)
- `spider-http`: OkHttp transport (GET/POST/PUT/DELETE)
- `spider-jackson`: Jackson JSON codec with generic type support
- `spider-metrics`: Micrometer integration (5 metrics)
- `spider-resilience`: Resilience4j CircuitBreaker + RateLimiter
- `spider-contract`: Contract validation interceptor (`@ValidateResponse`)
- `spider-grpc`: gRPC transport (JSON↔DynamicMessage, streaming, proto E2E)
- `spider-nacos`: Nacos service discovery
- `spider-admin`: Dashboard REST API + HTML UI (`SpiderAdminService`)
- `spider-codegen`: OpenAPI 3.0 → @SpiderClient code generator
- `spider-benchmark`: Spider vs OkHttp performance comparison
- `spider-telemetry`: OpenTelemetry trace propagation (`TracingInterceptor`)
- `spider-config`: Dynamic configuration SPI + `InMemoryConfigCenter`
- `spider-messaging`: Message queue transport SPI + `InMemoryMessageTransport`
- `spider-spring-boot-starter`: `@EnableSpiderClients`, auto-scan, `application.yml`
- `spider-demo`: Usage examples + Spring Boot demo

### Compliance
- Apache 2.0 LICENSE
- NOTICE (third-party attributions)
- CONTRIBUTING.md
- GitHub Actions CI (Java 8/11/17 matrix)
- Benchmark: Spider 22,877 QPS, +15% vs raw OkHttp
- COMPARISON.md (vs OpenFeign/Retrofit)
