# Spider

> [English](README.md) | **中文**

面向 Java 微服务的声明式服务调用治理中间件。

Spider = **声明式远程调用** + **弹性治理** + **契约保护** + **可观测性**

## 功能特性

- 通过 Java 注解定义远程服务调用（`@SpiderGet`、`@SpiderPost`、`@SpiderPut`、`@SpiderDelete`）
- JDK 动态代理，无需 ByteBuddy/CGLIB，仅支持接口
- **过滤器链管道** — 可插拔的调用过滤器：请求构建、服务发现、重试、传输、解码、指标、降级
- 可插拔传输层：OkHttp (HTTP)、gRPC，通过 SPI 扩展
- JSON 编解码，基于 Jackson，支持泛型，字符串响应自动识别 JSON/纯文本
- 重试：可配置次数、指数退避、智能跳过（4xx/配置错误/熔断拒绝永不重试）
- 熔断：内置计数熔断器 + Resilience4j 集成
- 限流：注解驱动，Resilience4j 实现
- 降级：接口级优雅降级，FallbackFactory 可获取异常信息
- 拦截器：before/after/onError 三个钩子
- 超时：每请求超时由传输层实际执行
- **服务发现**：SPI，内置内存版和 Nacos 实现
- **负载均衡**：默认轮询，可选随机，SPI 可扩展
- **类型化异常体系**：11 种异常类型，带 `ErrorCategory`，精确控制重试和指标
- **配置优先级**：方法注解 > 接口注解 > Spring 属性 > Builder > 框架默认值
- **每客户端独立配置**：`spider.clients.<name>.*` in `application.yml`
- 指标：Micrometer 集成，带 `error_type` 标签（`spider.client.requests`、`retries`、`fallbacks`、`duration`）
- 契约校验：响应校验拦截器
- Spring Boot starter：`@EnableSpiderClients`，自动扫描，`application.yml` 配置
- 独立控制台：`/spider` 监控 Dashboard，方法级客户端汇总
- Actuator 端点：`/actuator/spider`、`/actuator/spider/clients/{name}`，健康检查含每客户端指标
- 核心模块零依赖 Spring，兼容 Java 8

## 快速开始

### Maven

```xml
<!-- Spring Boot 项目：一个依赖搞定全部 -->
<dependency>
    <groupId>io.github.hdkjcom.spider</groupId>
    <artifactId>spider-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 纯 Java 项目：三个依赖 -->
<dependency><groupId>io.github.hdkjcom.spider</groupId><artifactId>spider-core</artifactId><version>1.0.0</version></dependency>
<dependency><groupId>io.github.hdkjcom.spider</groupId><artifactId>spider-http</artifactId><version>1.0.0</version></dependency>
<dependency><groupId>io.github.hdkjcom.spider</groupId><artifactId>spider-jackson</artifactId><version>1.0.0</version></dependency>
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

### 服务发现与负载均衡

配置 `SpiderServiceDiscovery` 后，`@SpiderClient` 可以省略 `url`。Spider 会按 `name` 解析服务实例，并默认使用 round-robin 负载均衡。

```java
@SpiderClient(name = "user-service")
public interface UserClient {
    @SpiderGet("/users/{id}")
    UserDTO getUser(@Path("id") Long id);
}

SpiderClientFactory factory = SpiderClientFactory.builder()
        .transport(new OkHttpSpiderTransport())
        .decoder(new JacksonSpiderDecoder())
        .encoder(new JacksonSpiderEncoder())
        .serviceDiscovery(new SimpleServiceDiscovery()
                .register("user-service",
                        "http://localhost:8081",
                        "http://localhost:8082"))
        .build();
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

| 指标（Micrometer） | 类型 | 标签 |
|---|---|---|
| `spider.client.requests` | Counter | client, method, outcome (success/failure), error_type |
| `spider.client.retries` | Counter | client, method, error_type |
| `spider.client.fallbacks` | Counter | client, method |
| `spider.client.duration` | Timer | client, method |

标签：`client`（服务名），`method`（方法名），`error_type`（异常类型）。

### 错误处理

11 种类型化异常，继承自 `SpiderException`，每种带有 `ErrorCategory`。详见 [核心概念](docs/concepts.md)。

### 控制台 Dashboard


**多服务统一监控：** 部署中央控制台，各业务服务配置上报地址：

```yaml
spider:
  console:
    url: http://spider-console:18080
    service-name: order-service
```

```bash
mvn exec:java -pl spider-console -Dexec.mainClass=io.github.spider.console.SpiderConsoleApplication
```

详见 [可观测性](docs/observability.md)。

## 架构

```
@SpiderClient 接口 -> JDK 动态代理 -> SpiderInvocationHandler
  -> SpiderInvocationContext -> SpiderFilterChain
  -> 过滤器链: ResponseContext -> ServiceDiscovery -> RequestBuild ->
     Interceptor -> Fallback -> Retry -> Transport -> Decode -> Metrics
  -> SpiderTransport (HTTP / gRPC) -> 远程服务
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
| `spider-console` | 独立监控控制台 |
| `spider-codegen` | OpenAPI 代码生成器 |
| `spider-config` | 动态配置 SPI |
| `spider-messaging` | 消息队列传输 SPI |
| `spider-benchmark` | JMH 基准测试 |
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

## 对比 OpenFeign

| 能力 | Spider | OpenFeign |
|---|---|---|
| 注解 | 有 | 有 |
| 重试 | 内置，智能跳过（按异常类型） | 需 Spring Retry |
| 熔断 | 内置 | 需 Sentinel/Hystrix |
| 限流 | 内置 | 无 |
| 负载均衡 | 轮询/随机，SPI 可扩展 | 需 Ribbon/LoadBalancer |
| 降级（含异常信息） | 内置，FallbackFactory | 需 Spring Cloud |
| 拦截器 | before/after/onError | RequestInterceptor |
| 异常体系 | 11 种类型化异常 | 仅 FeignException |
| 过滤器链 | 可插拔，可重排 | InvocationHandlerFactory |
| 指标 | `spider.client.*` 含 error_type 标签 | 需额外集成 |
| gRPC | 支持 | 不支持 |
| Spring 依赖 | 核心零依赖 | 强依赖 |
| Java 版本 | 8+ | 8+ |

## 设计原则

- 接口声明与执行逻辑分离
- 治理策略与传输协议分离
- 所有组件均可通过 SPI 替换
- 核心模块不依赖 Spring
- 不重复造轮子 — OkHttp、Jackson、Micrometer、Resilience4j、OpenTelemetry

## 文档

- [快速开始](docs/quickstart.md)
- [配置参考](docs/configuration.md)
- [核心概念](docs/concepts.md)
- [SPI 扩展指南](docs/spi.md)
- [可观测性](docs/observability.md)

## 许可证

Apache 2.0
