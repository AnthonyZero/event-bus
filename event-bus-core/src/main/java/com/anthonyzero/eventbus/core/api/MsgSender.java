package com.anthonyzero.eventbus.core.api;

import com.anthonyzero.eventbus.core.metadata.MsgBody;
import com.anthonyzero.eventbus.core.metadata.Request;

/**
 *  消息生产者（由底层实现）
 * @author : jin.ping
 * @date : 2024/9/4
 */
public interface MsgSender {

    /**
     * 通知发送接口
     * serviceId默认为本服务ID
     *
     * @param body 消息体
     */
    default void send(MsgBody body) {
        send(null, body.code(), body);
    }

    /**
     * 通知发送接口
     * serviceId默认为本服务ID
     *
     * @param code 业务消息类型
     * @param body 消息体
     */
    default void send(String code, Object body) {
        send(null, code, body);
    }

    /**
     * 通知发送接口
     *
     * @param serviceId 服务ID
     * @param body      消息体
     */
    default void send(String serviceId, MsgBody body) {
        send(serviceId, body.code(), body);
    }

    /**
     * 通知发送接口
     *
     * @param serviceId 服务ID
     * @param code      业务消息类型
     * @param body      消息体
     */
    default void send(String serviceId, String code, Object body) {
        send(Request.builder().serviceId(serviceId).code(code).body(body).build());
    }

    /**
     * 通知发送接口
     *
     * @param request request
     */
    void send(Request<?> request);

    /**
     * 发送延时消息接口 注意class<?>
     *
     * @param listener  延时处理器
     * @param body      延时消息实体
     * @param delayTime 延时时间，单位：秒
     */
    @SuppressWarnings("all")
    default void sendDelayMessage(Class<? extends MsgDelayListener> listener, MsgBody body, long delayTime) {
        sendDelayMessage(listener, body.code(), body, delayTime);
    }

    /**
     * 发送延时消息接口
     *
     * @param listener  延时处理器
     * @param code      延时消息类型
     * @param body      延时消息实体
     * @param delayTime 延时时间，单位：秒
     */
    @SuppressWarnings("all")
    default void sendDelayMessage(Class<? extends MsgDelayListener> listener, String code, Object body, long delayTime) {
        sendDelayMessage(Request.builder().delayListener(listener).code(code).body(body).delayTime(delayTime).build());
    }

    /**
     * 发送延时消息接口
     *
     * @param code      延时消息类型
     * @param body      延时消息实体
     * @param delayTime 延时时间，单位：秒
     */
    @SuppressWarnings("all")
    default void sendDelayMessage(MsgBody body, long delayTime) {
        sendDelayMessage(body.code(), body, delayTime);
    }

    /**
     * 发送延时消息接口
     *
     * @param code      延时消息类型
     * @param body      延时消息实体
     * @param delayTime 延时时间，单位：秒
     */
    @SuppressWarnings("all")
    default void sendDelayMessage(String code, Object body, long delayTime) {
        sendDelayMessage(null, code, body, delayTime);
    }

    /**
     * 发送延时消息接口
     *
     * @param request request
     */
    void sendDelayMessage(Request<?> request);
}
