package com.anthonyzero.eventbus.core.support;

import com.anthonyzero.eventbus.core.api.annotation.Fail;
import lombok.EqualsAndHashCode;
import lombok.Getter;



/**
 * @author : jin.ping
 * @date : 2024/9/3
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class FailTrigger extends Trigger{
    /**
     * eventbus投递失败配置信息
     */
    private final Fail fail;

    public FailTrigger(Fail fail, Trigger trigger) {
        super(trigger.getInvokeBean(), trigger.getMethod());
        this.fail = fail;
    }
}
