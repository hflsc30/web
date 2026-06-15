package com.base.config;

import com.base.aspect.MyTransactionAspect;
import com.base.util.SpringUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * @author base
 * @since 2026-06-11
 */
@AutoConfiguration
public class BaseAutoConfiguration {

    @Bean
    public SpringUtil springUtil() {
        return new SpringUtil();
    }

    @Bean
    @ConditionalOnClass(name = "org.apache.seata.spring.annotation.GlobalTransactional")
    public MyTransactionAspect myTransactionAspect() {
        return new MyTransactionAspect();
    }
}
