# Changelog

## 0.1.0 (2026-06-25)

First stable release. 15 modules published to Maven Central.

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
