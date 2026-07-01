# Spider Wiki

## 简介

Spider 是一个 Java 微服务的**声明式服务调用治理中间件**。通过注解定义远程接口，JDK 动态代理自动生成客户端，无需手写 HTTP 调用代码。内置重试、熔断、限流、降级、超时等治理能力，支持服务发现与负载均衡、Micrometer 指标和嵌入式 Dashboard。核心模块零外部依赖，兼容 Java 8。

> Spider = 声明式远程调用 + 弹性治理 + 契约保护 + 可观测性

## 特性

- **声明式 HTTP 客户端**：`@SpiderGet` / `@SpiderPost` / `@SpiderPut` / `@SpiderDelete` 注解
- **过滤器链管道**：9 个独立 filter，可插拔可重排
- **弹性治理**：超时、重试（jitter 防风暴）、熔断（内置 + Resilience4j）、限流、降级
- **11 种类型化异常**：`SpiderHttpServerException`（5xx）、`SpiderHttpClientException`（4xx）、`SpiderCircuitBreakerOpenException` 等，带 `ErrorCategory` 精确决策
- **服务发现与负载均衡**：Nacos / Spring Cloud DiscoveryClient 自动复用，RoundRobin / Random / 可自定义
- **嵌入式 Dashboard**：零配置访问 `/spider`，方法级指标、趋势图、暗色模式
- **Spring Boot 自动装配**：`@EnableSpiderClients` 自动扫描注册，`@ConditionalOnMissingBean` 全可覆盖
- **动态配置**：ConfigCenter SPI 支持运行时调整超时/重试/退避
- **异步调用**：`CompletableFuture<T>` 返回类型
- **OpenAPI 双向生成**：OpenAPI → SpiderClient 接口，接口 → OpenAPI spec
- **Spring Boot 2.x / 3.x 兼容**

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.github.hdkjcom.spider</groupId>
    <artifactId>spider-spring-boot-starter</artifactId>
    <version>1.0.1</version>
</dependency>
```

### 2. 定义接口

```java
@SpiderClient(name = "user-service", url = "http://localhost:8081")
public interface UserClient {

    @SpiderGet("/users/{id}")
    @Timeout(800)
    @Retry(maxAttempts = 3)
    UserDTO getUser(@Path("id") Long id);

    @SpiderPost("/users")
    UserDTO createUser(@Body CreateUserRequest req);
}
```

### 3. Spring Boot 注入

```java
@SpringBootApplication
@EnableSpiderClients
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}

@RestController
public class OrderController {
    private final UserClient userClient;
    public OrderController(UserClient c) { this.userClient = c; }

    @GetMapping("/users/{id}")
    public UserDTO get(@PathVariable Long id) {
        return userClient.getUser(id);  // 像本地方法一样调用
    }
}
```

### 4. 查看监控

访问 `http://localhost:8086/spider` 查看 Dashboard。

## 架构

```
@SpiderClient 接口
  → JDK 动态代理
  → SpiderInvocationHandler
  → SpiderFilterChain（9 个过滤器）
     ResponseContext → ServiceDiscovery → RequestBuild
     → Interceptor → Fallback → Metrics
     → Retry → Transport → Decode
  → SpiderTransport（HTTP / gRPC）→ 远程服务
```

## 配置优先级

```
方法注解 > 接口注解 > Spring 属性 > Builder 默认值 > 框架默认值
```

## 动态配置

实现 `SpiderConfigCenter` 接口并注册为 Spring Bean，可在运行时调整：

| 配置键 | 类型 | 说明 |
|---|---|---|
| `spider.client.<name>.retry.backoff` | long | 重试退避间隔 |
| `spider.client.<name>.retry.maxAttempts` | int | 最大重试次数 |
| `spider.client.<name>.timeout` | int | 调用超时 |

## 异步调用

```java
@SpiderGet("/users/{id}")
CompletableFuture<UserDTO> getUser(@Path("id") Long id);

CompletableFuture<UserDTO> future = client.getUser(1L);
future.thenAccept(user -> log.info("Got: {}", user));
```

默认使用 `ForkJoinPool.commonPool()`，可通过 `builder.asyncExecutor()` 自定义线程池。

## 最佳实践

- 每个外部接口设独立 `@Timeout`，关键路径 ≤ 3s
- GET 幂等操作 `@Retry(jitter = true)`，POST 非幂等不重试
- 关键接口有 `fallback` 降级实现
- 连接池 `maxIdle` 根据并发量调整

## 项目结构

| 模块 | 说明 |
|---|---|
| `spider-core` | 注解、代理、元数据、过滤器链、SPI |
| `spider-http` | OkHttp 传输 |
| `spider-jackson` | Jackson 编解码 |
| `spider-metrics` | Micrometer 指标 |
| `spider-resilience` | Resilience4j 熔断 + 限流 |
| `spider-contract` | 响应契约校验 |
| `spider-grpc` | gRPC 传输 |
| `spider-nacos` | Nacos 服务发现 |
| `spider-console` | 监控 Dashboard |
| `spider-codegen` | OpenAPI 双向代码生成 |
| `spider-config` | 动态配置 SPI |
| `spider-messaging` | 消息传输 SPI |
| `spider-benchmark` | JMH 基准测试 |
| `spider-spring-boot-starter` | Spring Boot 自动装配 |
| `spider-demo` | 使用示例 |

## 文档

- [快速开始](quickstart.md)
- [配置参考](configuration.md)
- [核心概念](concepts.md)
- [SPI 扩展指南](spi.md)
- [最佳实践](best-practices.md)
- [可观测性](observability.md)
