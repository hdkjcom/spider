# Spider 可观测性

## 指标

引入 starter 后自动通过 Micrometer 暴露，Prometheus/Grafana 可直接采集。

| 指标 | 类型 | 标签 |
|---|---|---|
| `spider.client.requests` | Counter | client, method, outcome (success/failure), error_type |
| `spider.client.retries` | Counter | client, method, error_type |
| `spider.client.fallbacks` | Counter | client, method |
| `spider.client.duration` | Timer | client, method |

**error_type 取值**：`SpiderHttpClientException`、`SpiderHttpServerException`、`SpiderIOException`、`SpiderCircuitBreakerOpenException`、`SpiderRateLimitException` 等。

## 链路追踪

引入 starter 后自动激活（传递依赖 `spider-telemetry`），无需配置。

### Span 属性

| 属性 | 说明 |
|---|---|
| `http.method` | GET / POST / PUT / DELETE |
| `http.url` | 请求 URL（已去除 query 参数，不泄露敏感信息） |
| `http.status_code` | 响应 HTTP 状态码 |
| `spider.protocol` | http |
| `error` | 调用是否失败 |
| `exception.type` | 异常类型名（仅失败时） |

### Span 名称

`GET /users/{id}` — HTTP 方法 + 路径模板。

### W3C Trace-Context

自动向请求头注入 `traceparent`，下游服务可继续链路。

### 示例

```text
订单服务 (span)
  └── GET /sns/jscode2session (span)
       ├── http.status_code: 200
       ├── duration: 24ms
       └── spider.protocol: http
```

## 控制台 Dashboard

### 单服务模式（默认）

不需要任何配置。引入 starter 后直接访问 `http://你的端口/spider`：

- 服务概览（调用次数、成功率、p50/p90/p99）
- 客户端列表和指标
- 熔断器状态
- 最近调用快照
- 追踪状态

数据从本地 `SpiderRuntime` 实时读取，不走上报链路。

### 多服务统一监控

当有多个微服务需要汇总查看时，部署一个中央控制台：

```yaml
# 各业务服务
spider:
  console:
    url: http://spider-console:18080
    service-name: order-service
```

```bash
# 中央控制台
mvn exec:java -pl spider-console -Dexec.mainClass=io.github.spider.console.SpiderConsoleApplication
```

业务服务定时（30s）向中央控制台上报指标，控制台汇总展示所有服务的调用情况。

## Actuator 端点

| 端点 | 内容 |
|---|---|
| `GET /actuator/spider` | 运行时摘要（活跃客户端数、总调用次数） |
| `GET /actuator/spider/clients` | 所有客户端详情 + 熔断器状态 + 最近错误 |
| `GET /actuator/spider/clients/{name}` | 单个客户端：QPS、成功率、p50/p90/p99 |
| `GET /actuator/health` | 健康状态（含熔断器状态、每客户端指标） |
