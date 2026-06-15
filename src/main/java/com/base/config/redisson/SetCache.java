package com.base.config.redisson;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.redisson.api.RSet;
import org.springframework.cache.Cache;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * Spring Cache 的 RSet 适配器
  * @author base
 * @since 2026-06-11
 */
@Slf4j
public class SetCache implements Cache {

    private final RSet<Object> set;
    private final boolean allowNullValues;

    public SetCache(RSet<Object> set, CacheConfig config, boolean allowNullValues) {
        this.set = set;
        this.allowNullValues = allowNullValues;
    }

    @Override
    @NonNull
    public String getName() {
        return set.getName();
    }

    @Override
    @NonNull
    public Object getNativeCache() {
        return set;
    }

    @Override
    @Nullable
    public ValueWrapper get(@NonNull Object key) {
        if (set.contains(key)) {
            return () -> key;
        }
        return null;
    }

    @Override
    @Nullable
    public <T> T get(@NonNull Object key, @Nullable Class<T> type) {
        if (set.contains(key)) {
            if (type != null && !type.isInstance(key)) {
                return null;
            }
            @SuppressWarnings("unchecked")
            T result = (T) key;
            return result;
        }
        return null;
    }

    @Override
    @Nullable
    public <T> T get(@NonNull Object key, @NonNull Callable<T> valueLoader) {
        if (set.contains(key)) {
            @SuppressWarnings("unchecked")
            T result = (T) key;
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
        if (value != null) {
            set.add(value);
        } else {
            set.add(key);
        }
    }

    @Override
    public void evict(@NonNull Object key) {
        set.remove(key);
    }

    @Override
    public void clear() {
        set.clear();
    }

    public Collection<Object> readAll() {
        return set.readAll();
    }
}
