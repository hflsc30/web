package com.base.basic.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.override.MybatisMapperProxy;
import com.baomidou.mybatisplus.core.toolkit.*;
import com.baomidou.mybatisplus.core.toolkit.reflect.GenericTypeUtils;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.base.annotation.MyTransaction;
import com.base.basic.service.CustomService;
import com.base.result.R;
import com.base.result.RUtil;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public class CustomServiceImpl<M extends BaseMapper<T>, T> implements CustomService<T> {
    private final ConversionService conversionService = DefaultConversionService.getSharedInstance();

    protected final Log log = LogFactory.getLog(getClass());

    @Autowired
    protected M dao;

    protected final Class<?>[] typeArguments = GenericTypeUtils.resolveTypeArguments(getClass(), CustomServiceImpl.class);

    @Override
    public M getBaseMapper() {
        return dao;
    }

    protected final Class<T> entityClass = currentModelClass();

    @Override
    public Class<T> getEntityClass() {
        return entityClass;
    }

    protected final Class<M> mapperClass = currentMapperClass();

    private volatile SqlSessionFactory sqlSessionFactory;

    @SuppressWarnings({"rawtypes", "deprecation"})
    protected SqlSessionFactory getSqlSessionFactory() {
        if (this.sqlSessionFactory == null) {
            synchronized (this) {
                if (this.sqlSessionFactory == null) {
                    Object target = this.dao;
                    if (AopUtils.isAopProxy(this.dao)) {
                        target = AopProxyUtils.getSingletonTarget(this.dao);
                    }
                    if (target != null) {
                        MybatisMapperProxy mybatisMapperProxy = (MybatisMapperProxy) Proxy.getInvocationHandler(target);
                        SqlSessionTemplate sqlSessionTemplate = (SqlSessionTemplate) mybatisMapperProxy.getSqlSession();
                        this.sqlSessionFactory = sqlSessionTemplate.getSqlSessionFactory();
                    } else {
                        this.sqlSessionFactory = GlobalConfigUtils.currentSessionFactory(this.entityClass);
                    }
                }
            }
        }
        return this.sqlSessionFactory;
    }

    protected Class<M> currentMapperClass() {
        return (Class<M>) this.typeArguments[0];
    }

    protected Class<T> currentModelClass() {
        return (Class<T>) this.typeArguments[1];
    }

    /**
     * 批量插入
     *
     * @param entityList ignore
     * @param batchSize  ignore
     * @return ignore
     */
    @GlobalTransactional
    @MyTransaction
    @Override
    public R saveBatch(Collection<T> entityList, int batchSize) {
        String sqlStatement = getSqlStatement(SqlMethod.INSERT_ONE);
        return executeBatch(entityList, batchSize, (sqlSession, entity) -> sqlSession.insert(sqlStatement, entity));
    }

    /**
     * 获取mapperStatementId
     *
     * @param sqlMethod 方法名
     * @return 命名id
     * @since 3.4.0
     */
    protected String getSqlStatement(SqlMethod sqlMethod) {
        return SqlHelper.getSqlStatement(mapperClass, sqlMethod);
    }

    /**
     * TableId 注解存在更新记录，否插入一条记录
     *
     * @param entity 实体对象
     * @return boolean
     */
    @GlobalTransactional
    @MyTransaction
    @Override
    public R saveOrUpdate(T entity) {
        if (null != entity) {
            TableInfo tableInfo = TableInfoHelper.getTableInfo(this.entityClass);
            Assert.notNull(tableInfo, "错误: 无法执行，因为找不到实体的TableInfo缓存!");
            String keyProperty = tableInfo.getKeyProperty();
            Assert.notEmpty(keyProperty, "错误: 无法执行，因为在实体中找不到ID列!");
            Object idVal = tableInfo.getPropertyValue(entity, tableInfo.getKeyProperty());
            return StringUtils.checkValNull(idVal) || Objects.isNull(getById((Serializable) idVal)) ? save(entity) : updateById(entity);
        }
        return RUtil.fail("保存失败");
    }

    @GlobalTransactional
    @MyTransaction
    @Override
    public R saveOrUpdateBatch(Collection<T> entityList, int batchSize) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
        Assert.notNull(tableInfo, "错误: 无法执行，因为找不到实体的TableInfo缓存!");
        String keyProperty = tableInfo.getKeyProperty();
        Assert.notEmpty(keyProperty, "错误: 无法执行，因为在实体中找不到ID列!");
        boolean result = SqlHelper.saveOrUpdateBatch(getSqlSessionFactory(), this.mapperClass, this.log, entityList, batchSize, (sqlSession, entity) -> {
            Object idVal = tableInfo.getPropertyValue(entity, keyProperty);
            return StringUtils.checkValNull(idVal)
                    || CollectionUtils.isEmpty(sqlSession.selectList(getSqlStatement(SqlMethod.SELECT_BY_ID), entity));
        }, (sqlSession, entity) -> {
            MapperMethod.ParamMap<T> param = new MapperMethod.ParamMap<>();
            param.put(Constants.ENTITY, entity);
            sqlSession.update(getSqlStatement(SqlMethod.UPDATE_BY_ID), param);
        });

        if (result) {
            return RUtil.success("保存成功");
        } else {
            return RUtil.fail("保存失败");
        }
    }

    @GlobalTransactional
    @MyTransaction
    @Override
    public R updateBatchById(Collection<T> entityList, int batchSize) {
        String sqlStatement = getSqlStatement(SqlMethod.UPDATE_BY_ID);
        return executeBatch(entityList, batchSize, (sqlSession, entity) -> {
            MapperMethod.ParamMap<T> param = new MapperMethod.ParamMap<>();
            param.put(Constants.ENTITY, entity);
            sqlSession.update(sqlStatement, param);
        });
    }

    @Override
    public T getOne(Wrapper<T> queryWrapper, boolean throwEx) {
        return dao.selectOne(queryWrapper, throwEx);
    }

    @Override
    public Optional<T> getOneOpt(Wrapper<T> queryWrapper, boolean throwEx) {
        return Optional.ofNullable(dao.selectOne(queryWrapper, throwEx));
    }

    @Override
    public Map<String, Object> getMap(Wrapper<T> queryWrapper) {
        return SqlHelper.getObject(log, dao.selectMaps(queryWrapper));
    }

    @Override
    public <V> V getObj(Wrapper<T> queryWrapper, Function<? super Object, V> mapper) {
        return SqlHelper.getObject(log, listObjs(queryWrapper, mapper));
    }

    /**
     * 执行批量操作
     *
     * @param list      数据集合
     * @param batchSize 批量大小
     * @param consumer  执行方法
     * @param <E>       泛型
     * @return 操作结果
     * @since 3.3.1
     */
    protected <E> R executeBatch(Collection<E> list, int batchSize, BiConsumer<SqlSession, E> consumer) {
        boolean result = SqlHelper.executeBatch(getSqlSessionFactory(), this.log, list, batchSize, consumer);
        if (result) {
            return RUtil.success("操作成功");
        }
        return RUtil.fail("操作失败");
    }

    /**
     * 执行批量操作（默认批次提交数量{@link IService#DEFAULT_BATCH_SIZE}）
     *
     * @param list     数据集合
     * @param consumer 执行方法
     * @param <E>      泛型
     * @return 操作结果
     * @since 3.3.1
     */
    protected <E> R executeBatch(Collection<E> list, BiConsumer<SqlSession, E> consumer) {
        return executeBatch(list, DEFAULT_BATCH_SIZE, consumer);
    }

    @GlobalTransactional
    @MyTransaction
    @Override
    public R removeById(Serializable id) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(getEntityClass());
        if (tableInfo.isWithLogicDelete() && tableInfo.isWithUpdateFill()) {
            return removeById(id, true);
        }
        boolean result = SqlHelper.retBool(getBaseMapper().deleteById(id));
        if (result) {
            return RUtil.success("删除成功");
        } else {
            return RUtil.fail("删除失败");
        }
    }

    @GlobalTransactional
    @MyTransaction
    @Override
    public R removeByIds(Collection<?> list) {
        if (CollectionUtils.isEmpty(list)) {
            return RUtil.fail("删除失败");
        }
        TableInfo tableInfo = TableInfoHelper.getTableInfo(getEntityClass());
        if (tableInfo.isWithLogicDelete() && tableInfo.isWithUpdateFill()) {
            return removeBatchByIds(list, true);
        }
        boolean result = SqlHelper.retBool(getBaseMapper().deleteByIds(list));
        if (result) {
            return RUtil.success("删除成功");
        } else {
            return RUtil.fail("删除失败");
        }
    }

    @GlobalTransactional
    @MyTransaction
    @Override
    public R removeById(Serializable id, boolean useFill) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
        if (useFill && tableInfo.isWithLogicDelete()) {
            if (!entityClass.isAssignableFrom(id.getClass())) {
                T instance = tableInfo.newInstance();
                Object value = tableInfo.getKeyType() != id.getClass() ? conversionService.convert(id, tableInfo.getKeyType()) : id;
                tableInfo.setPropertyValue(instance, tableInfo.getKeyProperty(), value);
                return removeById(instance);
            }
        }
        boolean result = SqlHelper.retBool(getBaseMapper().deleteById(id));
        if (result) {
            return RUtil.success("删除成功");
        } else {
            return RUtil.fail("删除失败");
        }
    }

    @GlobalTransactional
    @MyTransaction
    @Override
    public R removeBatchByIds(Collection<?> list, int batchSize) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
        return removeBatchByIds(list, batchSize, tableInfo.isWithLogicDelete() && tableInfo.isWithUpdateFill());
    }

    @GlobalTransactional
    @MyTransaction
    @Override
    public R removeBatchByIds(Collection<?> list, int batchSize, boolean useFill) {
        String sqlStatement = getSqlStatement(SqlMethod.DELETE_BY_ID);
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
        return executeBatch(list, batchSize, (sqlSession, e) -> {
            if (useFill && tableInfo.isWithLogicDelete()) {
                if (entityClass.isAssignableFrom(e.getClass())) {
                    sqlSession.update(sqlStatement, e);
                } else {
                    T instance = tableInfo.newInstance();
                    Object value = tableInfo.getKeyType() != e.getClass() ? conversionService.convert(e, tableInfo.getKeyType()) : e;
                    tableInfo.setPropertyValue(instance, tableInfo.getKeyProperty(), value);
                    sqlSession.update(sqlStatement, instance);
                }
            } else {
                sqlSession.update(sqlStatement, e);
            }
        });
    }
}
