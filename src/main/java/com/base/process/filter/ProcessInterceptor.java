package com.base.process.filter;

import com.base.process.ProcessChain;
import com.base.process.RequestContext;
import com.base.process.RequestProcessor;
import com.base.process.annotation.Process;
import com.base.process.properties.ProcessProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Servlet 模式拦截器，从 @Process 注解和全局配置中合并处理器列表，交给 ProcessChain 执行。
  * @author base
 * @since 2026-06-11
 */
@Slf4j
@RequiredArgsConstructor
public class ProcessInterceptor implements HandlerInterceptor {

    private final ProcessChain processChain;
    private final ProcessProperties properties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }

        List<Class<? extends RequestProcessor>> processorClasses = resolveProcessors(hm);
        if (processorClasses.isEmpty()) {
            return true;
        }

        RequestContext ctx = new RequestContext(request, response);
        return processChain.preProcess(ctx, processorClasses);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        if (!(handler instanceof HandlerMethod)) {
            return;
        }

        List<Class<? extends RequestProcessor>> processorClasses = resolveProcessors((HandlerMethod) handler);
        if (processorClasses.isEmpty()) {
            return;
        }

        RequestContext ctx = new RequestContext(request, response);
        processChain.postProcess(ctx);
    }

    @SuppressWarnings("unchecked")
    private List<Class<? extends RequestProcessor>> resolveProcessors(HandlerMethod hm) {
        // 方法级别注解优先
        Process methodAnn = hm.getMethodAnnotation(Process.class);
        if (methodAnn != null) {
            if (methodAnn.skip()) {
                return Collections.emptyList();
            }
            return resolve(methodAnn);
        }

        // 类级别注解
        Process classAnn = hm.getBeanType().getAnnotation(Process.class);
        if (classAnn != null) {
            if (classAnn.skip()) {
                return Collections.emptyList();
            }
            return resolve(classAnn);
        }

        // 全局默认处理器
        List<String> defaults = properties.getServlet().getDefaultProcessorNames();
        if (defaults.isEmpty()) {
            return Collections.emptyList();
        }
        return resolveByName(defaults);
    }

    @SuppressWarnings("unchecked")
    private List<Class<? extends RequestProcessor>> resolve(Process ann) {
        Class<? extends RequestProcessor>[] includes = ann.include();
        Class<? extends RequestProcessor>[] excludes = ann.exclude();

        List<Class<? extends RequestProcessor>> result = new ArrayList<>();
        if (includes.length > 0) {
            Collections.addAll(result, includes);
        } else {
            // include 为空时使用全局默认
            result.addAll(resolveByName(properties.getServlet().getDefaultProcessorNames()));
        }
        for (Class<? extends RequestProcessor> ex : excludes) {
            result.remove(ex);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Class<? extends RequestProcessor>> resolveByName(List<String> names) {
        List<Class<? extends RequestProcessor>> result = new ArrayList<>();
        List<RequestProcessor> all = processChain.processors();
        for (String name : names) {
            for (RequestProcessor p : all) {
                if (p.name().equals(name)) {
                    result.add((Class<? extends RequestProcessor>) p.getClass());
                    break;
                }
            }
        }
        return result;
    }
}
