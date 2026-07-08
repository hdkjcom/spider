# 变更记录 (Changelog)

本文件记录 Spider 各版本的显著变更。

## [1.1.2] - 2026-07-07

聚焦：全项目代码审查 + 实战反馈修复，共 26 项。建议所有用户从 1.1.1 及更早版本升级。

### 严重修复（生产静默失效）
- Micrometer 指标 Bean 永不注册（NOOP 抢跑），`spider.client.*` 指标全静默丢失
- 限流（RateLimiterInterceptor）与契约（ContractInterceptor）`@ConditionalOnMissingBean` 互斥，只能生效一个
- ResilienceCircuitBreaker.recordFailure 把墙钟时间当调用耗时传 onError，污染慢调用统计
- JacksonSpiderDecoder 解析失败异常外泄触发重试风暴（JsonProcessingException 被当 IOException 重试）
- ResponseContextFilter 的 finally 清 ThreadLocal，导致 `lastResponse()` 永远返回 null
- SLF4J 栈冲突：spider-core `slf4j-api` 2.0.9 降到 1.7.36，logback 1.3.14 降到 1.2.13（匹配 spring-boot 2.7）

### 一般修复
- SpiderRequest URL query/header 顺序不定（HashMap → LinkedHashMap），影响签名/缓存
- per-client retry 配置无条件覆盖全局 default-retry
- 方法级统计 key 用 httpMethod 改为方法名（`ctx.method().getName()`）
- ReportController successRate 格式不一致（带 % vs 不带）
- RequestTemplate PATH 参数为 null 时残留 `{xxx}`，改为抛 SpiderConfigurationException
- CircuitBreakerTransport 4xx 计为成功（补 Javadoc 说明设计决策）
- computeBackoff 位移溢出（attempt ≥ 64 时溢出为负），上限 62
- recordRetry / recordFallback 增加方法级统计
- CountingCircuitBreaker 滑动窗口竞态（record 路径加 synchronized 保护）
- OkHttpSpiderTransport 未知 HTTP 方法静默降级 GET，改为抛 IllegalArgumentException
- ContractValidationFilter 配置 requiredFields 但 body 为空时静默跳过，改为抛违约
- SpiderClientRegistrar 同名 @SpiderClient 静默覆盖，改为判重抛异常
- ResilienceCircuitBreaker.state() 的 FORCED_OPEN 误报 CLOSED
- ReportController snapshotCount 锁外取值 + 快照 Map 缺 totalLatencyMs/p50/p90/avgLatencyMs
- 熔断器解析顺序违反文档（properties 压过注解），改为注解优先

### 实战反馈修复
- JacksonSpiderEncoder 对 @Body String 二次序列化破坏 body（ppmt 智谱文生图 API 返回 400）
- SpiderHttpException 增加 `getBody()` / `getBodyAsString()`，便于诊断目标 API 响应体

### Removed
- ContractInterceptor 空壳自动配置（SpiderContractAutoConfiguration，注册 null validator 永远空跑）
- ReportPayload.tracing 字段与 TracingDto（tracing 模块已移除，死代码）

## [1.1.1] - 2026-07-07

### Fixed
- @Query 参数未拼到请求 URL（GET query string 全部丢失，如微信 jscode2session 的 appid/secret/js_code）

### Changed
- README 补充实测性能数据（Spider 代理 QPS 约为原生 OkHttp 的 72.5%，overhead +21µs）

## [1.1.0] - 2026-07-06

聚焦：CI 基线 + 动态治理 + 契约保护 + 可观测性 + 工程收尾。

### Added
- GitHub Actions CI 全量覆盖（Java 8/11/17 矩阵），README 接入 badge
- 熔断器 / 限流器 reconfigure 动态刷新（运行时不重启调阈值）
- 契约校验 ContractValidationFilter（注解驱动 expectedStatus / requireBody / requiredFields）
- 延迟 Timer 发布 p50/p90/p99 百分位 + 直方图
- JMH 规范微基准 SpiderJmhBenchmark
- console 版本动态化（build-info.properties + Thymeleaf ${version}）
- gRPC 流式控制类型化（StreamItem Type enum 替代字段组合）
- per-client CircuitBreaker 接入 Spring properties（`spider.clients.<name>.circuit-breaker.*`）
- SLA 达标快览（可用性 / P99 / 错误率，红绿灯卡片）

## [1.0.1] - 2026-07-01

### Added
- 静态请求头注解（`@SpiderClient.headers()` / `@SpiderGet.headers()` 等）

## [1.0.0] - 2026-06-30

### Added
- 声明式 HTTP 客户端：`@SpiderClient` + `@SpiderGet` / `@SpiderPost` / `@SpiderPut` / `@SpiderDelete` / `@SpiderStream`
- 参数绑定：`@Path` / `@Query` / `@Header` / `@Body`
- 弹性治理：超时、重试（智能跳过 + 指数退避 + jitter）、熔断、限流、降级（fallback + FallbackFactory）、拦截器
- 9 个可插拔、可重排的 filter 管道
- 11 类类型化异常 + ErrorCategory
- 多传输 SPI：HTTP（OkHttp）、gRPC、messaging
- 服务发现 SPI：内存、Nacos、Spring Cloud DiscoveryClient
- 负载均衡 SPI：round-robin、random、Spring Cloud LoadBalancer
- 编解码 SPI：Jackson
- 配置中心 SPI：内存实现
- 指标 SPI：Micrometer
- Spring Boot starter：`@EnableSpiderClients` 自动扫描 + 自动配置 + per-client 配置
- 监控 console：dashboard + actuator 端点 + health indicator
- 异步调用：CompletableFuture 返回类型
- OpenAPI 客户端代码生成 + 接口反向生成
