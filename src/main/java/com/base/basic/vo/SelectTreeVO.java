package com.base.basic.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @Date: 2025/3/19 15:50
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class SelectTreeVO<T extends SelectTreeVO<T>> extends SelectVO  implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;
	
	private Long parentId;
	private Boolean isLeaf;
	private String idPath;
	private List<T> children;
}
