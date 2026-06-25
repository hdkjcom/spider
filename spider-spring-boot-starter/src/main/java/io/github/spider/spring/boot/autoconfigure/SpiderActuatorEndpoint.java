package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.core.runtime.SpiderRuntime;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Spider Actuator 端点，通过 {@code /actuator/spider} 暴露 Spider 运行时状态。
 *
 * <p>仅在 classpath 中包含 Spring Boot Actuator 时生效。</p>
 */
@Component
@Endpoint(id = "spider")
@ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
public class SpiderActuatorEndpoint {

    /**
     * 返回 Spider 运行时汇总信息，包含活跃客户端数量、已发送请求数等。
     *
     * @return Spider 运行时状态快照
     */
    @ReadOperation
    public Map<String, Object> status() {
        return SpiderRuntime.getInstance().summary();
    }
}
