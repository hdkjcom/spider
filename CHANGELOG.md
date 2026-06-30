# Changelog

## 0.1.9 (2026-06-25)

First Maven Central release.

### Core
- `@SpiderClient`, `@SpiderGet`, `@SpiderPost`, `@SpiderPut`, `@SpiderDelete`
- `@Path`, `@Query`, `@Body`, `@Header`, `@Timeout`, `@Retry`, `@RateLimit`, `@SpiderCircuitBreaker`
- JDK Dynamic Proxy, `MethodMetadata`, `RequestTemplate`, `SpiderRequest/Response`
- `SpiderTransport/Encoder/Decoder` SPIs, `SpiderInterceptor`, `SpiderMetrics`, `SpiderServiceDiscovery`
- `FallbackFactory<T>`, `CountingCircuitBreaker`, exponential backoff, `SpiderResponseContext`
- `SpiderReporter` auto-reports metrics to Console

### Modules (14 published)
- `spider-core`, `spider-http`, `spider-jackson`, `spider-metrics`, `spider-resilience`, `spider-contract`
- `spider-grpc`, `spider-nacos`, `spider-console`, `spider-codegen`, `spider-telemetry`
- `spider-config`, `spider-messaging`, `spider-spring-boot-starter`

### Compliance
- Apache 2.0 LICENSE, NOTICE, CONTRIBUTING.md
- GitHub Actions CI (Java 8/11/17 matrix)
- Benchmark: 22,877 QPS, +15% vs raw OkHttp
- Bilingual README, CONFIGURATION.md, ERROR_HANDLING.md, COMPARISON.md, RELEASE.md
