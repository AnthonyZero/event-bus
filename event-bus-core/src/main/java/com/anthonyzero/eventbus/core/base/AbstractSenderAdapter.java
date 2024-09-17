package com.anthonyzero.eventbus.core.base;

import com.anthonyzero.eventbus.core.api.MsgSender;
import com.anthonyzero.eventbus.core.api.RequestIdGenerator;
import com.anthonyzero.eventbus.core.config.GlobalConfig;
import com.anthonyzero.eventbus.core.config.InterceptorConfig;
import com.anthonyzero.eventbus.core.metadata.MsgType;
import com.anthonyzero.eventbus.core.metadata.Request;
import com.anthonyzero.eventbus.core.part.ListenerRegistry;
import com.anthonyzero.eventbus.core.utils.Assert;
import com.anthonyzero.eventbus.core.utils.Func;
import com.anthonyzero.eventbus.core.support.Listener;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *  发送消息体包装处理类
 * @author : jin.ping
 * @date : 2024/9/4
 */
public abstract class AbstractSenderAdapter implements MsgSender {

    private final GlobalConfig config;
    private final InterceptorConfig interceptorConfig;
    private final RequestIdGenerator requestIdGenerator;
    /**
     * key=code  消息编码 => 延时监听器
     */
    private final Map<String, Listener> subscriberDelayMap;

    protected AbstractSenderAdapter(GlobalConfig config,
                                    InterceptorConfig interceptorConfig, RequestIdGenerator requestIdGenerator, ListenerRegistry registry) {
        this.config = config;
        this.interceptorConfig = interceptorConfig;
        this.requestIdGenerator = requestIdGenerator;
        this.subscriberDelayMap = registry.getDelayListeners().stream().filter(t -> !Func.isEmpty(t.getCode()))
                .collect(Collectors.toMap(Listener::getCode, Function.identity(), (x, y) -> x));
    }

    @Override
    public void send(Request<?> request) {
        request.setType(MsgType.TIMELY);
        checkBuild(request);
        Assert.isTrue(!Func.isEmpty(request.getCode()), "及时消息code不能为空");
        interceptorConfig.sendBeforeExecute(request);
        toSend(request);
        interceptorConfig.sendAfterExecute(request);
    }

    /**
     * 发送消息
     *
     * @param request req
     */
    public abstract void toSend(Request<?> request);

    @Override
    public void sendDelayMessage(Request<?> request) {
        request.setType(null == request.getType() ? MsgType.DELAY : request.getType());
        checkBuild(request);
        Assert.isTrue(null != request.getDelayTime() && request.getDelayTime() > 0, "延时时间不能小于0");
        //及时消息第二次会有投递ID
        // 投递ID
        if (Func.isEmpty(request.getDeliverId())) {
            //真正的业务使用的延时消息， 只能使用在同一个JVM下 延时消息
            Listener subscriber = subscriberDelayMap.get(request.getCode());
            Assert.notNull(subscriber, "延时消息code未找到对应订阅器！");
            request.setDeliverId(subscriber.getTrigger().getDeliverId()); //方便后面消费投递
        }
        interceptorConfig.sendBeforeExecute(request);
        toSendDelayMessage(request);
        interceptorConfig.sendAfterExecute(request);
    }

    /**
     * 发送延时消息
     *
     * @param request req
     */
    public abstract void toSendDelayMessage(Request<?> request);

    /**
     * 发送消息前置操作
     *
     * @param request req
     */
    protected void checkBuild(Request<?> request) {
        // 确保传入的对象不为空
        Objects.requireNonNull(request.getBody(), "消息体不能为空");

        // 设置服务ID为默认值，如果为空的话
        request.setServiceId(Func.isEmpty(request.getServiceId()) ? config.getServiceId() : request.getServiceId());

        // 设置请求ID为默认值，如果为空的话
        request.setRequestId(Func.isEmpty(request.getRequestId()) ? requestIdGenerator.nextId() : request.getRequestId());

        // 设置递送数量为默认值，如果为空的话
        request.setDeliverCount(request.getDeliverCount() != null ? request.getDeliverCount() : 1);
    }
}
