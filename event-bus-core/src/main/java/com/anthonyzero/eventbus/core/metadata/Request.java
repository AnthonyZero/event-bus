package com.anthonyzero.eventbus.core.metadata;

import com.anthonyzero.eventbus.core.api.MsgDelayListener;
import com.anthonyzero.eventbus.core.api.RequestIdGenerator;
import com.anthonyzero.eventbus.core.constant.EventBusConstant;
import com.anthonyzero.eventbus.core.utils.Func;
import lombok.*;

/**
 * 通知消息体，消息总线原始消息体
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("all")
@EqualsAndHashCode(callSuper = true)
public class Request<T> extends Topic implements Message<T> {
    private static final long serialVersionUID = 1L;
    /**
     * 事件ID,默认UUID
     * <p>
     * 如需修改请实现此接口{@link RequestIdGenerator)}
     */
    private String requestId;

    /**
     * 消息接收处理器（消费者/投递）ID=类完全限定名+方法名{@link Trigger#getDeliverId()}
     */
    private String deliverId;

    /**
     * 消息投递次数 (记录消息的投递次数, 投递失败的时候跟 @Fail 的配置次数结合进行判断)
     */
    private Integer deliverCount;

    /**
     * 消息类型,默认及时消息
     */
    private MsgType type;

    /**
     * 延时消息的延时时间，单位：秒
     */
    private Long delayTime;

    /**
     * 业务消息体
     * 注：必须包含无参构造函数
     */
    private T body;

    @Builder
    public Request(String serviceId,
                   String code,
                   String requestId,
                   String deliverId,
                   Class<? extends MsgDelayListener> delayListener,
                   Integer deliverCount,
                   MsgType type,
                   Long delayTime,
                   T body) {
        super(serviceId, code);
        this.requestId = requestId;
        this.deliverId = deliverId;
        if (null != delayListener) {
            this.deliverId = Func.getDeliverId(delayListener, EventBusConstant.ON_MESSAGE);
        }
        this.deliverCount = deliverCount;
        this.type = type;
        this.delayTime = delayTime;
        this.body = body;
    }

    @Override
    public String getRequestId() {
        return requestId;
    }

    @Override
    public Integer getDeliverCount() {
        return null == this.deliverCount ? 1 : this.deliverCount;
    }

    @Override
    public T getBody() {
        return this.body;
    }

    @Override
    public String topic() {
        return Func.getTopic(serviceId, code);
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setDeliverCount(Integer deliverCount) {
        this.deliverCount = deliverCount;
    }

    public void setBody(T body) {
        this.body = body;
    }
}
