package com.base.config.redisson;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 缓存配置参数
 */
@Data
@NoArgsConstructor
public class CacheConfig {
    /** TTL（毫秒），0 表示永不过期 */
    private long ttl;
    /** 最大空闲时间（毫秒），0 表示不限制 */
    private long maxIdleTime;
    /** 最大容量，0 表示不限制 */
    private int maxSize;
}
