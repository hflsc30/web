package com.base.config.redisson;

import com.base.util.RedissonUtil;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.redisson.api.RList;
import org.springframework.cache.Cache;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Spring Cache 的 RList 适配器
 */
@Slf4j
public class ListCache implements Cache {

    private final RList<Object> list;
    private final CacheConfig config;
    private final boolean allowNullValues;

    public ListCache(RList<Object> list, CacheConfig config, boolean allowNullValues) {
        this.list = list;
        this.config = config;
        this.allowNullValues = allowNullValues;
    }

    @Override
    @NonNull
    public String getName() {
        return list.getName();
    }

    @Override
    @NonNull
    public Object getNativeCache() {
        return list;
    }

    @Override
    @Nullable
    public ValueWrapper get(@NonNull Object key) {
        if (key instanceof Integer index) {
            if (index < 0 || index >= list.size()) {
                return null;
            }
            Object value = list.get(index);
            return value == null ? null : () -> value;
        }
        return null;
    }

    @Override
    @Nullable
    public <T> T get(@NonNull Object key, @Nullable Class<T> type) {
        ValueWrapper wrapper = get(key);
        if (wrapper == null) {
            return null;
        }
        Object value = wrapper.get();
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
        ValueWrapper wrapper = get(key);
        if (wrapper != null) {
            @SuppressWarnings("unchecked")
            T result = (T) wrapper.get();
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
        if (key instanceof Integer index) {
            if (index >= 0 && index < list.size()) {
                list.set(index, value);
            } else {
                list.add(value);
            }
        } else {
            list.add(value);
        }
    }

    @Override
    public void evict(@NonNull Object key) {
        if (key instanceof Integer index && index >= 0 && index < list.size()) {
            list.remove((int) index);
        } else {
            list.remove(key);
        }
    }

    @Override
    public void clear() {
        list.clear();
    }

    public List<Object> readAll() {
        return list.readAll();
    }
}
