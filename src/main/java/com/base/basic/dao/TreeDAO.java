package com.base.basic.dao;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.base.basic.po.TreePO;
import com.base.basic.vo.SelectTreeVO;
import com.base.basic.vo.TreeVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * @author base
 * @since 2026-06-11
 */
public interface TreeDAO<M, T extends TreePO> extends BaseDAO<M, T, Long> {

	<V extends TreeVO> Page<V> findDataList(@Param("parentId") Long parentId, @Param("paramMap") Map<String, Object> paramMap, @Param("page") Page<V> page);

	<V extends TreeVO> List<V> findDataList(@Param("parentId") Long parentId, @Param("paramMap") Map<String, Object> paramMap);

	<V extends TreeVO> List<V> findChildrenList(@Param("parentId") Long parentId, @Param("paramMap") Map<String, Object> paramMap);

	<V extends TreeVO> List<V> findChildrenListByParentIds(@Param("parentIds") List<Long> parentIds, @Param("paramMap") Map<String, Object> paramMap);

	<V extends SelectTreeVO<V>> List<V> select(@Param("paramMap") Map<String, Object> paramMap);

	<V extends SelectTreeVO<V>> List<V> loadNodeList(@Param("idList") List<Long> idList, @Param("idPathList") List<String> idPathList);
}
