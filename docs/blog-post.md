# 我写了一个中间件，把微服务调用的 try-catch 从 47 个减到了 0

> 每一个对接过三个以上外部服务的 Java 程序员，都曾经在某个深夜问过自己：「为什么我要为每一个接口手写一模一样的重试逻辑？」

---

## 你写过多少次这样的代码？

```java
public UserDTO getUser(Long id) {
    for (int i = 0; i < 3; i++) {
        try {
            Response resp = httpClient.get("http://user-service/users/" + id);
            if (resp.code() == 200) return mapper.readValue(resp.body(), UserDTO.class);
            if (resp.code() >= 400 && resp.code() < 500) break;
        } catch (IOException e) {
            if (i == 2) throw new RuntimeException(e);
            sleep(i * 100);
        }
    }
    return fallback();
}
```

这段代码在你的项目里出现过多少次？

- 超时写死 30 秒，没人敢改
- 重试用 `Thread.sleep` 退避，掐指一算就写了个 100ms
- 异常 `catch (Exception e)` 一把梭
- 熔断？「那是什么」
- 降级？「还没上线呢，不急」
- 监控？「有 Grafana 但我没配」

然后有一天业务挂了，老板问「调用成功率多少」，你沉默了。

## OpenFeign 解决了一半

Spring Cloud OpenFeign 确实优雅地解决了 HTTP 调用的声明式写法：

```java
@FeignClient(name = "user-service", url = "http://localhost:8081")
public interface UserClient {
    @GetMapping("/users/{id}")
    UserDTO getUser(@PathVariable Long id);
}
```

接口即文档，注入即调用——这很好。

但问题是：**OpenFeign 只管调用，不管治理。**

- 重试？需要额外引入 Spring Retry
- 熔断？需要额外引入 Sentinel 或 Resilience4j
- 限流？Feign 原生不支持
- 降级？需要 Spring Cloud CircuitBreaker + 手动配置
- 指标？需要 Micrometer + 自己埋点
- 监控面板？自己搭吧

**Feign 帮你省了 HTTP 代码，但治理那一坨还是你自己来。**

## 我们想要什么？

能不能这样：

```java
@SpiderClient(name = "user-service", url = "http://localhost:8081",
              fallback = UserFallback.class)
@SpiderCircuitBreaker(failureRateThreshold = 50)
public interface UserClient {

    @SpiderGet("/users/{id}")
    @Timeout(2000)
    @Retry(maxAttempts = 3, backoffStrategy = EXPONENTIAL, jitter = true)
    UserDTO getUser(@Path("id") Long id);
}
```

一个接口，注解声明调用方式 + 超时 + 重试 + 熔断 + 降级。注入即用。然后打开 `http://localhost:8086/spider`，Dashboard 已经在等你——调用次数、成功率、p50/p90/p99、最近调用趋势图、熔断器状态，全在。

**没部署任何监控组件。没写一行 OkHttp 代码。**

这就是 [Spider](https://github.com/hdkjcom/spider)。

## Spider 做了什么

Spider 是一个声明式服务调用治理中间件。它的核心思路是：

> **OkHttp 负责发请求，Jackson 负责序列化，Resilience4j 负责熔断，Micrometer 负责指标——Spider 负责把它们编排到一起，变成一个注解。**

架构上是过滤器链模型：9 个独立的 filter 按顺序执行——服务发现 → 请求构建 → 拦截器 → 降级 → 监控 → 重试 → 传输 → 解码——每一步都是可插拔可替换的。

这意味着：
- 你的 HttpTransport 可以换成 Netty 的，不用改接口
- 你的服务发现可以换成 Consul 的，不用改接口
- 你的熔断器可以换成 Sentinel 的，不用改接口
- 甚至你的 JSON 库都可以换成 Gson——只要你愿意

**Spider 不锁定任何底层实现。**

## vs OpenFeign：不只是调用，还有治理

| | Spider | OpenFeign |
|---|---|---|
| 声明式接口 | ✅ | ✅ |
| 重试 | ✅ 内置，智能跳过 4xx | 需 Spring Retry |
| 熔断 | ✅ 内置 | 需 Sentinel/Hystrix |
| 限流 | ✅ 内置 | ❌ |
| 降级 | ✅ 内置，FallbackFactory | 需 Spring Cloud |
| 负载均衡 | ✅ 内置 | 需 Ribbon/LoadBalancer |
| Dashboard | ✅ 嵌入式，零配置 | ❌ |
| Actuator 端点 | ✅ `/actuator/spider` | ❌ |
| 连接池可观测 | ✅ `/actuator/spider-pool` | ❌ |
| 动态配置 | ✅ Apollo/Nacos Config | ❌ |
| 异步调用 | ✅ CompletableFuture | ❌ |
| Spring Cloud 兼容 | ✅ DiscoveryClient + LoadBalancer | ✅ |
| Java 8 | ✅ | ✅ |

## 一个真实的故事

浩轩是一名 Java 开发。周三下午，PM 让他对接用户服务的 API。

他加了一个依赖。写了一个接口。加了几个注解。注入到 Controller。跑了。

**六分钟。** 他还没写 OkHttp，还没配连接池，还没写 try-catch，还没写 JSON.parse。他甚至还没泡茶。

然后他打开 `http://localhost:8086/spider`——一个完整的 Dashboard 浮现在屏幕上。调用次数、成功率、p50/p90/p99、最近调用趋势、熔断器状态，全在。

他没部署任何监控组件。

浩轩靠在椅背上，喝了一口还没泡的茶。他开始怀疑自己以前的工作量。

一个月后，他已经接入了 4 个微服务，配置整齐划一，Nacos 服务发现自动生效，Apollo 动态调参不用重启，OpenAPI spec 一键生成客户端代码。

同事路过他的工位，看着 Dashboard 问：「这是什么监控平台？」

「Spider。」浩轩说，「一个依赖，一个注解。六分钟。」

## 快速开始

```xml
<dependency>
    <groupId>io.github.hdkjcom.spider</groupId>
    <artifactId>spider-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

```java
@SpringBootApplication
@EnableSpiderClients
public class App { public static void main(String[] args) { SpringApplication.run(App.class, args); } }

@SpiderClient(name = "user-service", url = "http://localhost:8081")
public interface UserClient {
    @SpiderGet("/users/{id}") UserDTO getUser(@Path("id") Long id);
}

@RestController
public class Ctrl {
    @Autowired UserClient client;
    @GetMapping("/u/{id}") public UserDTO get(@PathVariable Long id) { return client.getUser(id); }
}
```

访问 `http://localhost:8086/spider` 看监控。

## 最后

这个项目是我在业余时间写的。从最开始的一个想法——"为什么对接外部服务这么麻烦"——到 1.0.0 发版，经历了 30 多个版本迭代、170 个单元测试、无数次 bug 修复。

如果你也觉得手写 HTTP 调用代码很烦，试试 Spider。

**人生没有 Ctrl+Z，但永远可以 `git checkout` 到一个新的分支。真正重要的，不是你曾经在哪个分支提交过多少代码，而是最后，你把自己 merge 到了一个真正想成为的人生里。**

---

- GitHub: [https://github.com/hdkjcom/spider](https://github.com/hdkjcom/spider)
- Gitee: [https://gitee.com/suxuZZR/spider](https://gitee.com/suxuZZR/spider)
- Maven Central: `io.github.hdkjcom.spider:spider-spring-boot-starter:1.0.0`
- 文档: [https://github.com/hdkjcom/spider](https://github.com/hdkjcom/spider)
