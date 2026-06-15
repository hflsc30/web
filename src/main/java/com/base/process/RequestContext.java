package com.base.process;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一请求上下文，封装 HttpServletRequest/Response，提供处理器间数据传递。
  * @author base
 * @since 2026-06-11
 */
@Getter
@Accessors(chain = true)
public class RequestContext {

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    /** 处理器间共享属性 */
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    /** 请求开始时间戳，用于耗时计算 */
    private final long startTime = System.currentTimeMillis();

    /** 处理器链是否被中断 */
    @Setter
    private boolean chainBroken;

    /** 原始请求体（网关模式下缓存用） */
    @Setter
    private byte[] body;

    public RequestContext(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public String getMethod() {
        return request.getMethod();
    }

    public String getUri() {
        return request.getRequestURI();
    }

    public long getElapsed() {
        return System.currentTimeMillis() - startTime;
    }
}
