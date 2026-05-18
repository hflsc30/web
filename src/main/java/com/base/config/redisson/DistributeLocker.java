package com.base.config.redisson;

import java.util.concurrent.TimeUnit;

public interface DistributeLocker {

    /**
     * 加锁
     *
     * @param lockKey key
     */
    void lock(String lockKey);

    /**
     * 释放锁
     *
     * @param lockKey key
     */
    void unlock(String lockKey);

    /**
     * 加锁，设置有效期（毫秒）
     *
     * @param lockKey key
     * @param timeout 有效时间，单位毫秒
     */
    void lock(String lockKey, int timeout);

    /**
     * 加锁，设置有效期并指定时间单位
     *
     * @param lockKey key
     * @param timeout 有效时间
     * @param unit    时间单位
     */
    void lock(String lockKey, int timeout, TimeUnit unit);

    /**
     * 尝试获取锁，获取到则持有该锁返回true,未获取到立即返回false
     *
     * @param lockKey key
     * @return true-获取锁成功 false-获取锁失败
     */
    boolean tryLock(String lockKey);

    /**
     * 尝试获取锁，获取到则持有该锁leaseTime时间.
     * 若未获取到，在waitTime时间内一直尝试获取，超过waitTime还未获取到则返回false
     *
     * @param lockKey   key
     * @param waitTime  尝试获取时间
     * @param leaseTime 锁持有时间
     * @param unit      时间单位
     * @return true-获取锁成功 false-获取锁失败
     */
    boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit)
            throws InterruptedException;

    /**
     * 锁是否被任意一个线程锁持有
     *
     * @param lockKey key
     * @return true-被锁 false-未被锁
     */
    boolean isLocked(String lockKey);

    /**
     * 查询当前线程是否持有此锁
     *
     * @param lockKey key
     * @return true-当前线程持有 false-未持有
     */
    boolean isHeldByCurrentThread(String lockKey);

}
