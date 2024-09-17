package com.anthonyzero.eventbus.core.metadata;

/**
 * 业务消息体
 * 发送消息时避免每次都要写code编码
 * <p>
 * 备注：实现类必须存在无参构造器
 *
 */
public interface MsgBody {

    /**
     * 消息体code
     *
     * @return code编码
     */
    default String code() {
        return this.getClass().getSimpleName();
    }
}
