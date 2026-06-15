package com.base.basic.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.base.basic.po.BasePO;
import com.base.result.R;
import com.base.result.RUtil;

import java.io.Serializable;
import java.util.Collection;

/**
 * @author base
 * @since 2026-06-11
 */
@SuppressWarnings("unchecked")
public interface BaseDAO<M, T extends BasePO<ID>, ID extends Serializable> extends BaseMapper<T> {

    default Class<T> currentModelClass() {
        return (Class<T>) ReflectionKit.getSuperClassGenericType(this.getClass(), BaseDAO.class, 1);
    }

    default Class<M> currentMapperClass() {
        return (Class<M>) ReflectionKit.getSuperClassGenericType(this.getClass(), BaseDAO.class, 0);
    }

    /**
     * 批量插入
     *
     * @param entityList 数据
     * @return {@link R}
     */
    default R insertBatch(Collection<T> entityList) {
        return Db.saveBatch(entityList) ? RUtil.success("插入成功") : RUtil.fail("插入失败");
    }

    /**
     * 批量更新
     *
     * @param entityList 数据
     * @return {@link R}
     */
    default R updateBatchById(Collection<T> entityList) {
        return Db.updateBatchById(entityList) ? RUtil.success("更新成功") : RUtil.fail("更新失败");
    }

    /**
     * 批量插入或更新
     *
     * @param entityList 数据
     * @return {@link R}
     */
    default R insertOrUpdateBatch(Collection<T> entityList) {
        return Db.saveOrUpdateBatch(entityList) ? RUtil.success("插入或更新成功") : RUtil.fail("插入或更新失败");
    }

    /**
     * 批量插入
     *
     * @param entityList 数据
     * @param batchSize  限制条数
     * @return {@link R}
     */
    default R insertBatch(Collection<T> entityList, int batchSize) {
        return Db.saveBatch(entityList, batchSize) ? RUtil.success("插入成功") : RUtil.fail("插入失败");
    }
}
