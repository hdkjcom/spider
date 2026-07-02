# Spider vs OpenFeign vs Retrofit — 对比分析

## 一句话总结

| 框架 | 定位 | 一句话 |
|---|---|---|
| **Spider** | 调用治理中间件 | 声明式调用 + 内置治理（重试/熔断/限流/降级/指标/追踪） |
| **OpenFeign** | 声明式 HTTP 客户端 | 接口 = HTTP 调用，依赖 Spring Cloud 生态 |
| **Retrofit** | 类型安全 HTTP 客户端 | Android 优先，注解驱动，依赖 OkHttp |

## 功能对比

| 能力 | Spider | OpenFeign | Retrofit |
|---|---|---|---|
| 声明式注解 | `@SpiderGet` | `@GetMapping` | `@GET` |
| PATH/QUERY/HEADER/BODY 绑定 | 支持 | 支持 | 支持 |
| 超时控制 | `@Timeout` | 需配置 OkHttpClient | `@Headers` |
| 重试策略 | `@Retry`(GET/5xx重试/4xx不重试/指数退避/异常类型/状态码) | 需 Spring Retry | 需 OkHttp Interceptor |
| **熔断器** | `@SpiderCircuitBreaker` (内置+Resilience4j) | 需 Sentinel/Hystrix | 需额外库 |
| **限流** | `@RateLimit` (Resilience4j) | 不支持 | 不支持 |
| **降级 Fallback** | `fallback` + `fallbackFactory`(含异常) | `fallback`(Spring Cloud) | 不支持 |
| **拦截器** | `SpiderInterceptor`(before/after/onError) | `RequestInterceptor` | `Interceptor` |
| **指标 Micrometer** | 5 个内置指标 | 需额外集成 | 不支持 |
| **链路追踪 OpenTelemetry** | `TracingInterceptor` | 需 Sleuth/Micrometer Tracing | 不支持 |
| **熔断器状态暴露** | 内置 | 不支持 | 不支持 |
| **服务发现** | Simple + Nacos + SPI | 需 Spring Cloud LoadBalancer | 不支持 |
| **gRPC 传输** | `GrpcSpiderTransport` | 不支持 | 不支持 |
| **契约校验** | `ContractInterceptor` | 不支持 | 不支持 |
| **Spring Boot 集成** | `@EnableSpiderClients` 零配置 | `@EnableFeignClients` | 需手动集成 |
| **Spring 依赖** | 不依赖 (core 零 Spring) | 强依赖 Spring | 不依赖 |
| **Java 版本** | Java 8+ | Java 8+ | Java 8+ |
| **OpenAPI 代码生成** | `spider-codegen` | 第三方 | 不支持 |

## 性能对比

Benchmark 条件：20000 次调用，本地回环 HTTP server，单线程。

| 指标 | Spider | Raw OkHttp | Feign (参考) |
|---|---|---|---|
| 平均延迟 | 43.7 µs | 38.0 µs | ~55 µs |
| QPS | 22,877 | 26,305 | ~18,000 |
| 额外开销 | **+15%** (5.7µs) | - | **+45%** |

> Feign 数据来自社区 benchmark，实际因版本和配置而异。Spider 的 JDK 动态代理开销显著低于 Feign 的反射+编解码链。

## 治理能力对比

这是 Spider 和 Feign 最本质的差异：

```
Feign:   注解 → 发 HTTP 请求 → 拿到响应 → 完
         重试/熔断/限流/指标 → 都依赖外部组件拼装

Spider:  注解 → 进入调用管道 →
           [拦截器] → [限流] → [熔断] → [重试] → [传输] → [解码] → [指标] → [追踪]
         所有治理能力内置，一条管道贯穿
```

## 何时选择 Spider

| 场景 | 原因 |
|---|---|
| 微服务调用需要重试/熔断/限流 | 内置，不用拼装多个组件 |
| 需要可观测性（指标+追踪） | Micrometer + OTel 开箱即用 |
| 不想强依赖 Spring Cloud | core 模块零 Spring 依赖 |
| 纯 Java 项目（无 Spring） | 3 个依赖就能用 |
| 需要 gRPC + HTTP 统一调用 | 同一套注解，换 transport 即可 |
| Java 8 存量系统 | 全栈兼容 JDK 8 |
| 从 OpenAPI 生成客户端 | `spider-codegen` 一键生成 |

## 何时选择 OpenFeign

| 场景 | 原因 |
|---|---|
| 已有 Spring Cloud 全家桶 | 生态集成度高 |
| 只需要发 HTTP，不需要治理 | Feign 更轻量 |
| 团队已熟悉 Feign | 迁移成本 |

## 何时选择 Retrofit

| 场景 | 原因 |
|---|---|
| Android 开发 | Retrofit 是 Android 生态标准 |
| 需要协程/响应式 | Retrofit 原生支持 |
| OkHttp 深度定制 | Retrofit 对 OkHttp 的封装更薄 |

## 架构差异

```
OpenFeign:
  Interface → Proxy → MethodHandler → Encoder/Decoder → Client → HTTP
  治理能力依赖 Spring Cloud 外部组件（Sentinel, Resilience4j, Sleuth...）

Retrofit:
  Interface → Proxy → ServiceMethod → Converter → Call → OkHttp → HTTP
  纯 HTTP 客户端，治理能力依赖 OkHttp Interceptor 扩展

Spider:
  Interface → Proxy → Pipeline →
    [Interceptor] → [RateLimit] → [CircuitBreaker] → [Retry] →
    [Transport(HTTP/gRPC)] → [Decoder] → [Metrics] → [Trace]
  治理能力全部内置在管道中，SPI 可替换任意环节
```

---

**结论：Spider 不是 OpenFeign 的替代品，而是"Feign 做不到的事"——把调用治理从外部组件拼装变成框架内置能力。**
