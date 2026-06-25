# Spider 错误处理指南

## 异常类型

| 异常 | 抛出时机 | 携带信息 |
|---|---|---|
| `SpiderClientException` | HTTP 响应非 2xx，熔断器拒绝请求，Fallback 执行失败，URL 配置错误 | `statusCode`（HTTP 状态码，-1 表示非 HTTP 错误），`message`（错误描述） |
| `IOException` | 网络连接失败、超时、DNS 解析失败 | 原始 I/O 异常 |
| `RuntimeException` | `SpiderDecoder` 解码失败、protobuf 解析失败 | 原始异常 |

## 重试行为矩阵

| 场景 | GET | POST/PUT/DELETE | 可配置 |
|---|---|---|---|
| 网络异常 (`IOException`) | 重试 | 不重试（除非`@Retry`声明） | `retryOn` / `maxAttempts` |
| 连接超时 | 重试 | 不重试（同上） | 同上 |
| 5xx (500-599) | 重试 | 不重试（同上） | `ignoreStatus` |
| 4xx (400-499) | 不重试 | 不重试 | `ignoreStatus`可精确忽略 |
| 熔断器 OPEN | 不重试 | 不重试 | 直接抛异常 |

## 降级 (Fallback) 触发条件

所有重试耗尽后，依次执行：拦截器 onError -> Fallback (如有配置) -> 抛异常给调用方。

Fallback 两种方式：
- 静态降级：`@SpiderClient(fallback = XxxFallback.class)` — 无法获取异常信息
- 工厂降级：`@SpiderClient(fallbackFactory = XxxFactory.class)` — 可获取 `Throwable cause`

## 熔断器状态转换

```
CLOSED --(失败率大于等于阈值)--> OPEN --(等待时间到)--> HALF_OPEN
  ^                                                      |
  |----(试探成功)------------------------------------------|
  |
  |----(试探失败)--> OPEN
```

- CLOSED：正常调用
- OPEN：直接拒绝，抛出 `SpiderClientException("Circuit breaker is OPEN")`
- HALF_OPEN：允许少量试探调用

## 拦截器错误处理

```java
SpiderInterceptor {
    beforeRequest(req)  // 返回修改后的请求，抛异常则中断调用
    afterResponse(resp) // 返回修改后的响应
    onError(req, ex)    // 返回 true 吞掉异常（调用方收到 null）
}
```

## 日志

所有关键节点有 SLF4J 日志：

| 日志级别 | 内容 |
|---|---|
| INFO | 代理创建、Fallback 触发 |
| WARN | 调用失败、熔断器 OPEN、限流触发 |
| DEBUG | 每次调用耗时、重试次数 |
| ERROR | Fallback 执行失败 |

## 自定义异常处理示例

```java
SpiderClientFactory.builder()
    .addInterceptor(new SpiderInterceptor() {
        @Override
        public boolean onError(SpiderRequest req, Exception ex) {
            if (ex instanceof SpiderClientException) {
                int code = ((SpiderClientException) ex).statusCode();
                if (code == 404) return true; // 404 不抛异常，返回 null
            }
            return false;
        }
    })
    .build();
```
