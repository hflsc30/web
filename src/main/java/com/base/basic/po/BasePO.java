package com.base.basic.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class BasePO<T extends Serializable> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableField(value = "id")
    private T id;
    /**
     * 创建人id
     */
    @TableField(value = "creator_id", fill = FieldFill.INSERT)
    protected T creatorId;
    /**
     * 创建人名称
     */
    @TableField(value = "creator_name", fill = FieldFill.INSERT)
    protected String creatorName;
    /**
     * 更新人id
     */
    @TableField(value = "updater_id", fill = FieldFill.INSERT_UPDATE)
    protected T updaterId;
    /**
     * 更新人名称
     */
    @TableField(value = "updater_name", fill = FieldFill.INSERT_UPDATE)
    protected String updaterName;
    /**
     * 插入时间
     */
    @TableField(value = "insert_time", fill = FieldFill.INSERT)
    private Date insertTime;
    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
