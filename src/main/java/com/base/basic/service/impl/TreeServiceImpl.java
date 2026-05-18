package com.base.basic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.base.annotation.MyTransaction;
import com.base.basic.dao.TreeDAO;
import com.base.basic.po.TreePO;
import com.base.basic.vo.SelectTreeVO;
import com.base.basic.vo.TreeVO;
import com.base.basic.service.TreeService;
import com.base.result.PageR;
import com.base.result.PageRUtil;
import com.base.result.R;
import com.base.result.RUtil;
import com.base.util.ClassUtil;
import com.base.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.seata.spring.annotation.GlobalTransactional;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class TreeServiceImpl<M extends TreeDAO<M, T>, T extends TreePO> extends BaseServiceImpl<M, T, Long> implements TreeService<T> {

    @GlobalTransactional
    @MyTransaction
    @Override
    public R saveOrUpdate(T entity) {
        if (entity.getId() != null) {
            T oldEntity = dao.selectById(entity.getId());
            if (oldEntity == null) {
                return RUtil.fail("数据不存在");
            }

            if (entity.getParentId() != 0L) {
                T parent = dao.selectById(entity.getParentId());
                if (parent == null) {
                    return RUtil.fail("上级节点不存在");
                }
                entity.setRootId(parent.getRootId());
                entity.setNamePath(parent.getNamePath() + ">" + entity.getName());
                entity.setIdPath(parent.getIdPath() + entity.getId() + ">");
                entity.setLv((short) (parent.getLv() + 1));
            } else {
                entity.setRootId(entity.getId());
                entity.setNamePath(entity.getName());
                entity.setIdPath(">" + entity.getId() + ">");
                entity.setLv((short) (1));
            }

            R result = super.saveOrUpdate(entity);
            if (!result.isSuccess()) {
                return result;
            }

            QueryWrapper<T> wrapper = new QueryWrapper<>(ClassUtil.getClass(entity));
            wrapper.apply("id_path like concat('%>',{0},'>%')", entity.getId());
            wrapper.ne("id", entity.getId());
            List<T> children = dao.selectList(wrapper);
            if (children != null && !children.isEmpty()) {
                boolean isParentChanged = !Objects.equals(entity.getParentId(), oldEntity.getParentId());
                boolean isNameChanged = !entity.getName().equals(oldEntity.getName());
                boolean isAvailableChanged = !Objects.equals(entity.getIsAvailable(), oldEntity.getIsAvailable());

                if (isParentChanged || isNameChanged || isAvailableChanged) {
                    for (T child : children) {
                        if (isParentChanged) {
                            child.setRootId(entity.getRootId());
                            child.setIdPath(StrUtil.replace(child.getIdPath(), oldEntity.getIdPath(), entity.getIdPath()));
                            child.setNamePath(StrUtil.replace(child.getNamePath(), oldEntity.getNamePath() + ">", entity.getNamePath() + ">"));
                            child.setLv((short) (child.getLv() + entity.getLv() - oldEntity.getLv()));
                        } else if (isNameChanged) {
                            child.setNamePath(StrUtil.replace(child.getNamePath(), ">" + oldEntity.getName() + ">", ">" + entity.getName() + ">"));
                        }
                        if (isAvailableChanged) {
                            child.setIsAvailable(entity.getIsAvailable());
                        }
                    }
                    result = super.saveOrUpdateBatch(children);
                    if (!result.isSuccess()) {
                        return result;
                    }
                }
            }
        } else {
            T parent = null;
            if (entity.getParentId() != 0L) {
                parent = dao.selectById(entity.getParentId());
                if (parent == null) {
                    return RUtil.fail("上级节点不存在");
                }

                if (parent.getIsLeaf()) {
                    parent.setIsLeaf(false);
                    R result = super.saveOrUpdate(parent);
                    if (!result.isSuccess()) {
                        return result;
                    }
                }

                entity.setRootId(parent.getRootId());
                entity.setNamePath(parent.getNamePath() + ">" + entity.getName());
                entity.setLv((short) (parent.getLv() + 1));
            } else {
                entity.setNamePath(entity.getName());
                entity.setLv((short) 1);
            }
            entity.setIsLeaf(true);
            R result = super.saveOrUpdate(entity);
            if (!result.isSuccess()) {
                return result;
            }
            if (parent == null) {
                entity.setRootId(entity.getId());
                entity.setIdPath(">" + entity.getId() + ">");
            } else {
                if (!parent.getIsAvailable()) {
                    entity.setIsAvailable(false);
                }
                entity.setIdPath(parent.getIdPath() + entity.getId() + ">");
            }
            result = super.saveOrUpdate(entity);
            if (!result.isSuccess()) {
                return result;
            }
        }

        return RUtil.success("保存成功");
    }

    @GlobalTransactional
    @MyTransaction
    @Override
    public R removeById(Serializable id) {
        T entity = dao.selectById(id);

        if (entity == null) {
            return RUtil.fail("数据不存在");
        }

        QueryWrapper<T> childWrapper = new QueryWrapper<>();
        childWrapper.eq("parent_id", id);
        List<T> activeChildren = dao.selectList(childWrapper);

        if (activeChildren != null && !activeChildren.isEmpty()) {
            return RUtil.fail("存在子节点，无法删除");
        }

        if (entity.getParentId() != null && entity.getParentId() != 0L) {
            T parent = dao.selectById(entity.getParentId());
            if (parent == null) {
                return RUtil.fail("上级节点不存在");
            }

            QueryWrapper<T> wrapper = new QueryWrapper<>();
            wrapper.eq("parent_id", entity.getParentId());
            List<T> children = dao.selectList(wrapper);

            if (children != null && children.size() == 1 && children.getFirst().getId().equals(entity.getId())) {
                parent.setIsLeaf(true);
                R result = super.saveOrUpdate(parent);
                if (!result.isSuccess()) {
                    return result;
                }
            }
        }

        return super.removeById(id);
    }

    @Override
    public <V extends TreeVO> PageR<List<V>> findDataList(Long parentId, Boolean isLazy, Map<String, Object> paramMap, Page<V> page) {
        Page<V> result = dao.findDataList(parentId, paramMap, page);
        boolean eager = isLazy == null || !isLazy;
        if (result != null && eager && result.getRecords() != null && !result.getRecords().isEmpty()) {
            List<Long> parentIds = result.getRecords().stream().map(V::getId).toList();
            List<V> allChildren = dao.findChildrenListByParentIds(parentIds, paramMap);
            if (allChildren != null && !allChildren.isEmpty()) {
                Map<Long, List<V>> childrenByParentId = allChildren.stream()
                        .collect(Collectors.groupingBy(V::getParentId));
                for (V item : result.getRecords()) {
                    List<V> directChildren = childrenByParentId.getOrDefault(item.getId(), Collections.emptyList());
                    if (!directChildren.isEmpty()) {
                        item.setChildren(buildTree(directChildren));
                    }
                }
            }
        }
        return PageRUtil.build(result);
    }

    @Override
    public <V extends TreeVO> List<V> findLazyLoadDataList(Long parentId, Map<String, Object> paramMap) {
        return dao.findDataList(parentId, paramMap);
    }

    @Override
    public <V extends TreeVO> List<V> buildTree(List<V> dataList) {
        return buildTreeInternal(dataList);
    }

    @Override
    public <V extends SelectTreeVO<V>> List<V> select(Map<String, Object> paramMap) {
        return dao.select(paramMap);
    }

    @Override
    public <V extends SelectTreeVO<V>> List<V> loadNodeList(List<Long> idList) {
        List<V> result = new ArrayList<>();

        List<T> nodeList = dao.selectByIds(idList);
        if (nodeList == null || nodeList.isEmpty()) {
            return result;
        }

        List<String> idPathList = nodeList.stream().map(T::getIdPath).toList();
        List<V> selectTreeVOList = dao.loadNodeList(idList, idPathList);

        if (selectTreeVOList == null || selectTreeVOList.isEmpty()) {
            return result;
        }

        return buildSelectTree(selectTreeVOList);
    }

    // region 高效建树（O(n log n)）

    private <V extends TreeVO> List<V> buildTreeInternal(List<V> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, List<V>> childrenMap = groupByParentIdPath(dataList);

        Set<String> idPathSet = dataList.stream().map(V::getIdPath).collect(Collectors.toSet());

        List<V> roots = new ArrayList<>();
        for (V node : dataList) {
            String parentIdPath = getParentIdPath(node.getIdPath());
            if (!idPathSet.contains(parentIdPath)) {
                roots.add(node);
            }
        }

        roots.sort(Comparator.comparing(V::getOrderNum, Comparator.nullsLast(Comparator.naturalOrder())));

        for (V root : roots) {
            attachChildren(root, childrenMap);
        }

        return roots;
    }

    private <V extends TreeVO> void attachChildren(V parent, Map<String, List<V>> childrenMap) {
        List<V> children = childrenMap.get(parent.getIdPath());
        if (children == null || children.isEmpty()) {
            return;
        }
        children.sort(Comparator.comparing(V::getOrderNum, Comparator.nullsLast(Comparator.naturalOrder())));
        parent.setChildren(new ArrayList<>(children));
        for (V child : children) {
            attachChildren(child, childrenMap);
        }
    }

    @SuppressWarnings("unchecked")
    private <V extends SelectTreeVO<V>> List<V> buildSelectTree(List<V> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, List<V>> childrenMap = new LinkedHashMap<>();
        for (V node : dataList) {
            String parentIdPath = getParentIdPath(node.getIdPath());
            childrenMap.computeIfAbsent(parentIdPath, k -> new ArrayList<>()).add(node);
        }

        Set<String> idPathSet = dataList.stream().map(V::getIdPath).collect(Collectors.toSet());

        List<V> roots = new ArrayList<>();
        for (V node : dataList) {
            String parentIdPath = getParentIdPath(node.getIdPath());
            if (!idPathSet.contains(parentIdPath)) {
                roots.add(node);
            }
        }

        for (V root : roots) {
            attachSelectChildren(root, childrenMap);
        }

        return roots;
    }

    private <V extends SelectTreeVO<V>> void attachSelectChildren(V parent, Map<String, List<V>> childrenMap) {
        List<V> children = childrenMap.get(parent.getIdPath());
        if (children == null || children.isEmpty()) {
            return;
        }
        parent.setChildren(new ArrayList<>(children));
        for (V child : children) {
            attachSelectChildren(child, childrenMap);
        }
    }

    private <V extends TreeVO> Map<String, List<V>> groupByParentIdPath(List<V> dataList) {
        Map<String, List<V>> map = new LinkedHashMap<>();
        for (V node : dataList) {
            String parentIdPath = getParentIdPath(node.getIdPath());
            map.computeIfAbsent(parentIdPath, k -> new ArrayList<>()).add(node);
        }
        return map;
    }

    private String getParentIdPath(String idPath) {
        if (idPath == null || idPath.isEmpty()) {
            return "";
        }
        int secondLastGt = idPath.lastIndexOf('>', idPath.length() - 2);
        return secondLastGt > 0 ? idPath.substring(0, secondLastGt + 1) : "";
    }

    // endregion
}
