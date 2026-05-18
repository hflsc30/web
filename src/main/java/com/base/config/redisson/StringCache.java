package com.base.config.redisson;

import com.base.util.RedissonUtil;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.Cache;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * Spring Cache 的 RBucket（String 类型）适配器
 */
@Slf4j
public class StringCache implements Cache {

    private final String cacheName;
    private final CacheConfig config;
    private final boolean allowNullValues;

    public StringCache(String cacheName, CacheConfig config, boolean allowNullValues) {
        this.cacheName = cacheName;
        this.config = config;
        this.allowNullValues = allowNullValues;
    }

    private String buildKey(Object key) {
        return cacheName + ":entry:" + key;
    }

    @Override
    @NonNull
    public String getName() {
        return cacheName;
    }

    @Override
    @NonNull
    public Object getNativeCache() {
        return RedissonUtil.getRawClient();
    }

    @Override
    @Nullable
    public ValueWrapper get(@NonNull Object key) {
        Object value = RedissonUtil.getRawClient().getBucket(buildKey(key)).get();
        return value == null ? null : () -> value;
    }

    @Override
    @Nullable
    public <T> T get(@NonNull Object key, @Nullable Class<T> type) {
        Object value = RedissonUtil.getRawClient().getBucket(buildKey(key)).get();
        if (value != null && type != null && !type.isInstance(value)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        T result = (T) value;
        return result;
    }

    @Override
    @Nullable
    public <T> T get(@NonNull Object key, @NonNull Callable<T> valueLoader) {
        Object value = RedissonUtil.getRawClient().getBucket(buildKey(key)).get();
        if (value != null) {
            @SuppressWarnings("unchecked")
            T result = (T) value;
            return result;
        }
        try {
            T loaded = valueLoader.call();
            put(key, loaded);
            return loaded;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(@NonNull Object key, @Nullable Object value) {
        if (!allowNullValues && value == null) {
            return;
        }
        String k = buildKey(key);
        if (config != null && config.getTtl() > 0) {
            RedissonUtil.setCacheObject(k, value, Duration.ofMillis(config.getTtl()));
        } else {
            RedissonUtil.setCacheObject(k, value);
        }
    }

    @Override
    public void evict(@NonNull Object key) {
        RedissonUtil.getRawClient().getBucket(buildKey(key)).delete();
    }

    @Override
    public void clear() {
        RedissonUtil.deleteKeys(cacheName + ":entry:*");
    }
}
