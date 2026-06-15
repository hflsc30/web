package com.base.client.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * client.* 配置属性。
  * @author base
 * @since 2026-06-11
 */
@Data
@ConfigurationProperties(prefix = "client")
public class ClientProperties {

    /** 是否启用，默认 true */
    private boolean enabled = true;

    /** 需要透传到下游请求的请求头名称 */
    private List<String> propagateHeaders = new ArrayList<>();

    /** 连接超时 */
    private Duration connectTimeout = Duration.ofSeconds(3);

    /** 响应超时 */
    private Duration responseTimeout = Duration.ofSeconds(10);
}
