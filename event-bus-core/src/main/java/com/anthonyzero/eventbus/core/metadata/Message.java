package com.anthonyzero.eventbus.core.metadata;

/**
 * 通知消息体
 *
 */
public interface Message<T> {
    /**
     * 获取消息ID
     *
     * @return 消息ID
     */
    String getRequestId();

    /**
     * 消息所属来源服务ID,服务名
     *
     * @return 应用服务ID
     */
    String getServiceId();

    /**
     * 消息类型，用于区分不同的消息类型
     *
     * @return 消息类型
     */
    String getCode();

    /**
     * 获取消息投递次数
     *
     * @return 消息投递次数
     */
    Integer getDeliverCount();

    /**
     * 获取消息体
     *
     * @return 消息体
     */
    T getBody();
}
