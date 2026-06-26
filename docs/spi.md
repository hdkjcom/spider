# Spider SPI 扩展指南

Spider 通过 SPI（Service Provider Interface）实现可扩展架构。所有核心组件均可替换。

## SPI 接口一览

| 接口 | 包 | 用途 | 默认实现 |
|---|---|---|---|
| `SpiderTransport` | `core.transport` | 远程调用传输 | OkHttp、gRPC |
| `SpiderEncoder` | `core.codec` | 请求体编码 | Jackson |
| `SpiderDecoder` | `core.codec` | 响应体解码 | Jackson |
| `SpiderInterceptor` | `core.interceptor` | 请求/响应拦截 | — |
| `SpiderMetrics` | `core.metrics` | 指标记录 | NOOP、Micrometer |
| `SpiderServiceDiscovery` | `core.discovery` | 服务发现 | 内存、Nacos |
| `SpiderLoadBalancer` | `core.discovery` | 负载均衡 | 轮询、随机 |
| `SpiderCircuitBreaker` | `core.policy` | 熔断器 | 计数、Resilience4j |
| `SpiderConfigCenter` | `config` | 动态配置 | 内存 |

## 自定义 Transport

```java
public class CustomTransport implements SpiderTransport {
    @Override
    public SpiderResponse execute(SpiderRequest request) throws IOException {
        // 自定义传输逻辑
        return new SpiderResponse()
                .statusCode(200)
                .bodyBytes("response".getBytes());
    }
}

// 使用
SpiderClientFactory.builder()
    .transport(new CustomTransport())
    .build();
```

## 自定义 Interceptor

```java
public class LoggingInterceptor implements SpiderInterceptor {
    @Override
    public SpiderRequest beforeRequest(SpiderRequest request) {
        log.info(">>> {} {}", request.method(), request.fullUrl());
        return request;
    }

    @Override
    public SpiderResponse afterResponse(SpiderResponse response) {
        log.info("<<< {} ({}ms)", response.statusCode(), response.elapsedMillis());
        return response;
    }

    @Override
    public boolean onError(SpiderRequest request, Exception ex) {
        log.error("!!! {} failed: {}", request.fullUrl(), ex.getMessage());
        return false;  // false = 继续抛出异常
    }
}
```

## 自定义 ServiceDiscovery

```java
public class ConsulDiscovery implements SpiderServiceDiscovery {
    @Override
    public List<String> resolve(String serviceName) {
        // 从 Consul 查询服务实例
        return consulClient.lookup(serviceName);
    }
}
```

## 自定义 LoadBalancer

```java
public class WeightedLoadBalancer implements SpiderLoadBalancer {
    @Override
    public String choose(String serviceName, List<String> instances) {
        // 加权选择逻辑
        return selectByWeight(instances);
    }
}
```

## 自定义 CircuitBreaker

```java
public class SentinelCircuitBreaker implements SpiderCircuitBreaker {
    @Override public boolean isAllowed() { ... }
    @Override public void recordSuccess() { ... }
    @Override public void recordFailure(Throwable t) { ... }
    @Override public State state() { ... }
}
```

## 自定义 ConfigCenter

```java
public class ApolloConfigCenter implements SpiderConfigCenter {
    @Override public String get(String key) { ... }
    @Override public void watch(String key, Consumer<String> callback) { ... }
}
```

## Spring Boot 中的 Bean 覆盖

所有自动配置的 Bean 都有 `@ConditionalOnMissingBean`，只需定义同名 Bean 即可覆盖：

```java
@Bean
public SpiderTransport spiderTransport() {
    return new CustomTransport();
}

@Bean
public SpiderInterceptor loggingInterceptor() {
    return new LoggingInterceptor();
}
```

## 过滤器链

Spider 内部使用过滤器链模型编排调用生命周期。如需自定义治理逻辑，可以实现 `SpiderInvocationFilter` 并插入链中：

```java
public class CustomFilter implements SpiderInvocationFilter {
    @Override
    public Object filter(SpiderInvocationContext ctx, SpiderFilterChain chain) throws Throwable {
        // 前置逻辑
        Object result = chain.next(ctx);
        // 后置逻辑
        return result;
    }
}
```
