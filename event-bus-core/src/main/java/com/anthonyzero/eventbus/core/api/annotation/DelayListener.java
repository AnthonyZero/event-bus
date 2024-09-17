package com.anthonyzero.eventbus.core.api.annotation;

import com.anthonyzero.eventbus.core.config.GlobalConfig;

import java.lang.annotation.*;

/**
 * 延时消息订阅注解
 * 注：只能订阅本服务 同一个JVM{@link GlobalConfig#getServiceId()}下的延时消息
 * @author : jin.ping
 * @date : 2024/9/4
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DelayListener {

    /**
     * 消息类型，用于区分不同的消息类型。
     */
    String[] codes();

    /**
     * 消息投递失败异常处理注解
     */
    Fail fail() default @Fail();
}
