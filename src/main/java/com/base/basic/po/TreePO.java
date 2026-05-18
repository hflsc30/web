package com.base.basic.po;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@EqualsAndHashCode(callSuper = true)
@Data
public class TreePO extends BasePO<Long> {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 节点名称
     */
    @TableField("name")
    protected String name;
    /**
     * 排序号
     */
    @TableField("order_num")
    protected Integer orderNum;
    /**
     * 是否可用
     */
    @TableField("is_available")
    protected Boolean isAvailable;
    /**
     * 父节点id
     */
    @TableField("parent_id")
    protected Long parentId;
    /**
     * 层级
     */
    @TableField("lv")
    protected Short lv;
    /**
     * id路径
     */
    @TableField("id_path")
    protected String idPath;
    /**
     * 名称路径
     */
    @TableField("name_path")
    protected String namePath;
    /**
     * 是否叶子节点
     */
    @TableField("is_leaf")
    protected Boolean isLeaf;
    /**
     * 根节点id
     */
    @TableField("root_id")
    protected Long rootId;
}
