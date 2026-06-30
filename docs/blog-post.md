# 我写了一个中间件，把微服务调用的 try-catch 从 47 个减到了 0

> 说实话，这个项目本来只是我一个周三下午的突发奇想。没想到后来我真的把它写到了 1.0.0，发到了 Maven Central，还给它画了一个 Dashboard。

---

## 那个让我崩溃的瞬间

我叫浩轩，一名普通的 Java 后端开发。

那是 2026 年初的一个下午。产品经理在钉钉上发了三个字：「用户服务。」

我知道这意味着什么——对接用户服务的 API。获取用户信息、创建用户、更新头像。标准的 CRUD。

我开始在心里盘算工作量：

1. 引入 OkHttp 依赖
2. 配置连接池：连接超时多长？读超时多长？最大空闲连接？保活多久？——我每次都要翻 OkHttp 文档，因为我永远记不住
3. 写一个 HttpClientUtil，封装 GET / POST / PUT / DELETE
4. 每次调用手动拼 URL，加 query 参数，设 header
5. 拿到 Response 后判断状态码：200 要解析，404 要打日志，500 要不要重试？
6. 重试逻辑自己写——重试几次？退避多久？指数还是固定？POST 要不要重试？
7. 每个接口 try-catch，异常类型越来越多，最后 `catch (Exception e)` 一锅端
8. 老板问「调用成功率多少」——沉默，因为根本没埋点

我数了一下，项目里已经有 47 个 try-catch 块。Forty-seven。

那天我没有写代码。我在工位上坐了一个小时，然后打开 IDEA，新建了一个项目。

我叫它 **Spider**。

## 我想要的东西很简单

「能不能这样——」

我对着空白的 README 自言自语：

「我写一个接口，加几个注解。@SpiderGet 就是 GET 请求，@SpiderPost 就是 POST 请求。然后我直接 @Autowired 注入，像调本地方法一样用。」

「然后，超时？@Timeout。重试？@Retry。熔断？@SpiderCircuitBreaker。降级？fallback。」

「然后，我打开浏览器，一个 Dashboard 已经在那儿了——调用次数、成功率、p50、p99、熔断器状态。不用部署任何监控组件。」

「然后，如果要接 Nacos 做服务发现？url 留空就行，自动走注册中心。如果要运行时调超时？在 Apollo 改个配置，不重启生效。」

听起来像魔法。但在那天下午，我开始写代码了。

## 故事最精彩的部分不是我写出了这个项目，而是我真的把它用在项目里了

我用 Spider 对接了第一个外部服务——微信 API。

六分钟。从加依赖到看到返回结果。六分钟。

我甚至还没泡茶。

然后我打开 `http://localhost:8086/spider`——我现在还记得那个瞬间。调用成功，Dashboard 上出现了一条绿色记录。再调一次。又一条。成功率那个数字在跳，p99 延迟那个数字在跳，迷你趋势图上那一排绿色竖条在跳。

我组的后端架构并没有部署 Grafana。没有 Prometheus。没有 ELK。我什么都没装。但 Dashboard 就在那儿。

我靠在椅背上，喝了一口还没泡的茶。

我开始怀疑我以前的工作量。

## 后来，我一个一个往回补，把该有的都补上了

最开始 Spider 只是一个简单的代理工具。后来我发现，只有调用不够，得有**治理**。于是加了重试、熔断、限流、降级。

后来我发现，治理逻辑如果全写在一个 Handler 里，会变成 200 行的大怪兽。于是我把它拆成了 9 个独立的过滤器——服务发现、请求构建、拦截器、降级、监控、重试、传输、解码——每个 filter 只做一件事，可插拔、可替换、可重排。

后来我又发现，熔断器给了 Resilience4j 的适配，服务发现对接了 Spring Cloud DiscoveryClient，负载均衡复用了 Spring Cloud LoadBalancer。你配了 `spring.cloud.nacos.discovery.server-addr`，Spider 自动就能用——不需要再写 `spider.nacos.*`。

后来我甚至加了——说出来你可能不信——**暗色模式**。因为有一次晚上加班，Dashboard 太白，晃得我眼睛疼。

## Spider 现在的样子

一个注解，声明式调用 + 全量治理：

```java
@SpiderClient(name = "user-service", url = "http://localhost:8081",
              fallback = UserFallback.class)
@SpiderCircuitBreaker(failureRateThreshold = 50)
public interface UserClient {

    @SpiderGet("/users/{id}")
    @Timeout(2000)
    @Retry(maxAttempts = 3, backoffStrategy = EXPONENTIAL, jitter = true)
    UserDTO getUser(@Path("id") Long id);

    @SpiderPost("/users")
    UserDTO createUser(@Body CreateUserRequest req);
}
```

然后注入，调用，看一眼 Dashboard。你甚至不需要理解什么是过滤器链、什么是 SPI、什么是 ErrorCategory——你先用，这些东西自然就懂了。

## 这不是什么高大上的项目

Spider 没有重新发明轮子。它只是在 OkHttp、Jackson、Resilience4j、Micrometer、Nacos 这些成熟组件之上，做了一层**编排**。让它们像一个整体一样工作。

它的定位不是「干掉 OpenFeign」，更不是「成为 Dubbo」。它只是想解决一个很朴素的问题——

> **为什么对接一个外部服务，研发要操心 HTTP 怎么发、JSON 怎么解、重试怎么写、熔断怎么配、监控怎么搭？这些不应该是基础的、自动化的事情吗？**

我觉得应该是。

所以我写了 Spider。

## 关于我

我是一个用 Java 8 写后端的人。Spider 从 0.1.0 到 1.0.0 用了几十个版本、170 个单元测试、数不清的深夜 debug。曾经有一个重试 bug 让我意识到——代码里所有"看起来在重试"的循环，根本就没有重新执行传输层。那个 bug 我修了一个晚上。

但我修好了。后来也没有再出现。

如果你也是那种"不想为每个接口写 try-catch"的人，试试 Spider。哪怕你不用，看一眼设计思路也好。

人生没有 Ctrl+Z，但永远可以 `git checkout` 到一个新的分支。真正重要的，不是你曾经在哪个分支提交过多少代码，而是最后，你把自己 merge 到了一个真正想成为的人生里。

---

- 🐙 **GitHub**: [https://github.com/hdkjcom/spider](https://github.com/hdkjcom/spider)
- 🐴 **Gitee**: [https://gitee.com/suxuZZR/spider](https://gitee.com/suxuZZR/spider)
- 📦 **Maven Central**: `io.github.hdkjcom.spider:spider-spring-boot-starter:1.0.0`
- 📖 **新手入门**: [docs/quickstart.md](docs/quickstart.md)
