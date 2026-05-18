package com.base.config.redisson;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Caffeine L1 缓存装饰器，包装 Spring Cache 实现两级缓存
 */
@Slf4j
public class CaffeineCacheDecorator implements org.springframework.cache.Cache {

    private final String name;
    private final org.springframework.cache.Cache delegate;
    private final Cache<Object, Object> localCache;
    private final boolean l1Enabled;

    public CaffeineCacheDecorator(String name, org.springframework.cache.Cache delegate,
                                  Cache<Object, Object> localCache, boolean l1Enabled) {
        this.name = name;
        this.delegate = delegate;
        this.localCache = localCache;
        this.l1Enabled = l1Enabled;
    }

    @Override
    @NonNull
    public String getName() {
        return name;
    }

    @Override
    @NonNull
    public Object getNativeCache() {
        return delegate.getNativeCache();
    }

    @Override
    @Nullable
    public ValueWrapper get(@NonNull Object key) {
        if (l1Enabled) {
            Object local = localCache.getIfPresent(key);
            if (local != null) {
                return () -> local;
            }
        }
        ValueWrapper wrapper = delegate.get(key);
        if (wrapper != null && l1Enabled) {
            localCache.put(key, Objects.requireNonNull(wrapper.get()));
        }
        return wrapper;
    }

    @Override
    @Nullable
    public <T> T get(@NonNull Object key, @Nullable Class<T> type) {
        if (l1Enabled) {
            Object local = localCache.getIfPresent(key);
            if (local != null && (type == null || type.isInstance(local))) {
                @SuppressWarnings("unchecked")
                T result = (T) local;
                return result;
            }
        }
        T value = delegate.get(key, type);
        if (value != null && l1Enabled) {
            localCache.put(key, value);
        }
        return value;
    }

    @Override
    @Nullable
    public <T> T get(@NonNull Object key, @NonNull Callable<T> valueLoader) {
        if (l1Enabled) {
            Object local = localCache.getIfPresent(key);
            if (local != null) {
                @SuppressWarnings("unchecked")
                T result = (T) local;
                return result;
            }
        }
        T value = delegate.get(key, valueLoader);
        if (value != null && l1Enabled) {
            localCache.put(key, value);
        }
        return value;
    }

    @Override
    public void put(@NonNull Object key, @Nullable Object value) {
        delegate.put(key, value);
        if (l1Enabled && value != null) {
            localCache.put(key, value);
        }
    }

    @Override
    public void evict(@NonNull Object key) {
        if (l1Enabled) {
            localCache.invalidate(key);
        }
        delegate.evict(key);
    }

    @Override
    public void clear() {
        if (l1Enabled) {
            localCache.invalidateAll();
        }
        delegate.clear();
    }

    // -- L1 cache management methods --

    public void clearL1Cache() {
        localCache.invalidateAll();
    }

    public boolean isL1CacheEnabled() {
        return l1Enabled;
    }

    public long getL1CacheSize() {
        return localCache.estimatedSize();
    }

    public String getL1CacheStats() {
        CacheStats stats = localCache.stats();
        return String.format("hits=%d, misses=%d, hitRate=%.2f%%, size=%d",
                stats.hitCount(), stats.missCount(),
                stats.hitRate() * 100, localCache.estimatedSize());
    }
}
