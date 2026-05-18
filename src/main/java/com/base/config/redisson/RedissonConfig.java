package com.base.config.redisson;

import com.base.util.RedissonUtil;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

@Slf4j
@RequiredArgsConstructor
@AutoConfiguration
@EnableCaching
@ConditionalOnClass(RedissonClient.class)
@EnableConfigurationProperties({RedissonProperties.class, MyCacheConfig.class})
public class RedissonConfig {

    private final RedissonProperties redissonProperties;
    private final MyCacheConfig myCacheConfig;

    @Bean
    public RedissonAutoConfigurationCustomizer redissonCustomizer() {
        return config -> {
            if (redissonProperties.getThreads() != null) {
                config.setThreads(redissonProperties.getThreads());
            }
            if (redissonProperties.getNettyThreads() != null) {
                config.setNettyThreads(redissonProperties.getNettyThreads());
            }
            config.setCodec(new FastJson2Codec(redissonProperties.getAutotypePackages()));

            boolean isFindConfig = false;
            RedissonProperties.SingleServerConfig singleServerConfig = redissonProperties.getSingleServerConfig();
            RedissonProperties.ClusterServersConfig clusterServersConfig = redissonProperties.getClusterServersConfig();

            if (singleServerConfig != null) {
                isFindConfig = true;
                config.setNameMapper(new KeyPrefixHandler(redissonProperties.getKeyPrefix()));
                applySingleServerConfig(config, singleServerConfig);
            } else if (clusterServersConfig != null) {
                isFindConfig = true;
                config.setNameMapper(new KeyPrefixHandler(redissonProperties.getKeyPrefix()));
                applyClusterServersConfig(config, clusterServersConfig);
            }
            if (!isFindConfig) {
                throw new RuntimeException("找不到 Redisson 配置");
            }
            log.info("初始化 Redis");
        };
    }

    @Bean
    public Cache<Object, Object> defaultLocalCache() {
        return myCacheConfig.createCaffeineCache(null);
    }

    @Bean
    public RedissonDistributeLocker redissonLocker(RedissonClient redissonClient) {
        RedissonDistributeLocker locker = new RedissonDistributeLocker(redissonClient);
        RedissonUtil.setLocker(locker);
        return locker;
    }

    @Bean
    public CacheManager cacheManager() {
        MySpringCacheManager cacheManager = new MySpringCacheManager(myCacheConfig);
        cacheManager.setGlobalL1CacheEnabled(myCacheConfig.isEnabled());
        cacheManager.afterPropertiesSet();
        return cacheManager;
    }

    private void applySingleServerConfig(
            org.redisson.config.Config config,
            RedissonProperties.SingleServerConfig sc) {
        org.redisson.config.SingleServerConfig serverConfig = config.useSingleServer();
        if (sc.getTimeout() != null) {
            serverConfig.setTimeout(sc.getTimeout());
        }
        if (sc.getClientName() != null) {
            serverConfig.setClientName(sc.getClientName());
        }
        if (sc.getIdleConnectionTimeout() != null) {
            serverConfig.setIdleConnectionTimeout(sc.getIdleConnectionTimeout());
        }
        if (sc.getSubscriptionConnectionPoolSize() != null) {
            serverConfig.setSubscriptionConnectionPoolSize(sc.getSubscriptionConnectionPoolSize());
        }
        if (sc.getConnectionMinimumIdleSize() != null) {
            serverConfig.setConnectionMinimumIdleSize(sc.getConnectionMinimumIdleSize());
        }
        if (sc.getConnectionPoolSize() != null) {
            serverConfig.setConnectionPoolSize(sc.getConnectionPoolSize());
        }
    }

    private void applyClusterServersConfig(
            org.redisson.config.Config config,
            RedissonProperties.ClusterServersConfig cc) {
        org.redisson.config.ClusterServersConfig clusterConfig = config.useClusterServers();
        if (cc.getTimeout() != null) {
            clusterConfig.setTimeout(cc.getTimeout());
        }
        if (cc.getClientName() != null) {
            clusterConfig.setClientName(cc.getClientName());
        }
        if (cc.getIdleConnectionTimeout() != null) {
            clusterConfig.setIdleConnectionTimeout(cc.getIdleConnectionTimeout());
        }
        if (cc.getSubscriptionConnectionPoolSize() != null) {
            clusterConfig.setSubscriptionConnectionPoolSize(cc.getSubscriptionConnectionPoolSize());
        }
        if (cc.getMasterConnectionMinimumIdleSize() != null) {
            clusterConfig.setMasterConnectionMinimumIdleSize(cc.getMasterConnectionMinimumIdleSize());
        }
        if (cc.getMasterConnectionPoolSize() != null) {
            clusterConfig.setMasterConnectionPoolSize(cc.getMasterConnectionPoolSize());
        }
        if (cc.getSlaveConnectionMinimumIdleSize() != null) {
            clusterConfig.setSlaveConnectionMinimumIdleSize(cc.getSlaveConnectionMinimumIdleSize());
        }
        if (cc.getSlaveConnectionPoolSize() != null) {
            clusterConfig.setSlaveConnectionPoolSize(cc.getSlaveConnectionPoolSize());
        }
        if (cc.getReadMode() != null) {
            clusterConfig.setReadMode(cc.getReadMode());
        }
        if (cc.getSubscriptionMode() != null) {
            clusterConfig.setSubscriptionMode(cc.getSubscriptionMode());
        }
    }
}
