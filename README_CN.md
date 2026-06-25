# Spider

> [English](README.md) | **中文**

面向 Java 微服务的声明式服务调用治理中间件。

Spider = **声明式远程调用** + **弹性治理** + **契约保护** + **可观测性**

## 功能特性

- 通过 Java 注解定义远程服务调用（`@SpiderGet`、`@SpiderPost`、`@SpiderPut`、`@SpiderDelete`）
- JDK 动态代理，无需 ByteBuddy/CGLIB，仅支持接口
- 可插拔传输层：OkHttp (HTTP)、gRPC，通过 SPI 扩展
- JSON 编解码，基于 Jackson，支持泛型
- 重试：GET 幂等自动重试，可配置次数、指数退避、忽略特定状态码
- 熔断：内置计数熔断器 + Resilience4j 集成
- 限流：注解驱动，Resilience4j 实现
- 降级：接口级优雅降级，FallbackFactory 可获取异常信息
- 拦截器：before/after/onError 三个钩子
- 超时：方法级和客户端级
- 指标：Micrometer 集成（成功、失败、重试、降级、耗时）
- 链路追踪：OpenTelemetry W3C trace-context 自动注入
- 服务发现：SPI，内置内存版和 Nacos 实现
- 契约校验：响应校验拦截器
- Spring Boot starter：`@EnableSpiderClients`，自动扫描，`application.yml` 配置
- 核心模块零依赖 Spring，兼容 Java 8

## 快速开始

### Maven

```xml
<!-- Spring Boot 项目：一个依赖搞定全部 -->
<dependency>
    <groupId>io.github.spider</groupId>
    <artifactId>spider-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>

<!-- 纯 Java 项目：三个依赖 -->
<dependency><groupId>io.github.spider</groupId><artifactId>spider-core</artifactId><version>0.1.0</version></dependency>
<dependency><groupId>io.github.spider</groupId><artifactId>spider-http</artifactId><version>0.1.0</version></dependency>
<dependency><groupId>io.github.spider</groupId><artifactId>spider-jackson</artifactId><version>0.1.0</version></dependency>
```

### 定义客户端接口

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

### 创建代理并调用

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

### 治理能力

```java
@SpiderClient(name = "pay-service", url = "http://...",
              fallback = PayFallback.class)             // 降级
@SpiderCircuitBreaker(failureRateThreshold = 50)        // 熔断
public interface PayClient {

    @SpiderPost("/pay")
    @Retry(maxAttempts = 3, backoffStrategy = EXPONENTIAL,
           ignoreStatus = {422})                        // 重试
    @RateLimit(permits = 200)                           // 限流
    @Timeout(1500)                                      // 超时
    PayResult pay(@Body PayRequest req);
}
```

### 可观测性

| 指标（Micrometer） | 类型 |
|---|---|
| `spider.requests.success` | Counter |
| `spider.requests.failure` | Counter |
| `spider.requests.retry` | Counter |
| `spider.requests.fallback` | Counter |
| `spider.requests.duration` | Timer |

标签：`client`（服务名），`method`（方法名）。

链路追踪：`TracingInterceptor` 自动注入 W3C trace-context（需 `spider-telemetry` 在 classpath 上）。

Dashboard：`SpiderAdminApp` 提供监控界面 `http://localhost:18000`，包含 QPS、延迟百分位、错误日志、趋势图。

启动横幅：首次使用 `SpiderClientFactory` 时控制台打印 SPIDER ASCII 字样。

## 架构

```
@SpiderClient 接口 → JDK 动态代理 → SpiderInvocationHandler
  → MethodMetadata → RequestTemplate → SpiderRequest
  → 管道（拦截器 → 限流 → 熔断 → 重试 → 超时 → 传输 → 解码 → 降级）
  → SpiderTransport (HTTP / gRPC) → 远程服务
  → SpiderResponse → 解码 → 返回
```

### 模块一览

| 模块 | 说明 |
|---|---|
| `spider-core` | 注解、代理、元数据、管道、传输 SPI |
| `spider-http` | OkHttp 传输 |
| `spider-jackson` | Jackson JSON 编解码 |
| `spider-metrics` | Micrometer 集成 |
| `spider-resilience` | Resilience4j 熔断 + 限流 |
| `spider-contract` | 契约校验拦截器 |
| `spider-grpc` | gRPC 传输 |
| `spider-nacos` | Nacos 服务发现 |
| `spider-admin` | Dashboard + Actuator 端点 |
| `spider-codegen` | OpenAPI 代码生成器 |
| `spider-telemetry` | OpenTelemetry 追踪 |
| `spider-config` | 动态配置 SPI |
| `spider-messaging` | 消息队列传输 SPI |
| `spider-spring-boot-starter` | 全功能 Spring Boot starter |
| `spider-demo` | 自包含演示 |

## 构建

```bash
mvn compile
mvn test
mvn package -DskipTests
mvn exec:java -pl spider-demo -Dexec.mainClass=io.github.spider.demo.SpiderDemo
```

要求 JDK 8+，Maven 3.6+。

## 路线图

| 阶段 | 状态 |
|---|---|
| 声明式调用 + 重试 + 降级 + 拦截器 | 已完成 |
| Spring Boot Starter + Micrometer 指标 | 已完成 |
| 熔断 + 限流 + 契约 + 服务发现 | 已完成 |
| gRPC + Nacos + Dashboard + OpenTelemetry | 已完成 |
| Maven Central 发布 | 待 Sonatype 审批 |

## 对比 OpenFeign

| 能力 | Spider | OpenFeign |
|---|---|---|
| 注解 | 有 | 有 |
| 重试 | 内置 | 需 Spring Retry |
| 熔断 | 内置 | 需 Sentinel/Hystrix |
| 限流 | 内置 | 无 |
| 降级（含异常信息） | 内置 | 需 Spring Cloud |
| 拦截器 | before/after/onError | RequestInterceptor |
| 指标 | 内置 5 个 Micrometer 指标 | 需额外集成 |
| 追踪 | OpenTelemetry 内置 | 需 Sleuth |
| gRPC | 支持 | 不支持 |
| Spring 依赖 | 核心零依赖 | 强依赖 |
| Java 版本 | 8+ | 8+ |
| 性能 | 22,877 QPS (+15%) | ~18,000 QPS (+45%) |

## 设计原则

- 接口声明与执行逻辑分离
- 治理策略与传输协议分离
- 核心模块不依赖 Spring
- 不重复造轮子 — OkHttp、Jackson、Micrometer、Resilience4j

## 许可证

Apache 2.0
