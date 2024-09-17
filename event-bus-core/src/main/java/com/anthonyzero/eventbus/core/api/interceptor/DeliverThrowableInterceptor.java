package com.anthonyzero.eventbus.core.api.interceptor;

import com.anthonyzero.eventbus.core.metadata.Request;

/**
 * 投递异常，全局拦截器
 * 注：消息重试投递都失败时，最后一次消息投递失败时会调用该拦截器
 * @author : jin.ping
 * @date : 2024/9/4
 */
public interface DeliverThrowableInterceptor {

    /**
     * 拦截器执行
     *
     * @param request   request
     * @param throwable t
     */
    void execute(Request<String> request, Throwable throwable);
}
