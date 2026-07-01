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

## 控制台 Dashboard

### 单服务模式（默认）

不需要任何配置。引入 starter 后直接访问 `http://你的端口/spider`：

- 服务概览（调用次数、成功率、p50/p90/p99）
- 客户端列表和指标
- 熔断器状态
- 最近调用快照

数据从本地 `SpiderRuntime` 实时读取，不走上报链路。

### 多服务统一监控

当有多个微服务需要汇总查看时，部署一个中央控制台：

```yaml
# 各业务服务
spider:
  console:
    url: http://spider-console:1.0.1
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
| `GET /actuator/spider-pool` | OkHttp 连接池状态：空闲/总连接数、最大空闲、保活时长 |
| `GET /actuator/health` | 健康状态（含熔断器状态、每客户端指标） |

`/actuator/spider-pool` 仅在 classpath 包含 Spring Boot Actuator 和 OkHttp 时自动注册。返回字段包括 `idleConnections`、`totalConnections`、`allocatedConnections`、`maxIdleConnections`、`keepAliveDurationMillis` 等，兼容 OkHttp 4.9.x 和 4.12+ 版本。

## 暗色模式

Dashboard 控制台支持浅色/暗色主题切换。页面首次加载时自动跟随系统偏好（`prefers-color-scheme`），也可通过右上角按钮手动切换，偏好保存在 `localStorage` 中跨会话持久化。CSS 变量通过 `[data-theme="dark"]` 选择器切换，覆盖背景、文字、边框、徽章等全部颜色 token。
