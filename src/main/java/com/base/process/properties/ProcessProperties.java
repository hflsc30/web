package com.base.process.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * process.* 配置属性。
  * @author base
 * @since 2026-06-11
 */
@Data
@ConfigurationProperties(prefix = "process")
public class ProcessProperties {

    /** 总开关，默认关闭 */
    private boolean enabled = false;

    /** 网关模式配置 */
    private GatewayConfig gateway = new GatewayConfig();

    /** Servlet 模式配置 */
    private ServletConfig servlet = new ServletConfig();

    /** 各处理器配置 */
    private Map<String, ProcessorDef> processors = new HashMap<>();

    @Data
    public static class GatewayConfig {
        /** 需要处理的路径 */
        private List<String> pathPatterns = new ArrayList<>();
        /** 排除的路径 */
        private List<String> excludePatterns = new ArrayList<>();
        /** 网关对所有匹配路径默认执行的处理器类名 */
        private List<String> defaultProcessorNames = new ArrayList<>();
    }

    @Data
    public static class ServletConfig {
        /** 无 @Process 注解时默认执行的处理器类名 */
        private List<String> defaultProcessorNames = new ArrayList<>();
    }

    @Data
    public static class ProcessorDef {
        /** 是否启用 */
        private boolean enabled = true;
        /** 执行顺序 */
        private int order = 0;
        /** 处理器配置（自由扩展） */
        private Map<String, Object> config = new HashMap<>();
    }
}
