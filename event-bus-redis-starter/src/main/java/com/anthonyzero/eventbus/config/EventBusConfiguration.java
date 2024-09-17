package com.anthonyzero.eventbus.config;

import com.anthonyzero.eventbus.core.api.interceptor.*;
import com.anthonyzero.eventbus.prop.EventBusProperties;
import com.anthonyzero.eventbus.SpringBootConnectionWatchdog;
import com.anthonyzero.eventbus.core.api.MsgSender;
import com.anthonyzero.eventbus.core.base.Lifecycle;
import com.anthonyzero.eventbus.core.base.NodeTestConnect;
import com.anthonyzero.eventbus.core.config.GlobalConfig;
import com.anthonyzero.eventbus.core.config.InterceptorConfig;
import com.anthonyzero.eventbus.core.part.ConnectionWatchdog;
import com.anthonyzero.eventbus.core.part.DeliveryEventBus;
import com.anthonyzero.eventbus.core.part.ListenerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * @author : jin.ping
 * @date : 2024/9/5
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(EventBusProperties.class)
@ImportAutoConfiguration({EventBusRedisConfiguration.class})
@ConditionalOnProperty(prefix = "eventbus", name = "enable", havingValue = "true", matchIfMissing = true)
public class EventBusConfiguration {

    /**
     * 事件总线拦截器配置
     */
    @Bean
    @ConditionalOnMissingBean(InterceptorConfig.class)
    public InterceptorConfig interceptorConfig(
            @Autowired(required = false) SendBeforeInterceptor sendBeforeInterceptor,
            @Autowired(required = false) SendAfterInterceptor sendAfterInterceptor,
            @Autowired(required = false) DeliverSuccessInterceptor deliverSuccessInterceptor,
            @Autowired(required = false) DeliverThrowableEveryInterceptor deliverThrowableEveryInterceptor,
            @Autowired(required = false) DeliverThrowableInterceptor deliverExceptionInterceptor) {
        return new InterceptorConfig(sendBeforeInterceptor, sendAfterInterceptor, deliverSuccessInterceptor, deliverThrowableEveryInterceptor, deliverExceptionInterceptor);
    }

    /**
     * 事件总线订阅者注册
     */
    @Bean
    @ConditionalOnMissingBean(ListenerRegistry.class)
    public ListenerRegistry listenerRegistry(ApplicationContext context, Environment environment, EventBusProperties eventBusProperties) {
        busConfig(environment, eventBusProperties);
        // Component
        Map<String, Object> beanMap = context.getBeansWithAnnotation(Component.class);
        ListenerRegistry registry = new ListenerRegistry(eventBusProperties);
        // 启动注册业务监听器 （注册中心）
        registry.register(beanMap.values());
        return registry;
    }

    /**
     * 事件总线配置
     */
    public void busConfig(Environment environment, EventBusProperties config) {
        log.info("Eventbus Initializing... {}", config.getType());
        // 自动获取env
        String env = config.getEnv();
        if(!StringUtils.hasLength(env)) {
            env = environment.getProperty("spring.profiles.active");
            if(StringUtils.hasLength(env)) {
                config.setEnv(env);
            }
        }
        // 自动获取服务名
        String serviceId = config.getServiceId();
        if (!StringUtils.hasLength(serviceId)) {
            serviceId = environment.getProperty("spring.application.name");
            if (null == serviceId || serviceId.isEmpty()) {
                serviceId = System.getProperties().getProperty("sun.java.command");
            }
        }
        config.setServiceId(serviceId);
    }

    /**
     * 消息总线分发器
     */
    @Bean
    @ConditionalOnBean(MsgSender.class)
    @ConditionalOnMissingBean(DeliveryEventBus.class)
    public DeliveryEventBus deliveryEventBus(InterceptorConfig interceptorConfig, GlobalConfig busConfig, MsgSender msgSender, ListenerRegistry registry) {
        return new DeliveryEventBus(interceptorConfig, busConfig, msgSender, registry);
    }

    /**
     * 连接监控
     */
    @Bean
    @ConditionalOnBean(NodeTestConnect.class)
    public ConnectionWatchdog connectionWatchdog(NodeTestConnect nodeTestConnect, GlobalConfig globalConfig, List<Lifecycle> listeners) {
        return new SpringBootConnectionWatchdog(nodeTestConnect, globalConfig.getTestConnect(), listeners);
    }
}
