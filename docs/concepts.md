# Spider 错误处理

## 异常层次结构

所有 Spider 异常继承自 `SpiderException`（抽象基类），每种异常都有明确的 `ErrorCategory`：

```
SpiderException (abstract, RuntimeException)
  ├── SpiderConfigurationException      CONFIG          永不重试
  ├── SpiderServiceDiscoveryException   SERVICE_DISCOVERY 可重试
  ├── SpiderCircuitBreakerOpenException CIRCUIT_BREAKER  永不重试
  ├── SpiderRateLimitException          RATE_LIMIT      永不重试
  ├── SpiderHttpException               (有 statusCode)
  │     ├── SpiderHttpClientException   HTTP_CLIENT/4xx  永不重试
  │     └── SpiderHttpServerException   HTTP_SERVER/5xx  可重试
  ├── SpiderIOException                 NETWORK_IO      可重试
  ├── SpiderFallbackException           FALLBACK        降级失败
  └── SpiderContractViolationException  CONTRACT        契约违反
```

`SpiderClientException` 保留用于向后兼容，继承自 `SpiderException`。

## ErrorCategory

| 分类 | 说明 | 重试建议 |
|---|---|---|
| `CONFIG` | 配置/启动错误 | 永不重试 |
| `SERVICE_DISCOVERY` | 服务发现失败 | 可短暂重试 |
| `CIRCUIT_BREAKER` | 熔断器拒绝 | 永不重试 |
| `RATE_LIMIT` | 限流拒绝 | 永不重试 |
| `HTTP_CLIENT` | HTTP 4xx 客户端错误 | 永不重试 |
| `HTTP_SERVER` | HTTP 5xx 服务端错误 | 可重试 |
| `NETWORK_IO` | 网络 I/O 故障 | 可重试 |
| `FALLBACK` | 降级执行失败 | — |
| `CONTRACT` | 契约校验失败 | — |

## 重试行为

`RetryFilter` 根据异常类型自动决定是否重试：

- **永不重试**：`SpiderConfigurationException`、`SpiderCircuitBreakerOpenException`、`SpiderRateLimitException`、`SpiderHttpClientException`
- **可重试**：`SpiderHttpServerException`、`SpiderIOException`、`SpiderServiceDiscoveryException`、`IOException`
- 重试策略（次数、退避）由 `@Retry` 注解控制

## 程序化异常处理

```java
try {
    userClient.getUser(1L);
} catch (SpiderHttpClientException e) {
    // HTTP 4xx — 请求方错误，检查参数
    log.warn("Client error: {} {}", e.statusCode(), e.getMessage());
} catch (SpiderHttpServerException e) {
    // HTTP 5xx — 服务端错误，可能已自动重试
    log.error("Server error: {} {}", e.statusCode(), e.getMessage());
} catch (SpiderCircuitBreakerOpenException e) {
    // 熔断器开启 — 快速失败
    log.error("Circuit breaker open: {}", e.circuitBreakerName());
} catch (SpiderException e) {
    // 其他 Spider 异常
    log.error("Spider error: {} category={}", e.getMessage(), e.category());
}
```

## 降级 (Fallback)

```java
@SpiderClient(name = "user-service", fallback = UserFallback.class)
public interface UserClient {
    @SpiderGet("/users/{id}")
    UserDTO getUser(@Path("id") Long id);
}

public class UserFallback implements UserClient {
    @Override
    public UserDTO getUser(Long id) {
        return UserDTO.empty(id);  // 返回降级值
    }
}
```

使用 `FallbackFactory` 获取异常原因：

```java
@SpiderClient(name = "user-service", fallbackFactory = UserFallbackFactory.class)
public interface UserClient { ... }

public class UserFallbackFactory implements FallbackFactory<UserClient> {
    @Override
    public UserClient create(Throwable cause) {
        log.warn("Fallback triggered by: {}", cause.getMessage());
        return new UserFallback();
    }
}
```
