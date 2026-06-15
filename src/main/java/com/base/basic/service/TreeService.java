package com.base.basic.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.base.basic.po.TreePO;
import com.base.basic.vo.SelectTreeVO;
import com.base.basic.vo.TreeVO;
import com.base.result.PageR;

import java.util.List;
import java.util.Map;

/**
 * @author base
 * @since 2026-06-11
 */
public interface TreeService<T extends TreePO> extends BaseService<T, Long> {

	<V extends TreeVO> PageR<List<V>> findDataList(Long parentId, Boolean isLazyLoad, Map<String, Object> paramMap, Page<V> page);

	<V extends TreeVO> List<V> findLazyLoadDataList(Long parentId, Map<String, Object> paramMap);

	/**
	 * 构建树结构的数据树表
	 *
	 * @param dataList 数据列表
	 * @return 构建完成的数据树表
	 */
	<V extends TreeVO> List<V> buildTree(List<V> dataList);

	/**
	 * 选择树列表
	 *
	 * @param paramMap 参数条件
	 * @return 树列表
	 */
	<V extends SelectTreeVO<V>> List<V> select(Map<String, Object> paramMap);

	/**
	 * 获取当前节点信息，包括上级层级
	 *
	 * @param idList 当前节点id
	 * @return 当前节点信息，包括上级层级
	 */
	<V extends SelectTreeVO<V>> List<V> loadNodeList(List<Long> idList);

}
