package com.anthonyzero.eventbus.provider.support;

import com.anthonyzero.eventbus.constant.RedisConstant;
import com.anthonyzero.eventbus.core.metadata.MsgType;
import com.anthonyzero.eventbus.core.support.Listener;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 *  redis消息订阅监听器消费者实体数据 (listener 的redis实现 包装)
 * @author : jin.ping
 * @date : 2024/9/5
 */
public class RedisListener extends Listener {

    /**
     * 消费者监听stream key
     */
    private final String streamKey;

    /**
     * 消费者所在消费者组
     */
    private final String group;

    public RedisListener(Listener subscriber, String subscribePrefix, String serviceId, String prefix) {
        super(subscriber.getServiceId(), subscriber.getCode(),
                subscriber.getConcurrency(), subscriber.getType(), subscriber.getTrigger(), subscriber.getFailTrigger(), subscriber.getPolling());
        this.streamKey = String.format(subscribePrefix, prefix, subscriber.getTopic());
        //this.group = null != subscriber.getTrigger() ? subscriber.getTrigger().getDeliverId() : subscriber.getServiceId();
        this.group = serviceId; //应用serviceId
    }

    public String getStreamKey() {
        return streamKey;
    }

    public String getGroup() {
        return group;
    }

    public static List<RedisListener> fullRedisSubscriber(List<Listener> listeners, String env, String serviceId) {
        //多个及时 消息定义器
        List<RedisListener> redisSubscribers = redisListeners(listeners, serviceId, env);

        // 延时的消息订阅
        redisSubscribers.addAll(redisDelaySubscriber(serviceId, env));
        return redisSubscribers;
    }

    public static List<RedisListener> redisListeners(List<Listener> listeners, String serviceId, String env) {
        return listeners.stream().map(t -> new RedisListener(t, RedisConstant.BUS_SUBSCRIBE_PREFIX, serviceId, env)).collect(Collectors.toList());
    }

    public static List<RedisListener> redisDelaySubscriber(String serviceId, String env) {
        return redisDelaySubscriber(serviceId, env,1);
    }

    public static List<RedisListener> redisDelaySubscriber(String serviceId, String env, int concurrency) {
        //所有的延时消息listen 底层都是由一个RedisListener驱动消息（通过延时消息订阅者注册表寻找）
        Listener listener = new Listener(serviceId, null, concurrency, MsgType.DELAY); //延时消息 监听的时候是本应用 serviceId
        return Collections.singletonList(new RedisListener(listener, RedisConstant.BUS_DELAY_SUBSCRIBE_PREFIX, serviceId, env)); //group = serviceId
    }
}
