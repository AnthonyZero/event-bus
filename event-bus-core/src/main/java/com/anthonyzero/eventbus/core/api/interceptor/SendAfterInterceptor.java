package com.anthonyzero.eventbus.core.api.interceptor;

import com.anthonyzero.eventbus.core.metadata.Request;

/**
 * 发送后，全局拦截器
 * @author : jin.ping
 * @date : 2024/9/4
 */
public interface SendAfterInterceptor {

    /**
     * 拦截器执行
     *
     * @param request request
     */
    void execute(Request<String> request);
}
