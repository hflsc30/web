package com.base.basic.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Date: 2025/6/16 16:20
 */
@Data
@NoArgsConstructor
public class CheckedVO implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;
	
	private Long value;
	private Boolean isSystem;
}
