# Spider 配置参考

## 注解参数一览

### @SpiderClient

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `name` | String | 是 | - | 服务名，用于指标标签和服务发现 |
| `url` | String | 是 | - | 远程服务基地址 |
| `fallback` | Class | 否 | Void.class | 降级实现类，必须实现该接口 |
| `fallbackFactory` | Class | 否 | Void.class | 降级工厂类，实现 `FallbackFactory<T>`，可获取异常。优先于 `fallback` |

### @SpiderGet / @SpiderPost / @SpiderPut / @SpiderDelete

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `value` | String | 是 | 请求路径，支持 `{变量}` 占位符 |
| `headers` | String[] | 否 | 预留，暂未实现 |

### @Path("name")

绑定方法参数到路径变量。`@SpiderGet("/users/{id}")` 配合 `@Path("id") Long id`。

### @Query("name")

绑定方法参数到 URL 查询参数。`?name=value`。

### @Header("name")

绑定方法参数到 HTTP 请求头。

### @Body

标记方法参数为请求体，由 Encoder 序列化。

### @Timeout

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `value` | int | 无 | 超时时间（毫秒） |

### @Retry

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `maxAttempts` | int | 3 | 最大尝试次数（含首次） |
| `backoffMillis` | long | 100 | 重试间隔（毫秒） |
| `backoffStrategy` | enum | FIXED | FIXED 或 EXPONENTIAL |
| `maxBackoffMillis` | long | 5000 | 指数退避上限 |
| `jitter` | boolean | false | 是否对退避值添加 +/-50% 随机抖动，防止重试风暴 |
| `retryOn` | Class[] | {} | 触发重试的异常类型，空=所有 IOException |
| `ignoreStatus` | int[] | {} | 不重试的 HTTP 状态码 |

开启 jitter 后实际退避为计算值的 50%~150%，适合多副本同时恢复时错峰重试：

```java
@Retry(maxAttempts = 5, backoffStrategy = BackoffStrategy.EXPONENTIAL, jitter = true)
@SpiderGet("/users/{id}")
UserDTO getUser(@Path("id") Long id);
```

### @SpiderCircuitBreaker

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `failureRateThreshold` | int | 50 | 失败率阈值（百分比），超此值开路 |
| `slidingWindowSize` | int | 10 | 滑动窗口大小 |
| `waitDurationInOpenStateMillis` | long | 10000 | 开路后等待多久进入半开（毫秒） |
| `permittedNumberOfCallsInHalfOpenState` | int | 3 | 半开状态允许的试探调用数 |

### @RateLimit

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `permits` | int | 100 | 时间窗口内允许的次数 |
| `duration` | long | 1 | 时间窗口长度 |
| `timeUnit` | TimeUnit | SECONDS | 时间窗口单位 |
| `timeoutMillis` | long | 0 | 等待许可的最大时间，0=立即失败 |

## application.yml

```yaml
spider:
  enabled: true                           # 默认 true，设为 false 禁用自动配置
  default-timeout: 5000                   # 全局默认超时（毫秒）
  default-retry:
    max-attempts: 3
    backoff-millis: 100
  transport:
    connect-timeout: 10000                # OkHttp 连接超时（毫秒）
    read-timeout: 30000                   # OkHttp 读取超时（毫秒）
    write-timeout: 30000                  # OkHttp 写入超时（毫秒）
  console:
    url: http://localhost:18080           # 控制台上报地址
    service-name: my-service              # 本服务名称
  clients:                                # 每客户端独立配置
    user-service:
      url: http://user:8081               # 覆盖 URL
      timeout: 2000                       # 覆盖超时
      retry:
        max-attempts: 5                   # 覆盖重试次数
```

## Builder 选项（编程方式）

```java
SpiderClientFactory.builder()
    .transport(transport)          // 必填，SpiderTransport 实现
    .decoder(decoder)              // 响应解码器
    .encoder(encoder)              // 请求编码器
    .url("http://...")            // 覆盖注解中的 URL
    .circuitBreaker(cb)            // 熔断器实现
    .addInterceptor(interceptor)   // 添加拦截器（可多次调用）
    .interceptors(list)            // 设置拦截器列表
    .metrics(metrics)              // 指标实现
    .serviceDiscovery(sd)          // 服务发现实现
    .build();
```

## 重试行为

| 方法 | 默认是否重试 | 如何改变 |
|---|---|---|
| GET | 是 | `@Retry(maxAttempts = 1)` 禁止 |
| POST/PUT/DELETE | 否 | `@Retry(maxAttempts = 3)` 启用 |
| 4xx 响应 | 否 | 不重试（可通过 `ignoreStatus` 跳过特定码） |
| 5xx 响应 | 是 | `@Retry` 控制次数 |
| IOException | 是（GET） | `retryOn` 筛选 |

## 响应头获取

```java
UserDTO user = client.getUser(1L);
SpiderResponse resp = SpiderResponseContext.lastResponse();
String traceId = resp.headers().get("X-Trace-Id").get(0);
int httpStatus = resp.statusCode();
long elapsed = resp.elapsedMillis();
```

## 系统属性

以下选项通过 JVM 系统属性（`-D` 参数）控制，不通过 `application.yml`：

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `spider.banner` | `false` | 是否在启动时打印 Spider banner。默认关闭以避免日志刷屏；如需查看，设置 `-Dspider.banner=true`。 |

```bash
java -Dspider.banner=true -jar your-app.jar
```

## 动态配置

Spider 通过 `SpiderConfigCenter` SPI 支持配置中心（Apollo、Nacos Config 等）在运行时动态调整客户端参数，无需重启应用。

### 接入方式

实现 `SpiderConfigCenter` 接口并注册为 Spring Bean，starter 自动创建 `ConfigOverrideFilter` 插入过滤器链：

```java
@Component
public class NacosConfigCenter implements SpiderConfigCenter {
    // 从 Nacos 读取配置
}
```

无需实现 `SpiderConfigCenter` 的模块也可直接使用 `InMemoryConfigCenter` 进行编程式更新，适用于测试或外部管理控制台推送。

### 配置键命名规范

| 配置键 | 类型 | 说明 |
|---|---|---|
| `spider.client.<name>.retry.backoff` | long | 重试退避间隔（毫秒） |
| `spider.client.<name>.timeout` | int | 调用超时时间（毫秒） |

`<name>` 为 `@SpiderClient(name = "...")` 中声明的服务名。

### 覆盖优先级

```
ConfigCenter 动态值 > 方法注解 > Spring 属性 > Builder > 框架默认值
```

ConfigCenter 中未配置的 key 不影响现有行为，自动回退到下一优先级。

### 无配置中心时的行为

当容器中不存在 `SpiderConfigCenter` Bean 时（默认状态），`SpiderConfigAutoConfiguration` 不会创建 `ConfigOverrideFilter`，调用链路为零开销，与未引入 `spider-config` 模块时行为完全一致。

## Spring Cloud LoadBalancer 集成

当 classpath 中同时存在 Spring Cloud `LoadBalancerClient` 时，`SpiderLoadBalancerAutoConfiguration` 自动注册 `LoadBalancerSpiderLoadBalancer` 作为 `SpiderLoadBalancer` 实现，复用项目已有的 Spring Cloud 负载均衡策略（权重、健康检查、ZoneAware 等），无需在 Spider 中单独配置。

若项目未引入 Spring Cloud LoadBalancer，Spider 回退到内置的 `RoundRobinSpiderLoadBalancer`，行为不受影响。

## Spring Boot 3 兼容

`spider-spring-boot-starter` 提供 `spring-boot-3` Maven profile，用于在 Spring Boot 3.x 环境中编译：

```bash
mvn compile -pl spider-spring-boot-starter -Pspring-boot-3
```

该 profile 将 `spring-boot.version` 切换为 `3.2.5`，并将 starter 模块的编译目标提升至 Java 17（`spider-core` 等核心模块保持 Java 8 兼容）。Starter 中同时避免直接依赖 `javax.annotation`，改用 `DisposableBean` 等 spring-beans 内稳定 API，确保跨 Spring Boot 主版本兼容。
