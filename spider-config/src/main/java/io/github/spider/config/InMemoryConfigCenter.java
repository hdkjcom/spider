package io.github.spider.config;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 基于内存的配置中心实现，适用于测试和编程式更新。
 */
public class InMemoryConfigCenter implements SpiderConfigCenter {

    private final Map<String, String> config = new ConcurrentHashMap<>();
    private final List<ConfigChangeListener> listeners = new CopyOnWriteArrayList<>();

    /** 存入配置项，并通知所有监听器该 key 已发生变更。 */
    public InMemoryConfigCenter put(String key, String value) {
        Map<String, String> change = new HashMap<>();
        change.put(key, value);
        config.put(key, value);
        for (ConfigChangeListener l : listeners) l.onChange(change);
        return this;
    }

    /** 根据 key 获取配置值，不存在时返回默认值。 */
    @Override public String get(String key, String defaultValue) { return config.getOrDefault(key, defaultValue); }
    /** 获取 int 类型的配置值。 */
    @Override public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    /** 获取 long 类型的配置值。 */
    @Override public long getLong(String key, long defaultValue) {
        try {
            return Long.parseLong(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    @Override public void addListener(ConfigChangeListener listener) { listeners.add(listener); }
    @Override public void watch(String... keys) { /* 内存模式始终监听所有 key */ }
}
