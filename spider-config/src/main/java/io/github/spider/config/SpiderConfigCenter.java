package io.github.spider.config;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * SPI for dynamic configuration sources (Nacos, Apollo, Consul KV, etc.).
 *
 * <pre>{@code
 * SpiderConfigCenter center = new NacosConfigCenter("localhost:8848");
 * center.addListener(changed -> {
 *     if (changed.containsKey("spider.default-timeout")) {
 *         // reconfigure SpiderClientFactory
 *     }
 * });
 * }</pre>
 */
public interface SpiderConfigCenter {

    /** Get a config value by key. */
    String get(String key, String defaultValue);

    /** Get a config value as int. */
    int getInt(String key, int defaultValue);

    /** Get a config value as long. */
    long getLong(String key, long defaultValue);

    /** Register a listener that fires when any watched key changes. */
    void addListener(ConfigChangeListener listener);

    /** Watch a specific key for changes. */
    void watch(String... keys);

    /** Functional interface for config change events. */
    @FunctionalInterface
    interface ConfigChangeListener {
        void onChange(Map<String, String> changed);
    }

    /** No-op implementation. */
    SpiderConfigCenter NOOP = new SpiderConfigCenter() {
        @Override public String get(String key, String defaultValue) { return defaultValue; }
        @Override public int getInt(String key, int defaultValue) { return defaultValue; }
        @Override public long getLong(String key, long defaultValue) { return defaultValue; }
        @Override public void addListener(ConfigChangeListener listener) {}
        @Override public void watch(String... keys) {}
    };
}
