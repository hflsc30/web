package com.base.process;

/**
 * 请求处理器接口，所有处理器需实现此接口并注册为 Spring Bean。
 * ProcessChain 会自动发现并串联所有处理器。
  * @author base
 * @since 2026-06-11
 */
public interface RequestProcessor {

    /** 处理器名称 */
    String name();

    /** 执行顺序，值越小越先执行 */
    default int order() {
        return 0;
    }

    /** 前置处理（进入 Controller 前），返回 false 中断链路 */
    default boolean preProcess(RequestContext ctx) {
        return true;
    }

    /** 后置处理（返回结果前） */
    default void postProcess(RequestContext ctx) {
    }

    /** 网关模式下是否执行 */
    default boolean runOnGateway() {
        return true;
    }
}
