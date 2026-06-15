package com.base.process.config;

import com.base.process.ProcessChain;
import com.base.process.RequestProcessor;
import com.base.process.filter.ProcessInterceptor;
import com.base.process.properties.ProcessProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 请求处理管道自动配置。
 * process.enabled=true 时激活，自动检测 Gateway 环境切换模式。
  * @author base
 * @since 2026-06-11
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "process", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ProcessProperties.class)
public class ProcessAutoConfiguration {

    @PostConstruct
    public void init() {
        log.info("Process pipeline enabled");
    }

    @Bean
    ProcessChain processChain(List<RequestProcessor> processors) {
        return new ProcessChain(processors);
    }

    // ── Servlet 模式拦截器注册 ──
    // 有 Gateway 依赖时跳过，避免和 Gateway 过滤器冲突

    @Bean
    @ConditionalOnMissingClass("org.springframework.cloud.gateway.filter.GlobalFilter")
    ServletInterceptorConfigurer servletInterceptorConfigurer(ProcessChain processChain, ProcessProperties properties) {
        return new ServletInterceptorConfigurer(processChain, properties);
    }

    @ConditionalOnMissingClass("org.springframework.cloud.gateway.filter.GlobalFilter")
    static class ServletInterceptorConfigurer implements WebMvcConfigurer {

        private final ProcessInterceptor interceptor;

        ServletInterceptorConfigurer(ProcessChain processChain, ProcessProperties properties) {
            this.interceptor = new ProcessInterceptor(processChain, properties);
        }

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(interceptor).addPathPatterns("/**");
        }
    }
}
