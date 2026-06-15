package com.base.process.annotation;

import com.base.process.RequestProcessor;

import java.lang.annotation.*;

/**
 * 标记接口需要执行的处理器。
 * 可用于类级别（该类所有方法默认行为）或方法级别（覆盖类级别）。
  * @author base
 * @since 2026-06-11
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Process {

    /** 启用的处理器 */
    Class<? extends RequestProcessor>[] include() default {};

    /** 排除的处理器 */
    Class<? extends RequestProcessor>[] exclude() default {};

    /** 跳过所有处理（包括全局默认处理器） */
    boolean skip() default false;
}
