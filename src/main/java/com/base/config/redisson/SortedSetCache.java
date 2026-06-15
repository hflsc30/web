package com.base.config.redisson;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.redisson.api.RScoredSortedSet;
import org.springframework.cache.Cache;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * Spring Cache 的 RScoredSortedSet 适配器
  * @author base
 * @since 2026-06-11
 */
@Slf4j
public class SortedSetCache implements Cache {

    private final RScoredSortedSet<Object> sortedSet;
    private final boolean allowNullValues;

    public SortedSetCache(RScoredSortedSet<Object> sortedSet, CacheConfig config, boolean allowNullValues) {
        this.sortedSet = sortedSet;
        this.allowNullValues = allowNullValues;
    }

    @Override
    @NonNull
    public String getName() {
        return sortedSet.getName();
    }

    @Override
    @NonNull
    public Object getNativeCache() {
        return sortedSet;
    }

    @Override
    @Nullable
    public ValueWrapper get(@NonNull Object key) {
        Double score = sortedSet.getScore(key);
        if (score != null) {
            return () -> key;
        }
        return null;
    }

    @Override
    @Nullable
    public <T> T get(@NonNull Object key, @Nullable Class<T> type) {
        if (sortedSet.getScore(key) != null) {
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
        if (sortedSet.getScore(key) != null) {
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
        double score = key instanceof Number n ? n.doubleValue() : 1.0;
        sortedSet.add(score, value != null ? value : key);
    }

    @Override
    public void evict(@NonNull Object key) {
        sortedSet.remove(key);
    }

    @Override
    public void clear() {
        sortedSet.clear();
    }

    public Collection<Object> readAll() {
        return sortedSet.readAll();
    }
}
