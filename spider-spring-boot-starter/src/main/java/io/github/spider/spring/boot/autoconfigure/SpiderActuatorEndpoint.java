package io.github.spider.spring.boot.autoconfigure;

import io.github.spider.core.runtime.SpiderRuntime;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Endpoint(id = "spider")
@ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
public class SpiderActuatorEndpoint {

    @ReadOperation
    public Map<String, Object> status() {
        return SpiderRuntime.getInstance().summary();
    }
}
