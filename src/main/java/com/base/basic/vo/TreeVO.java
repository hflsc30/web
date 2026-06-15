package com.base.basic.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @Date: 2024/11/26 10:22
  * @author base
 * @since 2026-06-11
 */
@Data
@NoArgsConstructor
public class TreeVO implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;
	
	private Long id;
	private String name;
	private Integer orderNum;
	private Boolean isAvailable;
	private Long parentId;
	private Short lv;
	private String idPath;
	private String namePath;
	private Boolean isLeaf;
	private Long rootId;
	private Boolean hasChildren;
	
	private List<? extends TreeVO> children;
}
