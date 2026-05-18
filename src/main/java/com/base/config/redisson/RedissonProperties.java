package com.base.config.redisson;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.redisson.config.ReadMode;
import org.redisson.config.SubscriptionMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;


@Data
@ConfigurationProperties(prefix = "redisson")
public class RedissonProperties {
    // redis 缓存 key 前缀
    private String keyPrefix;
    // 线程池数量, 默认值 = 当前处理核数量 * 2
    private Integer threads;
    // Netty 线程池数量, 默认值 = 当前处理核数量 * 2
    private Integer nettyThreads;
    // FastJson2 反序列化白名单包名
    private List<String> autotypePackages;
    // 单机服务配置
    private SingleServerConfig singleServerConfig;
    // 集群服务配置
    private ClusterServersConfig clusterServersConfig;

    @Data
    @NoArgsConstructor
    public static class SingleServerConfig {
        // 客户端名称
        private String clientName;
        // 最小空闲连接数
        private Integer connectionMinimumIdleSize;
        // 连接池大小
        private Integer connectionPoolSize;
        // 连接空闲超时，单位：毫秒
        private Integer idleConnectionTimeout;
        // 命令等待超时，单位：毫秒
        private Integer timeout;
        // 发布和订阅连接池大小
        private Integer subscriptionConnectionPoolSize;
    }

    @Data
    @NoArgsConstructor
    public static class ClusterServersConfig {
        // 客户端名称
        private String clientName;
        // master 最小空闲连接数
        private Integer masterConnectionMinimumIdleSize;
        // master 连接池大小
        private Integer masterConnectionPoolSize;
        // slave 最小空闲连接数
        private Integer slaveConnectionMinimumIdleSize;
        // slave 连接池大小
        private Integer slaveConnectionPoolSize;
        // 连接空闲超时，单位：毫秒
        private Integer idleConnectionTimeout;
        // 命令等待超时，单位：毫秒
        private Integer timeout;
        // 发布和订阅连接池大小
        private Integer subscriptionConnectionPoolSize;
        // 读取模式
        private ReadMode readMode;
        // 订阅模式
        private SubscriptionMode subscriptionMode;
    }

}
