package com.anthonyzero.eventbus.core.api;


import com.anthonyzero.eventbus.core.metadata.Message;
import com.anthonyzero.eventbus.core.config.GlobalConfig;

/**
 * 延时消息监听器
 * 注：只能订阅本服务{@link GlobalConfig#getServiceId()}下的延时消息
 *
 * @date 2024/01/01
 */
public interface MsgDelayListener<T> {

    /**
     * 消息类型，用于区分不同的消息类型。
     */
    String code();
    /**
     * 处理器
     *
     * @param message 消息体
     */
    void onMessage(Message<T> message);

}
