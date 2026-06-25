package io.github.spider.config;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * 动态配置中心的 SPI 接口（支持 Nacos、Apollo、Consul KV 等配置源）。
 *
 * <pre>{@code
 * SpiderConfigCenter center = new NacosConfigCenter("localhost:8848");
 * center.addListener(changed -> {
 *     if (changed.containsKey("spider.default-timeout")) {
 *         // 重新配置 SpiderClientFactory
 *     }
 * });
 * }</pre>
 */
public interface SpiderConfigCenter {

    /** 根据 key 获取配置值。 */
    String get(String key, String defaultValue);

    /** 获取 int 类型的配置值。 */
    int getInt(String key, int defaultValue);

    /** 获取 long 类型的配置值。 */
    long getLong(String key, long defaultValue);

    /** 注册配置变更监听器，当被监听的 key 发生变化时触发。 */
    void addListener(ConfigChangeListener listener);

    /** 监听指定的 key 的变化。 */
    void watch(String... keys);

    /** 配置变更事件的函数式接口。 */
    @FunctionalInterface
    interface ConfigChangeListener {
        void onChange(Map<String, String> changed);
    }

    /** 空实现（无操作），用于默认占位。 */
    SpiderConfigCenter NOOP = new SpiderConfigCenter() {
        @Override public String get(String key, String defaultValue) { return defaultValue; }
        @Override public int getInt(String key, int defaultValue) { return defaultValue; }
        @Override public long getLong(String key, long defaultValue) { return defaultValue; }
        @Override public void addListener(ConfigChangeListener listener) {}
        @Override public void watch(String... keys) {}
    };
}
