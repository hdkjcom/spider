package io.github.spider.demo;

import io.github.spider.core.annotation.*;
import io.github.spider.spring.boot.autoconfigure.EnableSpiderClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Spring Boot demo for Spider.
 * Demonstrates @EnableSpiderClients, auto-scanning, and dependency injection.
 *
 * Start a JSON API on localhost:8081 first, then run this application.
 */
@SpringBootApplication
@EnableSpiderClients(basePackages = "io.github.spider.demo")
public class SpiderSpringBootDemo {

    // ---- Client interface — auto-scanned and registered as a Spring bean ----

    @SpiderClient(name = "user-service", url = "http://localhost:8081")
    public interface UserClient {

        @SpiderGet("/users/{id}")
        @Timeout(800)
        @Retry(maxAttempts = 3, backoffMillis = 100)
        UserDTO getUser(@Path("id") Long id);
    }

    public static class UserDTO {
        public Long id;
        public String name;
        public int age;

        @Override
        public String toString() {
            return "UserDTO{id=" + id + ", name='" + name + "', age=" + age + '}';
        }
    }

    // ---- Business component that uses the auto-injected client ----

    @Component
    public static class UserService {

        private final UserClient userClient;

        public UserService(UserClient userClient) {
            this.userClient = userClient;
        }

        public void run() {
            System.out.println("===== Spider Spring Boot Demo =====");
            try {
                UserDTO user = userClient.getUser(1L);
                System.out.println("Result: " + user);
            } catch (Exception e) {
                System.err.println("Request failed: " + e.getMessage());
                System.err.println("Make sure a JSON API is running on http://localhost:8081");
            }
            System.out.println("===== Demo complete =====");
        }
    }

    // ---- Entry point ----

    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(SpiderSpringBootDemo.class, args);
        UserService service = ctx.getBean(UserService.class);
        service.run();
    }
}
