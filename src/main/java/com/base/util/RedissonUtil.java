package com.base.util;

import com.base.config.redisson.DistributeLocker;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.redisson.api.options.KeysScanOptions;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * redis 工具类
 *
 * @author base
 * @since 2026-05-15
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RedissonUtil {

    private static volatile RedissonClient client;
    private static volatile DistributeLocker locker;
    private static final String PREFIX_KEY = "redisson:";

    private static RedissonClient getClient() {
        if (client == null) {
            synchronized (RedissonUtil.class) {
                if (client == null) {
                    client = SpringUtil.getBean(RedissonClient.class);
                }
            }
        }
        return client;
    }

    // region 限流

    /**
     * 尝试获取许可，首次调用时自动配置限流规则
     *
     * @param key          限流key
     * @param rateType     限流类型
     * @param rate         速率
     * @param rateInterval 速率间隔（秒）
     * @return -1 表示失败
     */
    public static long rateLimiter(String key, RateType rateType, int rate, int rateInterval) {
        RRateLimiter rateLimiter = getClient().getRateLimiter(key);
        rateLimiter.trySetRate(rateType, rate, Duration.ofSeconds(rateInterval));
        if (rateLimiter.tryAcquire()) {
            return rateLimiter.availablePermits();
        }
        return -1L;
    }

    // endregion

    // region 客户端

    /**
     * 获取 RedissonClient 原生实例，用于调用工具类未封装的 API
     */
    public static RedissonClient getRawClient() {
        return getClient();
    }

    // endregion

    // region 发布/订阅

    /**
     * 发布消息到通道，并在发送后执行本地回调
     *
     * @param channelKey 通道key
     * @param msg        发送数据
     * @param consumer   发送后本地回调（非订阅方回调）
     */
    public static <T> void publish(String channelKey, T msg, Consumer<T> consumer) {
        RTopic topic = getClient().getTopic(channelKey);
        topic.publish(msg);
        consumer.accept(msg);
    }

    /**
     * 发布消息到通道
     */
    public static <T> void publish(String channelKey, T msg) {
        RTopic topic = getClient().getTopic(channelKey);
        topic.publish(msg);
    }

    /**
     * 订阅通道接收消息
     *
     * @param channelKey 通道key
     * @param clazz      消息类型
     * @param consumer   消息处理回调
     */
    public static <T> void subscribe(String channelKey, Class<T> clazz, Consumer<T> consumer) {
        RTopic topic = getClient().getTopic(channelKey);
        topic.addListener(clazz, (channel, msg) -> consumer.accept(msg));
    }

    // endregion

    // region 基本对象缓存

    /**
     * 缓存基本的对象
     */
    public static <T> void setCacheObject(final String key, final T value) {
        setCacheObject(key, value, false);
    }

    /**
     * 缓存基本的对象，可选保留当前 TTL
     *
     * @param isSaveTtl 是否保留 TTL 有效期
     */
    public static <T> void setCacheObject(final String key, final T value, final boolean isSaveTtl) {
        RBucket<T> bucket = getClient().getBucket(key);
        if (isSaveTtl) {
            try {
                bucket.setAndKeepTTL(value);
            } catch (Exception e) {
                long timeToLive = bucket.remainTimeToLive();
                setCacheObject(key, value, Duration.ofMillis(timeToLive));
            }
        } else {
            bucket.set(value);
        }
    }

    /**
     * 缓存基本的对象并设置有效期
     */
    public static <T> void setCacheObject(final String key, final T value, final Duration duration) {
        RBatch batch = getClient().createBatch();
        RBucketAsync<T> bucket = batch.getBucket(key);
        bucket.setAsync(value);
        bucket.expireAsync(duration);
        batch.execute();
    }

    /**
     * 如果 key 不存在则设置，返回 true；已存在则返回 false
     */
    public static <T> boolean setObjectIfAbsent(final String key, final T value, final Duration duration) {
        RBucket<T> bucket = getClient().getBucket(key);
        return bucket.setIfAbsent(value, duration);
    }

    /**
     * 注册对象监听器
     * <p>
     * 需开启 Redis {@code notify-keyspace-events} 配置
     */
    public static <T> void addObjectListener(final String key, final ObjectListener listener) {
        RBucket<T> result = getClient().getBucket(key);
        result.addListener(listener);
    }

    /**
     * 设置有效时间
     *
     * @param timeout 超时时间（秒）
     */
    public static boolean expire(final String key, final long timeout) {
        return expire(key, Duration.ofSeconds(timeout));
    }

    /**
     * 设置有效时间
     */
    public static boolean expire(final String key, final Duration duration) {
        RBucket<Object> rBucket = getClient().getBucket(key);
        return rBucket.expire(duration);
    }

    /**
     * 获得缓存的基本对象
     */
    public static <T> T getCacheObject(final String key) {
        RBucket<T> rBucket = getClient().getBucket(key);
        return rBucket.get();
    }

    /**
     * 获得 key 剩余存活时间（毫秒），-1 表示永不过期，-2 表示 key 不存在
     */
    public static long getTimeToLive(final String key) {
        RBucket<Object> rBucket = getClient().getBucket(key);
        return rBucket.remainTimeToLive();
    }

    /**
     * 删除单个 key
     */
    public static long deleteObject(final String key) {
        return getClient().getBucket(key).delete() ? 1 : 0;
    }

    /**
     * 批量删除
     */
    public static void deleteObject(final Collection<?> collection) {
        RBatch batch = getClient().createBatch();
        collection.forEach(t -> batch.getBucket(t.toString()).deleteAsync());
        batch.execute();
    }

    /**
     * 检查 key 是否存在
     */
    public static boolean isExistsObject(final String key) {
        return getClient().getBucket(key).isExists();
    }

    // endregion

    // region List 缓存

    /**
     * 缓存 List（覆盖写入）
     */
    public static <T> void setCacheList(final String key, final List<T> dataList) {
        RBatch batch = getClient().createBatch();
        RListAsync<T> rList = batch.getList(key);
        rList.deleteAsync();
        rList.addAllAsync(dataList);
        batch.execute();
    }

    /**
     * 缓存 List 并设置有效期
     */
    public static <T> void setCacheList(final String key, final List<T> dataList, final Duration duration) {
        RBatch batch = getClient().createBatch();
        RListAsync<T> rList = batch.getList(key);
        rList.deleteAsync();
        rList.addAllAsync(dataList);
        rList.expireAsync(duration);
        batch.execute();
    }

    /**
     * 注册 List 监听器
     * <p>
     * 需开启 Redis {@code notify-keyspace-events} 配置
     */
    public static <T> void addListListener(final String key, final ObjectListener listener) {
        RList<T> rList = getClient().getList(key);
        rList.addListener(listener);
    }

    /**
     * 获得缓存的 list
     */
    public static <T> List<T> getCacheList(final String key) {
        RList<T> rList = getClient().getList(key);
        return rList.readAll();
    }

    // endregion

    // region Set 缓存

    /**
     * 缓存 Set（覆盖写入）
     */
    public static <T> void setCacheSet(final String key, final Set<T> dataSet) {
        RSet<T> rSet = getClient().getSet(key);
        rSet.delete();
        rSet.addAll(dataSet);
    }

    /**
     * 注册 Set 监听器
     * <p>
     * 需开启 Redis {@code notify-keyspace-events} 配置
     */
    public static <T> void addSetListener(final String key, final ObjectListener listener) {
        RSet<T> rSet = getClient().getSet(key);
        rSet.addListener(listener);
    }

    /**
     * 获得缓存的 set
     */
    public static <T> Set<T> getCacheSet(final String key) {
        RSet<T> rSet = getClient().getSet(key);
        return rSet.readAll();
    }

    // endregion

    // region Map 缓存

    /**
     * 缓存 Map（覆盖写入）
     */
    public static <T> void setCacheMap(final String key, final Map<String, T> dataMap) {
        if (dataMap != null) {
            RMap<String, T> rMap = getClient().getMap(key);
            rMap.clear();
            rMap.putAll(dataMap);
        }
    }

    /**
     * 注册 Map 监听器
     * <p>
     * 需开启 Redis {@code notify-keyspace-events} 配置
     */
    public static <T> void addMapListener(final String key, final ObjectListener listener) {
        RMap<String, T> rMap = getClient().getMap(key);
        rMap.addListener(listener);
    }

    /**
     * 获得缓存的 Map
     */
    public static <T> Map<String, T> getCacheMap(final String key) {
        RMap<String, T> rMap = getClient().getMap(key);
        return rMap.readAllMap();
    }

    /**
     * 获得缓存 Map 的 key 列表
     */
    public static <T> Set<String> getCacheMapKeySet(final String key) {
        RMap<String, T> rMap = getClient().getMap(key);
        return rMap.keySet();
    }

    /**
     * 往 Hash 中存入数据
     */
    public static <T> void setCacheMapValue(final String key, final String hKey, final T value) {
        RMap<String, T> rMap = getClient().getMap(key);
        rMap.put(hKey, value);
    }

    /**
     * 获取 Hash 中的数据
     */
    public static <T> T getCacheMapValue(final String key, final String hKey) {
        RMap<String, T> rMap = getClient().getMap(key);
        return rMap.get(hKey);
    }

    /**
     * 删除 Hash 中的数据
     */
    public static <T> T delCacheMapValue(final String key, final String hKey) {
        RMap<String, T> rMap = getClient().getMap(key);
        return rMap.remove(hKey);
    }

    /**
     * 批量删除 Hash 中的数据
     */
    public static <T> void delMultiCacheMapValue(final String key, final Set<String> hKeys) {
        RBatch batch = getClient().createBatch();
        RMapAsync<String, T> rMap = batch.getMap(key);
        for (String hKey : hKeys) {
            rMap.removeAsync(hKey);
        }
        batch.execute();
    }

    /**
     * 批量获取 Hash 中的数据
     */
    public static <K, V> Map<K, V> getMultiCacheMapValue(final String key, final Set<K> hKeys) {
        RMap<K, V> rMap = getClient().getMap(key);
        return rMap.getAll(hKeys);
    }

    // endregion

    // region 原子值

    public static void setAtomicValue(String key, long value) {
        RAtomicLong atomic = getClient().getAtomicLong(key);
        atomic.set(value);
    }

    public static long getAtomicValue(String key) {
        RAtomicLong atomic = getClient().getAtomicLong(key);
        return atomic.get();
    }

    public static long incrAtomicValue(String key) {
        RAtomicLong atomic = getClient().getAtomicLong(key);
        return atomic.incrementAndGet();
    }

    public static long decrAtomicValue(String key) {
        RAtomicLong atomic = getClient().getAtomicLong(key);
        return atomic.decrementAndGet();
    }

    // endregion

    // region Key 扫描

    /**
     * 按通配符模式获取匹配的 key 列表
     *
     * @param pattern 通配符模式，如 "user:*"
     */
    public static Collection<String> keys(final String pattern) {
        RKeys keys = getClient().getKeys();
        return StreamSupport.stream(
                keys.getKeys(KeysScanOptions.defaults().pattern(pattern)).spliterator(),
                false
        ).collect(Collectors.toList());
    }

    /**
     * 按通配符模式删除匹配的 key
     *
     * @param pattern 通配符模式，如 "user:*"
     */
    public static void deleteKeys(final String pattern) {
        getClient().getKeys().deleteByPattern(pattern);
    }

    /**
     * 检查 key 是否存在
     */
    public static Boolean hasKey(String key) {
        RKeys rKeys = getClient().getKeys();
        return rKeys.countExists(key) > 0;
    }

    // endregion

    // region 分布式锁

    public static void setLocker(DistributeLocker locker) {
        RedissonUtil.locker = locker;
    }

    // -- 基础加锁/解锁 --

    public static void lock(String lockKey) {
        locker.lock(PREFIX_KEY + lockKey);
    }

    public static void unlock(String lockKey) {
        locker.unlock(PREFIX_KEY + lockKey);
    }

    public static void lock(String lockKey, int timeout) {
        locker.lock(PREFIX_KEY + lockKey, timeout);
    }

    public static void lock(String lockKey, int timeout, TimeUnit unit) {
        locker.lock(PREFIX_KEY + lockKey, timeout, unit);
    }

    public static boolean tryLock(String lockKey) {
        return locker.tryLock(PREFIX_KEY + lockKey);
    }

    public static boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit)
            throws InterruptedException {
        return locker.tryLock(PREFIX_KEY + lockKey, waitTime, leaseTime, unit);
    }

    public static boolean isLocked(String lockKey) {
        return locker.isLocked(PREFIX_KEY + lockKey);
    }

    public static boolean isHeldByCurrentThread(String lockKey) {
        return locker.isHeldByCurrentThread(PREFIX_KEY + lockKey);
    }

    // -- 带自动释放的 lock --

    public static void lock(String lockKey, Runnable handle) {
        lock(lockKey);
        try {
            handle.run();
        } finally {
            if (isHeldByCurrentThread(lockKey)) {
                unlock(lockKey);
            }
        }
    }

    public static <T> T lock(String lockKey, Supplier<T> handle) {
        lock(lockKey);
        try {
            return handle.get();
        } finally {
            if (isHeldByCurrentThread(lockKey)) {
                unlock(lockKey);
            }
        }
    }

    public static void lock(String lockKey, int timeout, Runnable handle) {
        lock(lockKey, timeout);
        try {
            handle.run();
        } finally {
            if (isHeldByCurrentThread(lockKey)) {
                unlock(lockKey);
            }
        }
    }

    public static <T> T lock(String lockKey, int timeout, Supplier<T> handle) {
        lock(lockKey, timeout);
        try {
            return handle.get();
        } finally {
            if (isHeldByCurrentThread(lockKey)) {
                unlock(lockKey);
            }
        }
    }

    public static void lock(String lockKey, int timeout, TimeUnit unit, Runnable handle) {
        lock(lockKey, timeout, unit);
        try {
            handle.run();
        } finally {
            if (isHeldByCurrentThread(lockKey)) {
                unlock(lockKey);
            }
        }
    }

    public static <T> T lock(String lockKey, int timeout, TimeUnit unit, Supplier<T> handle) {
        lock(lockKey, timeout, unit);
        try {
            return handle.get();
        } finally {
            if (isHeldByCurrentThread(lockKey)) {
                unlock(lockKey);
            }
        }
    }

    // -- 带自动释放的 tryLock --

    public static void tryLock(String lockKey, Runnable handle) {
        if (tryLock(lockKey)) {
            try {
                handle.run();
            } finally {
                if (isHeldByCurrentThread(lockKey)) {
                    unlock(lockKey);
                }
            }
        }
    }

    public static <T> T tryLock(String lockKey, Supplier<T> handle) {
        if (tryLock(lockKey)) {
            try {
                return handle.get();
            } finally {
                if (isHeldByCurrentThread(lockKey)) {
                    unlock(lockKey);
                }
            }
        }
        return null;
    }

    public static void tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Runnable handle)
            throws InterruptedException {
        if (tryLock(lockKey, waitTime, leaseTime, unit)) {
            try {
                handle.run();
            } finally {
                if (isHeldByCurrentThread(lockKey)) {
                    unlock(lockKey);
                }
            }
        }
    }

    public static <T> T tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> handle)
            throws InterruptedException {
        if (tryLock(lockKey, waitTime, leaseTime, unit)) {
            try {
                return handle.get();
            } finally {
                if (isHeldByCurrentThread(lockKey)) {
                    unlock(lockKey);
                }
            }
        }
        return null;
    }

    // endregion
}
