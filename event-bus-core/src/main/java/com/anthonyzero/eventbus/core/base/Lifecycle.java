package com.anthonyzero.eventbus.core.base;

import com.anthonyzero.eventbus.core.exception.EventBusException;

/**
 * 中间件实现监听容器初始化接口
 * @author : jin.ping
 * @date : 2024/9/4
 */
public interface Lifecycle {

    /**
     * 监听组件注册
     *
     * @throws EventBusException e
     */
    void register() throws EventBusException;

    /**
     * 监听组件销毁
     *
     * @throws EventBusException e
     */
    void destroy() throws EventBusException;
}
