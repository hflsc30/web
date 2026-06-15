package com.base.client;

import com.base.client.properties.ClientProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 微服务 HTTP 客户端自动配置。
 * 仅在 spring-boot-starter-webflux 存在且 client.enabled=true 时激活。
  * @author base
 * @since 2026-06-11
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(WebClient.class)
@ConditionalOnProperty(prefix = "client", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ClientProperties.class)
public class ClientAutoConfiguration {

    public ClientAutoConfiguration() {
        log.info("HTTP client module enabled");
    }

    @Bean
    @ConditionalOnMissingBean
    WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    ClientTokenFilter clientTokenFilter(ClientProperties properties) {
        return new ClientTokenFilter(properties);
    }
}
