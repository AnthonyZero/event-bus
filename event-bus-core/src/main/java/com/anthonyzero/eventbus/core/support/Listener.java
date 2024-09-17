package com.anthonyzero.eventbus.core.support;

import com.anthonyzero.eventbus.core.api.annotation.Polling;
import com.anthonyzero.eventbus.core.config.GlobalConfig;
import com.anthonyzero.eventbus.core.metadata.MsgType;
import com.anthonyzero.eventbus.core.utils.Func;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 核心组件
 * @author : jin.ping
 * @date : 2024/9/3
 */
@Data
@NoArgsConstructor
public class Listener {

    /**
     * 消息所属来源服务ID,服务名
     */
    private String serviceId;
    /**
     * 消息编码
     */
    private String code;

    /**
     * 定义并发级别，默认{@link GlobalConfig#getConcurrency()}。
     */
    private int concurrency;
    /**
     * 消息类型
     */
    private MsgType type = MsgType.TIMELY;
    /**
     * 投递触发
     */
    private Trigger trigger;
    /**
     * 失败触发
     */
    private FailTrigger failTrigger;

    /**
     * 轮询配置
     */
    private Polling polling;

    public Listener(String serviceId, String code, int concurrency, MsgType type) {
        this.serviceId = serviceId;
        this.code = code;
        this.concurrency = concurrency;
        this.type = type;
    }

    public Listener(String serviceId, String code, int concurrency, MsgType type, Trigger trigger, FailTrigger failTrigger, Polling polling) {
        Polling.ValidatorInterval.isValid(null == polling ? null : polling.interval());
        this.serviceId = serviceId;
        this.code = code;
        this.concurrency = concurrency;
        this.type = type;
        this.trigger = trigger;
        this.failTrigger = failTrigger;
        this.polling = polling;
    }

    public static Listener ofDelay(GlobalConfig config) {
        return new Listener(config.getServiceId(), null, config.getDelayConcurrency(), MsgType.DELAY);
    }

    public String getTopic() {
        return Func.getTopic(serviceId, code);
    }
}
