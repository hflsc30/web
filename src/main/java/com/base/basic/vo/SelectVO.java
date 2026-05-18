package com.base.basic.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Date: 2024/11/26 14:27
 */
@Data
@NoArgsConstructor
public class SelectVO implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private Long key;
	private Long value;
	private String label;
	private Integer orderNum;
}
