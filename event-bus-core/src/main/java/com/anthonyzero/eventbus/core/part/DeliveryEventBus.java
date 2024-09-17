package com.anthonyzero.eventbus.core.part;

import com.anthonyzero.eventbus.core.api.MsgSender;
import com.anthonyzero.eventbus.core.api.annotation.Fail;
import com.anthonyzero.eventbus.core.api.annotation.Polling;
import com.anthonyzero.eventbus.core.config.GlobalConfig;
import com.anthonyzero.eventbus.core.config.InterceptorConfig;
import com.anthonyzero.eventbus.core.exception.EventBusException;
import com.anthonyzero.eventbus.core.metadata.Request;
import com.anthonyzero.eventbus.core.utils.CalculateUtil;
import com.anthonyzero.eventbus.core.utils.Func;
import com.anthonyzero.eventbus.core.support.FailTrigger;
import com.anthonyzero.eventbus.core.support.Listener;
import com.anthonyzero.eventbus.core.support.Trigger;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;

/**
 * 消息投递分发器  核心
 * @author : jin.ping
 * @date : 2024/9/4
 */
@Slf4j
public class DeliveryEventBus {

    private final InterceptorConfig interceptorConfig;
    private final GlobalConfig config;
    private final MsgSender msgSender;
    private final ListenerRegistry registry;

    public DeliveryEventBus(InterceptorConfig interceptorConfig,
                       GlobalConfig config,
                       MsgSender msgSender,
                       ListenerRegistry registry) {
        this.interceptorConfig = interceptorConfig;
        this.config = config;
        this.msgSender = msgSender;
        this.registry = registry;
    }

    /**
     * 发送及时消息给订阅者
     *
     * @param subscriber 订阅者
     * @param body       内容的主体
     */
    public void deliverTimely(Listener subscriber, byte[] body) {
        deliverTimely(subscriber, Func.convertByBytes(body));
    }

    /**
     * 发送及时消息给订阅者
     *
     * @param subscriber 订阅者
     * @param body       内容的主体
     */
    public void deliverTimely(Listener subscriber, String body) {
        deliverTimely(subscriber, Func.convertByJson(body));
    }

    /**
     * 发送及时消息给订阅者
     *
     * @param subscriber 订阅者
     * @param request    请求对象
     */
    public void deliverTimely(Listener subscriber, Request<?> request) {
        if (null != request.getDeliverId()
                && (!subscriber.getTrigger().getDeliverId().equals(request.getDeliverId()))) {
            //不满足
            return;
        }
        // 发送消息给订阅者
        deliver(subscriber, request);
    }

    /**
     * 接收延时消息
     *
     * @param body body
     */
    public void deliverDelay(String body) {
        deliverDelay(Func.convertByJson(body));
    }

    /**
     * 接收延时消息
     *
     * @param body body
     */
    public void deliverDelay(byte[] body) {
        deliverDelay(Func.convertByBytes(body));
    }

    /**
     * 接收延时消息
     * 所有类型code的延时消息都聚合在一起，所以需要发现具体的订阅者
     * @param body body
     */
    @SuppressWarnings("all")
    public void deliverDelay(Request request) {
        // 获取延时订阅者
        Listener subscriber = null;
        if (request.getType().isDelay()) {
            subscriber = registry.getDelayListener(request.getDeliverId());
        } else {
            //特殊情况 及时消息类型消费失败或者poll 的时候底层是通过发送延时消息实现
            subscriber = registry.getTimelyListener(request.getDeliverId());
        }
        // 如果订阅者为空，则打印错误日志并返回
        if (null == subscriber) {
            log.error("delay msg handler not found deliverId={}", request.getDeliverId());
            return;
        }
        // 交付消息给订阅者
        deliver(subscriber, request);
    }

    /**
     * 投递消息
     */
    private void deliver(Listener subscriber, Request<?> request) {
        Trigger trigger = subscriber.getTrigger(); //重点触发
        if (null == request.getDeliverId()) {
            //第一次及时消息投递的时候赋值，方便后面deliverDelay 重要
            request.setDeliverId(trigger.getDeliverId());
        }
        if (log.isDebugEnabled()) {
            log.debug("deliver msg：{}", Func.toJson(request));
        }
        try {
            trigger.invoke(request);
            // 轮询处理
            polling(subscriber, request);
            interceptorConfig.deliverSuccessExecute(request);
        } catch (Exception exception) {
            failHandle(subscriber, request, exception);
        }
    }

    /**
     * 失败处理
     *
     * @param subscriber subscriber
     * @param request    request
     * @param throwable  throwable
     */
    private void failHandle(Listener subscriber, Request<?> request, Throwable throwable) {
        if (!(throwable instanceof InvocationTargetException)) {
            throw new EventBusException(throwable.getCause());
        }
        // 获取异常的真正原因
        throwable = throwable.getCause();
        // 发生异常时记录错误日志
        log.error("deliver error", throwable);
        // 获取订阅器的FailTrigger
        FailTrigger failTrigger = subscriber.getFailTrigger();
        Fail fail = null;
        if (null != failTrigger) {
            // 如果FailTrigger不为空，则获取Fail对象
            fail = failTrigger.getFail();
        }

        // 每次投递消息异常时都会调用
        interceptorConfig.deliverThrowableEveryExecute(request, throwable);
        // 获取有效的投递次数
        int deliverCount = (null != fail && fail.retryCount() >= 0) ? fail.retryCount() : config.getFail().getRetryCount();
        if (request.getDeliverCount() <= deliverCount) {
            // 如果请求的投递次数小于等于有效的投递次数，则重新尝试投递
            failReTry(request, fail);
            return;
        }
        try {
            // 如果FailTrigger不为空，则执行订阅器的异常处理
            if (null != failTrigger && null != failTrigger.getMethod()) {
                failTrigger.invoke(request, throwable);
            }

            // 如果全局拦截器配置不为空且包含投递异常拦截器，则执行全局拦截器的异常处理
            interceptorConfig.deliverThrowableExecute(request, throwable);
        } catch (Exception var2) {
            // 捕获异常并记录错误日志
            log.error("deliveryBus.failHandle error", var2);
        }
    }

    /**
     * 失败重试
     *
     * @param request req
     * @param fail    fail
     */
    private void failReTry(Request<?> request, Fail fail) {
        // 获取下次投递失败时间
        long delayTime = (null != fail && fail.nextTime() > 0) ? fail.nextTime() : config.getFail().getNextTime();
        request.setDelayTime(delayTime);
        // 投递次数加一
        request.setDeliverCount(request.getDeliverCount() + 1);
        msgSender.sendDelayMessage(request);
    }

    /**
     * 轮询投递
     *
     * @param subscriber subscriber
     * @param request    req
     */
    private void polling(Listener subscriber, Request<?> request) {
        Polling polling = subscriber.getPolling();
        // 已轮询次数大于轮询次数，则不进行轮询投递
        // 是否已退出轮询
        boolean isOver = Polling.Keep.clear();
        if (null == polling
                || request.getDeliverCount() > polling.count()
                || isOver) {
            return;
        }

        Long delayTime = request.getDelayTime();
        Integer deliverCount = request.getDeliverCount();

        String interval = polling.interval();
        interval = interval.replace("$count", String.valueOf(deliverCount))
                .replace("$intervalTime", String.valueOf(null == delayTime ? 1 : delayTime));
        // 获取下次投递失败时间
        delayTime = CalculateUtil.fixEvalExpression(interval);
        request.setDelayTime(delayTime);
        // 投递次数加一
        request.setDeliverCount(request.getDeliverCount() + 1);
        msgSender.sendDelayMessage(request);
    }
}
