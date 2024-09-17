package com.anthonyzero.eventbus.core.api;

import com.anthonyzero.eventbus.core.exception.EventBusException;
import com.anthonyzero.eventbus.core.metadata.Message;
import com.anthonyzero.eventbus.core.metadata.MsgBody;
import com.anthonyzero.eventbus.core.config.GlobalConfig;
import com.anthonyzero.eventbus.core.utils.Assert;
import com.anthonyzero.eventbus.core.utils.Func;
import lombok.Getter;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 及时消息订阅超类
 * @author : jin.ping
 * @date : 2024/9/4
 */
@Getter
public abstract class MsgListener<T> {
    /**
     * 消息所属来源服务ID,服务名
     */
    private final String serviceId;

    /**
     * 消息类型，用于区分不同的消息类型
     */
    private final List<String> codes;

    /**
     * 定义并发级别，默认{@link GlobalConfig#getConcurrency()}。
     */
    private final Integer concurrency;

    /**
     * 构造器
     */
    @SuppressWarnings("all")
    protected MsgListener() {
        this(null, new ArrayList<>(1), null);
        Type superclass = this.getClass().getGenericSuperclass();
        Class<?> beanClz = (Class<?>) ((ParameterizedType) superclass).getActualTypeArguments()[0];
        if (Func.isInterfaceImplemented(beanClz, MsgBody.class)) {
            try {
                Constructor<?> constructor = beanClz.getConstructor();
                if (!constructor.isAccessible()) {
                    constructor.setAccessible(true);
                }
                String code = ((MsgBody) beanClz.newInstance()).code();
                codes.add(code);
            } catch (Exception e) {
                throw new EventBusException(e);
            }
        }
        Assert.notEmpty(codes, this.getClass().getName() + "msg code is not null");
    }

    /**
     * 构造器
     *
     * @param code 消息编码
     */
    protected MsgListener(String code) {
        this(Collections.singletonList(code));
    }

    /**
     * 构造器
     *
     * @param code        消息编码
     * @param concurrency 并发级别
     */
    protected MsgListener(String code, Integer concurrency) {
        this(Collections.singletonList(code), concurrency);
    }

    /**
     * 构造器
     *
     * @param codes 消息编码
     */
    protected MsgListener(List<String> codes) {
        this(null, codes);
    }

    /**
     * 构造器
     *
     * @param codes       消息编码
     * @param concurrency 并发级别
     */
    protected MsgListener(List<String> codes, Integer concurrency) {
        this(null, codes, concurrency);
    }

    /**
     * 构造器
     *
     * @param serviceId 消息服务的ID
     * @param codes     消息编码
     */
    protected MsgListener(String serviceId, List<String> codes) {
        this(serviceId, codes, null);
    }

    /**
     * 构造器
     *
     * @param serviceId   消息服务的ID
     * @param codes       消息编码
     * @param concurrency 并发级别
     */
    protected MsgListener(String serviceId, List<String> codes, Integer concurrency) {
        this.serviceId = serviceId;
        this.codes = codes;
        this.concurrency = concurrency;
    }

    /**
     * 处理器
     *
     * @param message 消息体
     */
    public abstract void onMessage(Message<T> message);
}

