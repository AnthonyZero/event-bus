package com.anthonyzero.eventbus.config;

import com.anthonyzero.eventbus.core.api.RequestIdGenerator;
import com.anthonyzero.eventbus.core.base.UUIDRequestIdGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Lazy
@Configuration(proxyBeanMethods = false)
class RequestIdGeneratorConfiguration {
    private RequestIdGeneratorConfiguration() {
    }

    @Configuration(proxyBeanMethods = false)
    static class RequestIdAutoConfig {

        @Bean
        @ConditionalOnMissingBean
        public RequestIdGenerator requestIdGenerator() {
            return new UUIDRequestIdGenerator();
        }
    }
}
