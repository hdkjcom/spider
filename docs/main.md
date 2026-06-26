# Spider Middleware Improvement Plan

## Goal

Turn Spider from a functional middleware prototype into a predictable, testable, and production-ready Java microservice invocation middleware. The target architecture should keep `spider-core` Spring-free while providing stable SPI contracts, clear governance behavior, strong Spring Boot integration, and reliable observability.

## Current Progress

- Baseline tests are green with `mvn test` (71 spider-core tests, all modules pass except pre-existing demo issues).
- `spider-jackson` string decoding now supports JSON string responses while falling back to raw UTF-8 text.
- `SpiderServiceDiscovery` is wired into runtime URL resolution when `@SpiderClient.url` is empty.
- `SpiderLoadBalancer` SPI with round-robin (default) and random implementations; full unit test coverage.
- `SimpleServiceDiscovery` has full unit test coverage (register, deregister, overwrite, unregistered).
- Discovery edge cases tested: empty instances, deregister between calls, annotation URL precedence, whitespace-only URL.
- `spider-console` now serves a split frontend: `templates/console.html`, `static/spider-console.css`, and `static/spider-console.js`.
- The console supports Chinese by default with an English toggle.
- Console dashboard data now includes method-level client summaries and recent report snapshots.
- Documentation fixes: duplicate module entry removed, `spider-benchmark` added, CLAUDE.md updated, dead H2/JPA config removed.
- SLF4J binding warnings resolved via `logback-classic` in root `dependencyManagement`.
- CI enhanced with `mvn verify` step and dependency analysis.
- **Phase 2 complete**: Invocation pipeline refactored into filter chain model.
  - `SpiderInvocationContext` carries all per-invocation state through the pipeline.
  - `SpiderInvocationFilter` interface with servlet-filter pattern.
  - `SpiderFilterChain` manages ordered filter execution.
  - 9 discrete filters replace the monolithic `SpiderInvocationHandler`:
    `ResponseContextFilter`, `ServiceDiscoveryFilter`, `RequestBuildFilter`,
    `InterceptorFilter`, `FallbackFilter`, `RetryFilter`, `TransportFilter`,
    `DecodeFilter`, `MetricsFilter`.
  - `SpiderClientFactory` builds the chain from builder options.
  - Spring Boot auto-configuration works without modification.
  - All existing tests pass with zero changes.
- **Phase 4 complete**: Configuration priority rules documented and enforced.
  - `@SpiderClient` Javadoc includes priority table.
  - `spider.default-timeout` and `spider.default-retry.*` Spring properties now consumed.
  - Interface-level `@Timeout`/`@Retry` inherited by methods when not specified.
  - `OkHttpSpiderTransport` honors per-request timeout via `SpiderRequest.timeoutMillis()`.
- **Phase 5 complete**: Typed exception hierarchy replacing generic `SpiderClientException`.
  - 11 exception types under `SpiderException` with `ErrorCategory` enum.
  - `RetryFilter` uses instanceof-based classification instead of statusCode ranges.
  - All throw sites migrated to typed exceptions (config, discovery, circuit breaker, rate limit, HTTP, IO, fallback, contract).
  - `SpiderClientException` preserved for backward compatibility.
- **Phase 6 complete**: Spring Boot Starter strengthened.
  - Auto-configuration bugs fixed (`@ConditionalOnMissingBean`, registrar `setPrimary`, `ROLE_APPLICATION`).
  - Spring Boot 3.x `AutoConfiguration.imports` added.
  - Per-client configuration via `spider.clients.<name>.*` in `application.yml`.
  - Actuator endpoint extended with per-client detail (`/actuator/spider/clients/{name}`).
  - Health indicator enhanced with per-client metrics and overall summary.
- **Phase 7 complete**: Observability standardized.
  - Metric names: `spider.client.requests` (with `outcome` tag), `spider.client.retries` (with `error_type`), `spider.client.fallbacks`, `spider.client.duration`.
  - `MicrometerSpiderMetrics` adds `error_type` tag derived from exception class name.
  - `TracingInterceptor` strips query params from URLs, adds `exception.type` span attribute.
  - `SpiderRuntime.recordError()` stores exception type for programmatic filtering.
- **Phase 8**: Test coverage expanded — 71 spider-core tests (0 failures), discovery/load-balancer/parser covered.
- **Phase 9**: Documentation created — `docs/quickstart.md`, `docs/configuration.md`, `docs/error-handling.md`, `docs/spi.md`.

## Phase 1: Stabilize Baseline Quality ✅ COMPLETE

- ~~Fix the current `mvn test` failure in `spider-jackson`~~ — Jackson string decoding supports JSON + raw fallback.
- ~~Keep `mvn test` green across all modules~~ — 71 tests, 0 failures in spider-core.
- ~~Add CI checks~~ — CI enhanced with verify + dependency analysis.
- ~~Correct documentation inconsistencies~~ — README duplicate row removed, benchmark added, CLAUDE.md updated, dead config removed.

## Phase 2: Refactor the Invocation Pipeline ✅ COMPLETE

- ~~Introduce `SpiderInvocationContext`~~ — carries client, method, request, response, retry, timing, exception, attributes.
- ~~Implement filter chain model~~ — `SpiderInvocationFilter` + `SpiderFilterChain` in `spider-core/.../invocation/`.
- ~~9 discrete filters~~ — ServiceDiscovery, RequestBuild, Interceptor, Retry, Transport, Decode, Metrics, Fallback, ResponseContext.
- ~~Refactor `SpiderInvocationHandler`~~ — from 213-line monolith to clean filter chain delegation.
- ~~Refactor `SpiderClientFactory`~~ — builds filter chain from builder options.
- ~~Keep filter order explicit and covered by tests~~ — all 71 existing tests pass without modification.

Chain order:
```
ResponseContextFilter → ServiceDiscoveryFilter → RequestBuildFilter →
InterceptorFilter → FallbackFilter → RetryFilter → TransportFilter →
DecodeFilter → MetricsFilter
```

## Phase 3: Complete Discovery and Load Balancing ✅ COMPLETE

- ~~Make `SpiderServiceDiscovery` participate in URL resolution~~ — `resolveBaseUrl()` calls discovery + load balancer when URL is empty.
- ~~Add `SpiderLoadBalancer` SPI~~ — `choose(serviceName, instances)` contract.
- ~~Round-robin and random implementations~~ — `RoundRobinSpiderLoadBalancer` + `RandomSpiderLoadBalancer` with full unit tests.
- ~~Tests for edge cases~~ — empty instances, deregister between calls, annotation URL precedence, whitespace-only URL, per-service isolation.

## Phase 4: Define Configuration Rules ✅ COMPLETE

- ~~Document configuration priority~~ — Javadoc in `@SpiderClient`, `docs/main.md`.
- ~~Wire orphaned Spring properties~~ — `spider.default-timeout` and `spider.default-retry.*` now flow through `SpiderAutoConfiguration` → `SpiderClientFactory.Builder` → `DefaultMethodMetadataParser`.
- ~~Interface-level annotation fallback~~ — `DefaultMethodMetadataParser` checks interface `@Timeout`/`@Retry` when method has none.
- ~~Per-request timeout~~ — `OkHttpSpiderTransport` now honors `SpiderRequest.timeoutMillis()` via per-call OkHttpClient.

Priority chain: **method annotation > interface annotation > Spring properties > builder defaults > framework defaults**

## Phase 5: Improve Error Semantics ✅ COMPLETE

- ~~Exception hierarchy~~ — 11 typed exceptions under `SpiderException` in `spider-core/.../exception/`:
  ```
  SpiderException (abstract, ErrorCategory enum)
    ├── SpiderConfigurationException      (CONFIG, never retry)
    ├── SpiderServiceDiscoveryException   (SERVICE_DISCOVERY, retryable)
    ├── SpiderCircuitBreakerOpenException (CIRCUIT_BREAKER, never retry)
    ├── SpiderRateLimitException          (RATE_LIMIT, never retry)
    ├── SpiderHttpException               (has statusCode)
    │     ├── SpiderHttpClientException   (HTTP_CLIENT/4xx, never retry)
    │     └── SpiderHttpServerException   (HTTP_SERVER/5xx, retryable)
    ├── SpiderIOException                 (NETWORK_IO, retryable)
    ├── SpiderFallbackException           (FALLBACK)
    └── SpiderContractViolationException  (CONTRACT)
  ```
- ~~Migrated 10+ throw sites~~ — `SpiderClientFactory`, `SpiderInvocationHandler`, `DecodeFilter`, `ServiceDiscoveryFilter`, `FallbackFilter`, `RetryFilter`, `CircuitBreakerTransport`, `RateLimiterInterceptor`, `ContractInterceptor`.
- ~~Refactored `RetryFilter`~~ — instanceof-based classification replaces fragile `statusCode` integer branching.
- ~~Backward compatible~~ — `SpiderClientException` extends `SpiderException`, existing catch blocks still work.

## Phase 6: Strengthen Spring Boot Starter ✅ COMPLETE

- ~~Fix auto-configuration bugs~~ — `SpiderMetricsAutoConfiguration` duplicate bean conflict resolved, `@ConditionalOnMissingBean` added to Resilience/Contract auto-configs.
- ~~Fix `SpiderClientRegistrar`~~ — removed incorrect `setPrimary(true)`, changed `ROLE_INFRASTRUCTURE` to `ROLE_APPLICATION`.
- ~~Spring Boot 3.x compatibility~~ — added `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- ~~Per-client configuration~~ — `spider.clients.<name>.url`, `.timeout`, `.retry.max-attempts`, `.retry.backoff-millis` in `application.yml`.
- ~~Actuator enhancements~~ — `/actuator/spider/clients`, `/actuator/spider/clients/{name}` endpoints with per-client detail.
- ~~Health indicator enhancements~~ — per-client stats (calls, success rate, avg latency, QPS), overall summary, circuit breaker status.

## Phase 7: Standardize Observability

- Stabilize metric names such as `spider.client.requests`, `spider.client.duration`, `spider.client.retries`, `spider.client.fallbacks`, and `spider.client.errors`.
- Limit tags to stable, low-cardinality values: `client`, `method`, `protocol`, `status`, and `exception`.
- Ensure tracing records client name, method, path, retry count, fallback status, and error type without exposing sensitive query values.

## Phase 8: Expand Test Coverage

- Add unit tests for metadata parsing, request building, retry, fallback, interceptor order, and discovery.
- Add transport tests for headers, query parameters, bodies, status codes, and timeouts.
- Add Jackson tests for objects, generics, strings, null, and empty bodies.
- Add end-to-end tests using a mock HTTP server and real Spider clients.

## Phase 9: Productize Documentation

Reorganize docs around user workflows:

- Quick Start
- Core Concepts
- Configuration Reference
- Spring Boot Integration
- SPI Extension Guide
- Error Handling
- Observability
- Release and Compatibility Notes

## Recommended Execution Order

1. ~~Fix failing tests and documentation errors.~~
2. ~~Refactor the invocation pipeline.~~
3. ~~Complete service discovery and load balancing.~~
4. ~~Define exception and configuration semantics.~~
5. ~~Strengthen Spring Boot Starter and observability.~~
6. ~~Expand integration tests and CI.~~
7. ~~Finalize user-facing documentation.~~
