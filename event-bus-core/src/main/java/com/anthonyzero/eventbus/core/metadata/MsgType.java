package com.anthonyzero.eventbus.core.metadata;

import lombok.Getter;

/**
 * 消息类型
 *
 **/
@Getter
public enum MsgType {
    /**
     * 及时消息
     */
    TIMELY(1) {
        @Override
        public boolean isTimely() {
            return true;
        }
    },

    /**
     * 延迟消息
     */
    DELAY(2) {
        @Override
        public boolean isDelay() {
            return true;
        }
    };

    /**
     * 消息类型的值
     */
    private final Integer value;

    /**
     * 构造函数
     *
     * @param value 消息类型的值
     */
    MsgType(Integer value) {
        this.value = value;
    }

    /**
     * 判断消息类型是否为及时消息
     *
     * @return true代表是及时消息，false代表不是及时消息
     */
    public boolean isTimely() {
        return false;
    }

    /**
     * 判断消息类型是否为延迟消息
     *
     * @return true代表是延迟消息，false代表不是延迟消息
     */
    public boolean isDelay() {
        return false;
    }

    /**
     * 根据消息类型的值获取消息类型
     *
     * @param value 消息类型的值
     * @return 消息类型
     */
    public static MsgType of(Integer value) {
        for (MsgType msgType : MsgType.values()) {
            if (msgType.value.equals(value)) {
                return msgType;
            }
        }
        return null;
    }
}
