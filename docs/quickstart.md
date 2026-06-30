# Spider Quick Start

5分钟上手 Spider 声明式服务调用中间件。

## 1. 添加依赖

**Spring Boot 项目（推荐）：**

```xml
<dependency>
    <groupId>io.github.hdkjcom.spider</groupId>
    <artifactId>spider-spring-boot-starter</artifactId>
    <version>0.1.9</version>
</dependency>
```

**纯 Java 项目：**

```xml
<dependency><groupId>io.github.hdkjcom.spider</groupId><artifactId>spider-core</artifactId><version>0.1.9</version></dependency>
<dependency><groupId>io.github.hdkjcom.spider</groupId><artifactId>spider-http</artifactId><version>0.1.9</version></dependency>
<dependency><groupId>io.github.hdkjcom.spider</groupId><artifactId>spider-jackson</artifactId><version>0.1.9</version></dependency>
```

## 2. 定义接口

```java
@SpiderClient(name = "user-service", url = "http://localhost:8081")
public interface UserClient {

    @SpiderGet("/users/{id}")
    @Timeout(800)
    @Retry(maxAttempts = 3)
    UserDTO getUser(@Path("id") Long id);

    @SpiderPost("/users")
    UserDTO createUser(@Body CreateUserRequest request);
}
```

## 3. 创建代理并调用

```java
SpiderClientFactory factory = SpiderClientFactory.builder()
        .transport(new OkHttpSpiderTransport())
        .decoder(new JacksonSpiderDecoder())
        .encoder(new JacksonSpiderEncoder())
        .build();

UserClient client = factory.create(UserClient.class);
UserDTO user = client.getUser(1L);
```

## 4. Spring Boot 集成

```java
@SpringBootApplication
@EnableSpiderClients
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}

@RestController
public class UserController {
    private final UserClient client;
    public UserController(UserClient c) { this.client = c; }
    @GetMapping("/users/{id}")
    public UserDTO get(@PathVariable Long id) { return client.getUser(id); }
}
```

## 5. 配置治理策略

```java
@SpiderClient(name = "pay-service", url = "http://...",
              fallback = PayFallback.class)
@SpiderCircuitBreaker(failureRateThreshold = 50)
public interface PayClient {

    @SpiderPost("/pay")
    @Retry(maxAttempts = 3, backoffStrategy = EXPONENTIAL)
    @RateLimit(permits = 200)
    @Timeout(1500)
    PayResult pay(@Body PayRequest req);
}
```

## 6. 查看监控

访问 `http://localhost:8086/spider` 查看 Dashboard。

## 下一步

- [核心概念](concepts.md)
- [配置参考](configuration.md)
- [Spring Boot 集成](spring-boot.md)
- [SPI 扩展指南](spi.md)
