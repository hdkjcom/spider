package io.github.spider.spring.boot.autoconfigure;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Spider OkHttp 连接池状态端点，通过 {@code /actuator/spider-pool} 暴露连接池健康度。
 *
 * <p>端点：
 * <ul>
 *   <li>{@code GET /actuator/spider-pool} — OkHttp 连接池状态</li>
 * </ul>
 *
 * <p>仅在 classpath 中同时包含 Spring Boot Actuator 与 OkHttp，且容器中存在
 * {@link OkHttpClient} Bean 时激活。用户若自行排除了 OkHttp 传输，本端点不会注册，
 * 避免依赖缺失导致启动失败。</p>
 *
 * <p>不同 OkHttp 版本的 {@link ConnectionPool} 公共 API 不一致：4.9.x 仅暴露
 * {@code idleConnectionCount()} 与 {@code connectionCount()}，而 4.12+ 才额外提供
 * {@code maxIdleConnections()}、{@code keepAliveDuration(TimeUnit)} 等方法。本端点
 * 优先调用公开 API，对缺失的方法退化为反射读取，保证跨版本可用。</p>
 */
@Component
@Endpoint(id = "spider-pool")
@ConditionalOnClass({OkHttpClient.class, Endpoint.class})
@ConditionalOnBean(OkHttpClient.class)
public class SpiderConnectionPoolEndpoint {

    private final ObjectProvider<OkHttpClient> clientProvider;

    /**
     * 通过 {@link ObjectProvider} 注入 OkHttpClient，避免在 Bean 尚未创建或被排除时
     * 触发提前实例化，从而保证 {@code @ConditionalOnBean} 的语义生效。
     *
     * @param clientProvider OkHttpClient 的 ObjectProvider
     */
    public SpiderConnectionPoolEndpoint(ObjectProvider<OkHttpClient> clientProvider) {
        this.clientProvider = clientProvider;
    }

    /**
     * 返回 OkHttp 连接池状态信息。
     *
     * <p>字段：
     * <ul>
     *   <li>{@code idleConnections} — 当前空闲连接数（{@link ConnectionPool#idleConnectionCount()}）</li>
     *   <li>{@code totalConnections} — 当前总连接数（{@link ConnectionPool#connectionCount()}）</li>
     *   <li>{@code allocatedConnections} — 已分配（活跃）连接数，由 total - idle 推导</li>
     *   <li>{@code maxIdleConnections} — 允许的最大空闲连接数（OkHttp 4.12+ 直接暴露，旧版反射读取，缺失为 null）</li>
     *   <li>{@code keepAliveDurationNanos} — 空闲连接保活时长（纳秒，缺失为 null）</li>
     *   <li>{@code keepAliveDurationMillis} — 空闲连接保活时长（毫秒，便于阅读）</li>
     * </ul>
     *
     * @return 连接池状态 Map
     */
    @ReadOperation
    public Map<String, Object> connectionPool() {
        Map<String, Object> info = new LinkedHashMap<>();
        OkHttpClient client = clientProvider.getIfAvailable();
        if (client == null) {
            info.put("error", "OkHttpClient bean not available");
            return info;
        }
        ConnectionPool pool = client.connectionPool();
        int idle = pool.idleConnectionCount();
        int total = pool.connectionCount();
        info.put("idleConnections", idle);
        info.put("totalConnections", total);
        info.put("allocatedConnections", total - idle);

        Integer maxIdle = invokeInt(pool, "maxIdleConnections");
        if (maxIdle != null) {
            info.put("maxIdleConnections", maxIdle);
        }
        Long keepAliveNanos = invokeLong(pool, "keepAliveDuration", TimeUnit.NANOSECONDS);
        if (keepAliveNanos != null) {
            info.put("keepAliveDurationNanos", keepAliveNanos);
            info.put("keepAliveDurationMillis", TimeUnit.NANOSECONDS.toMillis(keepAliveNanos));
        }
        return info;
    }

    /**
     * 反射调用无参 int 方法，兼容不同 OkHttp 版本中可能缺失的方法。
     *
     * @param pool       连接池实例
     * @param methodName 方法名
     * @return 调用结果；方法不存在或调用失败时返回 null
     */
    private static Integer invokeInt(ConnectionPool pool, String methodName) {
        try {
            Method m = pool.getClass().getMethod(methodName);
            Object result = m.invoke(pool);
            if (result instanceof Number) {
                return ((Number) result).intValue();
            }
        } catch (NoSuchMethodException ignored) {
            // 该 OkHttp 版本未暴露此方法
        } catch (Exception e) {
            // 反射调用失败时仅跳过该字段，不影响端点其他信息
        }
        return null;
    }

    /**
     * 反射调用带一个 TimeUnit 参数的 long 方法，兼容不同 OkHttp 版本中可能缺失的方法。
     *
     * @param pool       连接池实例
     * @param methodName 方法名
     * @param unit       时间单位参数
     * @return 调用结果；方法不存在或调用失败时返回 null
     */
    private static Long invokeLong(ConnectionPool pool, String methodName, TimeUnit unit) {
        try {
            Method m = pool.getClass().getMethod(methodName, TimeUnit.class);
            Object result = m.invoke(pool, unit);
            if (result instanceof Number) {
                return ((Number) result).longValue();
            }
        } catch (NoSuchMethodException ignored) {
            // 该 OkHttp 版本未暴露此方法
        } catch (Exception e) {
            // 反射调用失败时仅跳过该字段，不影响端点其他信息
        }
        return null;
    }
}
