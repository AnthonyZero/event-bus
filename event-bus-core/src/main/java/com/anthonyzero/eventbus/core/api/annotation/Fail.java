package com.anthonyzero.eventbus.core.api.annotation;

import com.anthonyzero.eventbus.core.metadata.Message;
import com.anthonyzero.eventbus.core.config.GlobalConfig;

import java.lang.annotation.*;

/**
 * tips: 消息投递失败异常处理注解，
 * 使用在消息重试投递最终失败时进行回调。必须和订阅器在同一个类中
 * @author : jin.ping
 * @date : 2024/9/3
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Fail {

    /**
     * 失败回调方法，回调方法必须和订阅器处理器在同一个类中,最后一次重试投递仍然抛出异常时会调用此方法
     * <p>回调方法可选参数（无序）：</p>
     * 1：消息体{@link Message}
     * 1：重复投递失败异常，为原始异常{@link java.lang.Throwable}
     */
    String callMethod() default "";

    /**
     * 消息投递失败时，一定时间内再次进行投递的次数
     * <code>retryCount < 0</code> 时根据全局配置{@link GlobalConfig.Fail#getRetryCount()} 默认为3次
     */
    int retryCount() default -1;

    /**
     * 投递失败时，下次下次投递触发的间隔时间,单位：秒
     * <code>nextTime <= 0</code>时根据全局配置{@link GlobalConfig.Fail#getNextTime()} 默认为10秒
     */
    long nextTime() default -1L;

}
