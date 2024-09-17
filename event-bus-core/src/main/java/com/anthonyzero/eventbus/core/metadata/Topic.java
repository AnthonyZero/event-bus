package com.anthonyzero.eventbus.core.metadata;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * topic
 *
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class Topic implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 消息所属来源服务ID,服务名
     */
    protected String serviceId;

    /**
     * 消息编码
     */
    protected String code;

    /**
     * 获取topic
     *
     * @return topic
     */
    public abstract String topic();
}
