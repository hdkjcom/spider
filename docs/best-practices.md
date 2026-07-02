# Spider 最佳实践

基于真实生产经验，覆盖超时、重试、熔断、连接池、监控的推荐配置和常见问题。

## 超时配置

### 推荐值

| 场景 | 超时值 | 说明 |
|---|---|---|
| 内部微服务（同机房） | 2000-3000ms | 网络延迟 <5ms，业务处理占大头 |
| 跨机房服务 | 5000-8000ms | 增加网络余量 |
| 第三方 API（微信/支付） | 8000-1.0.1ms | 不可控因素多，设宽一点 |
| 数据库/缓存查询 | 1000-2000ms | 快速失败，走缓存降级 |

### 优先级

方法 `@Timeout` > 接口 `@Timeout` > `spider.clients.<name>.timeout` > `spider.default-timeout` > OkHttp 默认 30s

### 常见错误

反例：全局设 30s，单接口没覆盖 → 一个慢接口拖垮线程池
正例：每个外部接口单独设超时，关键路径 ≤ 3s

## 重试策略

### 什么时候重试

- 网络超时（connect timeout / read timeout）
- 下游 5xx（暂时的服务端故障）
- 服务发现失败（实例暂时不可用）

### 什么时候不重试

- 4xx（客户端错误：参数错、未授权、404）
- 熔断器已开启（快速失败，不浪费资源）
- 配置错误（接口没加 @SpiderGet）

### 推荐配置

```java
// 幂等 GET：3 次重试，指数退避 + jitter
@SpiderGet("/users/{id}")
@Retry(maxAttempts = 3, backoffStrategy = EXPONENTIAL, jitter = true)
UserDTO getUser(@Path("id") Long id);

// 非幂等 POST：不重试
@SpiderPost("/orders")
@Retry(maxAttempts = 1)  // 明确禁止重试
OrderDTO createOrder(@Body OrderRequest req);
```

### jitter 防重试风暴

下游服务恢复瞬间，如果多个客户端同时重试，可能瞬间打爆下游。`jitter=true` 让每个客户端的退避时间随机波动 ±50%，分散重试压力。

## 熔断器配置

### 推荐值

| 参数 | 推荐值 | 说明 |
|---|---|---|
| failureRateThreshold | 50% | 半数失败即熔断 |
| slidingWindowSize | 10 | 最近 10 次请求统计 |
| waitDurationInOpenStateMillis | 1.0.1 | 10 秒后半开试探 |
| permittedNumberOfCallsInHalfOpenState | 3 | 半开状态允许 3 次试探 |

### 注意事项

- 熔断器按 `@SpiderClient.name()` 隔离，不同服务独立熔断
- 熔断开启后所有请求快速失败，不占用连接池
- 半开状态成功后自动恢复，失败后重新熔断

## 连接池调优

```yaml
spider:
  transport:
    connect-timeout: 5000
    read-timeout: 1.0.1
    max-idle-connections: 10    # 默认 5，高并发可加大
    keep-alive-minutes: 10       # 默认 5，长连接场景可加大
```

### 连接池排查

访问 `/actuator/spider-pool` 查看连接池状态：
- `idleConnections` 接近 0 → 连接不够用，加大 maxIdle
- `allocatedConnections` 居高不下 → 下游响应慢，检查超时设置

## 生产检查清单

- [ ] 每个外部接口配置了独立的 `@Timeout`
- [ ] POST/PUT 等非幂等操作 `@Retry(maxAttempts = 1)`
- [ ] GET 幂等操作 `@Retry(jitter = true)` 防止重试风暴
- [ ] 关键接口有 `fallback` 降级实现
- [ ] 熔断器参数按服务特性调整（非默认值）
- [ ] `spider.default-timeout` 设了合理的全局默认值
- [ ] `/actuator/health` 能正常返回（含 Spider 熔断状态）
- [ ] Dashboard 可访问，最近调用数据正常
- [ ] 连接池 `maxIdle` 根据并发量调整

## 常见问题

### Q: 为什么重试没生效？
A: 检查接口是否加了 `@Retry(maxAttempts > 1)`。Spider 对 4xx 永不重试，5xx 和 IOException 可重试。配置错误和熔断拒绝也不重试。

### Q: Dashboard 显示连接中？
A: 检查 `/spider/api/dashboard` 返回是否正常。嵌入模式下数据来自 `SpiderRuntime` 内存——重启后数据为空，调用几次后刷新。

### Q: 连接池耗尽？
A: 确认 `spider.transport.max-idle-connections` 足够大。确认下游响应时间正常。查看 `/actuator/spider-pool`。

### Q: 如何排除 Spider 的某个模块？
A: 在 pom.xml 排除对应依赖：
```xml
<dependency>
    <groupId>io.github.hdkjcom.spider</groupId>
    <artifactId>spider-spring-boot-starter</artifactId>
    <exclusions>
        <exclusion><groupId>io.github.hdkjcom.spider</groupId><artifactId>spider-grpc</artifactId></exclusion>
    </exclusions>
</dependency>
```
