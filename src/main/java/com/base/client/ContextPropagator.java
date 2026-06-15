package com.base.client;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * 上下文透传工具：从当前 HTTP 请求中提取指定请求头，设置到下游请求头。
  * @author base
 * @since 2026-06-11
 */
public final class ContextPropagator {

    private ContextPropagator() {
    }

    /**
     * 从当前请求上下文透传指定请求头到目标 headers。
     */
    public static void propagate(HttpHeaders target, List<String> headerNames) {
        if (headerNames == null || headerNames.isEmpty()) {
            return;
        }
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sa) {
            HttpServletRequest req = sa.getRequest();
            for (String name : headerNames) {
                String value = req.getHeader(name);
                if (value != null) {
                    target.set(name, value);
                }
            }
        }
    }
}
