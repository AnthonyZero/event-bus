package com.anthonyzero.eventbus.provider;

import com.anthonyzero.eventbus.core.part.DeliveryEventBus;
import com.anthonyzero.eventbus.core.support.Listener;
import com.anthonyzero.eventbus.prop.EventBusProperties;
import com.anthonyzero.eventbus.provider.support.RedisListener;
import com.anthonyzero.eventbus.provider.support.AbstractStreamListenerContainer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Record;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

/**
 * 及时消息订阅器实现
 * @author : jin.ping
 * @date : 2024/9/5
 */
@Slf4j
public class RedisMsgSubscribeContainer extends AbstractStreamListenerContainer {

    private final List<RedisListener> subscribers;
    private final DeliveryEventBus deliveryEventBus;

    public RedisMsgSubscribeContainer(StringRedisTemplate stringRedisTemplate,
                                      EventBusProperties busProperties,
                                      List<Listener> subscribers,
                                      DeliveryEventBus deliveryEventBus) {
        super(stringRedisTemplate, busProperties);
        this.deliveryEventBus = deliveryEventBus;
        // subscribers (listener 原始 -> redisListener 包装)
        this.subscribers = RedisListener.redisListeners(subscribers, busProperties.getServiceId(), busProperties.getEnv());
    }

    @Override
    protected List<RedisListener> getListeners() {
        return this.subscribers;
    }

    @Override
    protected void deliver(RedisListener subscriber, Record<String, String> msg) {
        deliveryEventBus.deliverTimely(subscriber, msg.getValue());
    }
}
