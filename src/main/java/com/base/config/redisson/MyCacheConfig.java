package com.base.config.redisson;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caffeine + Redis 二级缓存配置属性
 *
 * @author base
 */
@ConfigurationProperties(prefix = "cache")
@Data
public class MyCacheConfig {

    /**
     * Caffeine 一级缓存配置
     */
    private CaffeineSpec caffeine = new CaffeineSpec();

    /**
     * Redis 二级缓存配置
     */
    private RedisSpec redis = new RedisSpec();

    @Data
    public static class CaffeineSpec {
        private boolean enabled = true;
        private long maximumSize = 1000;
        private long expireAfterWriteSeconds = 300;
        private long expireAfterAccessSeconds = 120;
        private boolean recordStats = true;
        private Map<String, CacheSpec> specs = new ConcurrentHashMap<>();
    }

    @Data
    public static class RedisSpec {
        private long defaultTTL;
        private long defaultMaxIdleTime;
        private int defaultMaxSize;
        private boolean allowNullValues = true;
        private boolean transactionAware = true;
        private boolean globalL1CacheEnabled = true;
        private boolean dynamic = true;
    }

    @Data
    public static class CacheSpec {
        private long maximumSize = 1000;
        private long expireAfterWriteSeconds = 300;
        private long expireAfterAccessSeconds = 120;
        private boolean recordStats = true;
    }

    // -- 兼容性方法，供 MySpringCacheManager 和 RedissonConfig 调用 --

    public boolean isEnabled() {
        return caffeine.enabled;
    }

    public long getMaximumSize() {
        return caffeine.maximumSize;
    }

    public Map<String, CacheSpec> getSpecs() {
        return caffeine.specs;
    }

    /**
     * 根据缓存名称创建 Caffeine 缓存实例
     */
    public Cache<Object, Object> createCaffeineCache(String cacheName) {
        CacheSpec spec = null;
        if (cacheName != null && !cacheName.isEmpty()) {
            spec = caffeine.specs.get(cacheName);
        }
        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        if (spec != null) {
            builder.maximumSize(spec.getMaximumSize())
                    .expireAfterWrite(Duration.ofSeconds(spec.getExpireAfterWriteSeconds()))
                    .expireAfterAccess(Duration.ofSeconds(spec.getExpireAfterAccessSeconds()));
            if (spec.isRecordStats()) {
                builder.recordStats();
            }
        } else {
            builder.maximumSize(caffeine.maximumSize)
                    .expireAfterWrite(Duration.ofSeconds(caffeine.expireAfterWriteSeconds))
                    .expireAfterAccess(Duration.ofSeconds(caffeine.expireAfterAccessSeconds));
            if (caffeine.recordStats) {
                builder.recordStats();
            }
        }
        return builder.build();
    }
}
