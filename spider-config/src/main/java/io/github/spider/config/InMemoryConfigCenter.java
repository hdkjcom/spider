package io.github.spider.config;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory config center — useful for testing and programmatic updates.
 */
public class InMemoryConfigCenter implements SpiderConfigCenter {

    private final Map<String, String> config = new ConcurrentHashMap<>();
    private final List<ConfigChangeListener> listeners = new CopyOnWriteArrayList<>();

    public InMemoryConfigCenter put(String key, String value) {
        Map<String, String> change = new HashMap<>();
        change.put(key, value);
        config.put(key, value);
        for (ConfigChangeListener l : listeners) l.onChange(change);
        return this;
    }

    @Override public String get(String key, String defaultValue) { return config.getOrDefault(key, defaultValue); }
    @Override public int getInt(String key, int defaultValue) { return Integer.parseInt(get(key, String.valueOf(defaultValue))); }
    @Override public long getLong(String key, long defaultValue) { return Long.parseLong(get(key, String.valueOf(defaultValue))); }
    @Override public void addListener(ConfigChangeListener listener) { listeners.add(listener); }
    @Override public void watch(String... keys) { /* in-memory always watches all */ }
}
