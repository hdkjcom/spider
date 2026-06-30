# 我写了一个中间件，把微服务调用的 try-catch 从 47 个减到了 0

> 说实话，这个项目本来只是我一个周三下午的突发奇想。没想到后来我真的把它写到了 1.0.0，发到了 Maven Central，还给它画了一个 Dashboard。

---

## 那个让我崩溃的瞬间

我叫浩轩，一名普通的 Java 后端开发。

那是年初的一个下午。产品经理在钉钉上发了三个字：「用户服务。」

我知道这意味着什么——对接用户服务的 API，获取用户信息，创建用户，更新头像。标准的 CRUD。然后我开始在心里盘算工作量：

引入 OkHttp → 配连接池（超时多长？我每次都翻文档）→ 写 HttpClientUtil → 手动拼 URL → 判断状态码 → 写重试逻辑 → 每个接口 try-catch → 47 个了。

我一直往下翻。翻到第 47 个 catch 块的时候停下了。

那天我没有写代码。我在工位上坐了一个小时，然后打开 IDEA，新建了一个项目。我叫它 **Spider**。

## 我想要的东西很简单

「能不能这样——」我对着空白的 README 自言自语。「我写一个接口，加几个注解。@SpiderGet 就是 GET。@Retry 就是重试。@SpiderCircuitBreaker 就是熔断。然后我 @Autowired 注入，像调本地方法一样用。然后打开浏览器，一个 Dashboard 已经在等我了——没部署任何监控组件。」

听起来像魔法。但在那天下午，我开始写代码了。

## 故事最精彩的部分不是我写出了这个项目，而是我真的把它用在项目里了

我用 Spider 对接了第一个外部服务——微信 API。

**六分钟。** 从加依赖到看到返回结果。六分钟。我甚至还没泡茶。

然后我打开 `http://localhost:8086/spider`。调用成功，Dashboard 上出现了一条绿色记录。再调一次，又一条。成功率在跳，p99 延迟在跳，迷你趋势图上那一排绿色竖条在跳。

我靠在椅背上，喝了一口还没泡的茶。我开始怀疑我以前的工作量。

## 后来，我一个一个往回补

最开始 Spider 只是一个简单的代理工具。后来我发现，治理逻辑如果全写在一个 Handler 里会变成 200 行的大怪兽。于是拆成了 9 个独立的过滤器——服务发现、请求构建、拦截器、降级、监控、重试、传输、解码——每个 filter 只做一件事，可插拔、可替换。

后来我又发现，熔断器可以接 Resilience4j，服务发现可以复用 Spring Cloud DiscoveryClient，负载均衡可以复用 Spring Cloud LoadBalancer。你配了 `spring.cloud.nacos.discovery.*`，Spider 自动就能用——不需要再写 `spider.nacos.*`。

我甚至还加了**暗色模式**。因为有一次晚上加班，Dashboard 太白，晃得我眼睛疼。

## Spider 现在的样子

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

然后注入，调用，看一眼 Dashboard。你甚至不需要理解什么是过滤器链、什么是 SPI——你先用，这些东西自然就懂了。

## 这不是什么高大上的项目

Spider 没有重新发明轮子。它只是在 OkHttp、Jackson、Resilience4j、Micrometer、Nacos 之上做了一层**编排**。它想解决的问题很朴素：

> **为什么对接一个外部服务，研发要操心 HTTP 怎么发、JSON 怎么解、重试怎么写、熔断怎么配、监控怎么搭？这些不应该是基础的、自动化的事情吗？**

我觉得应该是。所以我写了 Spider。从 0.1.0 到 1.0.0，几十个版本迭代，170 个单元测试，无数个深夜 debug。有一个重试 bug 让我意识到——代码里所有"看起来在重试"的循环，根本就没有重新执行传输层。那个 bug 我修了一个晚上。但我修好了，后来也没有再出现。

如果你也是那种"不想为每个接口写 try-catch"的人，试试 Spider。哪怕你不用，看一眼设计思路也好。

---

**人生没有 Ctrl+Z，但永远可以 `git checkout` 到一个新的分支。真正重要的，不是你曾经在哪个分支提交过多少代码，而是最后，你把自己 merge 到了一个真正想成为的人生里。**

- 🐙 **GitHub**: [https://github.com/hdkjcom/spider](https://github.com/hdkjcom/spider)
- 🐴 **Gitee**: [https://gitee.com/suxuZZR/spider](https://gitee.com/suxuZZR/spider)
- 📦 **Maven Central**: `io.github.hdkjcom.spider:spider-spring-boot-starter:1.0.0`
