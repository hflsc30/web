package com.base.config.redisson;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.redisson.api.RMap;
import org.redisson.api.RMapCache;
import org.springframework.cache.Cache;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Spring Cache 的 Redisson RMap/RMapCache 适配器
 */
@Slf4j
public class RedissonCache implements Cache {

    private final RMap<Object, Object> map;
    private final CacheConfig config;
    private final boolean allowNullValues;
    private final String name;

    public RedissonCache(RMap<Object, Object> map, boolean allowNullValues) {
        this(map, null, allowNullValues);
    }

    public RedissonCache(RMap<Object, Object> map, CacheConfig config, boolean allowNullValues) {
        this.map = map;
        this.config = config;
        this.allowNullValues = allowNullValues;
        this.name = map.getName();
    }

    @Override
    @NonNull
    public String getName() {
        return name;
    }

    @Override
    @NonNull
    public Object getNativeCache() {
        return map;
    }

    @Override
    @Nullable
    public ValueWrapper get(@NonNull Object key) {
        Object value = map.get(key);
        return toValueWrapper(value);
    }

    @Override
    @Nullable
    public <T> T get(@NonNull Object key, @Nullable Class<T> type) {
        Object value = map.get(key);
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
        Object value = map.get(key);
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
        if (config != null && map instanceof RMapCache) {
            RMapCache<Object, Object> mapCache = (RMapCache<Object, Object>) map;
            if (config.getTtl() > 0) {
                mapCache.put(key, value, config.getTtl(), TimeUnit.MILLISECONDS,
                        config.getMaxIdleTime(), TimeUnit.MILLISECONDS);
                return;
            }
        }
        map.put(key, value);
    }

    @Override
    public void evict(@NonNull Object key) {
        map.remove(key);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Nullable
    private ValueWrapper toValueWrapper(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        return () -> value;
    }
}
