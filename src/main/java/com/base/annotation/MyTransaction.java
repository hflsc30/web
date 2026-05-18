package com.base.annotation;

import java.lang.annotation.*;

/**
 * @author base
 * @since 2026-05-15
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyTransaction {
	/**
	 * 事务名称
	 * 用于日志记录和事务标识
	 */
	String value() default "";

	/**
	 * 事务名称
	 */
	String name() default "";

	/**
	 * 是否启用异常回滚，默认为true
	 */
	boolean rollbackOnBusinessFail() default true;
}
