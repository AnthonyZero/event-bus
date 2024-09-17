package com.anthonyzero.eventbus.core.api.annotation;

import com.anthonyzero.eventbus.core.config.GlobalConfig;

import java.lang.annotation.*;

/**
 * 及时消息订阅注解
 * @author : jin.ping
 * @date : 2024/9/4
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Listener {

    /**
     * 消息所属来源服务ID或服务名。默认订阅本服务{@link GlobalConfig#getServiceId()}配置的ID
     *
     * @see GlobalConfig#getServiceId()
     */
    String serviceId() default "";

    /**
     * 消息类型，用于区分不同的消息类型。
     */
    String[] codes();

    /**
     * 定义并发级别，默认值为-1。
     *
     * @return 返回并发级别的整数值。设置-1表示未设置，默认{@link GlobalConfig#getConcurrency()}。
     */
    int concurrency() default -1;

    /**
     * 消息投递失败异常处理注解
     */
    Fail fail() default @Fail();
}
