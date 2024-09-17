package com.anthonyzero.eventbus.provider;

import com.anthonyzero.eventbus.constant.RedisConstant;
import com.anthonyzero.eventbus.prop.EventBusProperties;
import com.anthonyzero.eventbus.provider.support.RedisListener;
import com.anthonyzero.eventbus.core.part.DeliveryEventBus;
import com.anthonyzero.eventbus.core.part.TaskRegistry;
import com.anthonyzero.eventbus.core.support.task.PeriodTask;
import com.anthonyzero.eventbus.provider.support.AbstractStreamListenerContainer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Record;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author : jin.ping
 * @date : 2024/9/5
 */
@Slf4j
public class RedisMsgDelayContainer extends AbstractStreamListenerContainer {

    /**
     * 最大轮询时间间隔，单位：毫秒
     */
    private static final long POLL_MILLIS = 1000L * 5;
    /**
     * 最大消息推送数量，默认10万条
     */
    private static final long MAX_PUSH_COUNT = 10000L * 10;

    private final StringRedisTemplate stringRedisTemplate;
    private final RLock rLock;
    private final DeliveryEventBus deliveryEventBus;
    private final DefaultRedisScript<Long> pushMsgStreamRedisScript;
    /**
     * 延时消息key,zset
     */
    private final String delayZetKey;
    /**
     * 轮询锁
     */
    private final String pollLockKey;
    /**
     * 延时消息流key
     */
    private final String delayStreamKey;
    private final TaskRegistry taskRegistry;
    private PeriodTask task;

    public RedisMsgDelayContainer(StringRedisTemplate stringRedisTemplate,
                                  TaskRegistry taskRegistry,
                                  EventBusProperties busProperties,
                                  DefaultRedisScript<Long> pushMsgStreamRedisScript, RLock rLock, DeliveryEventBus deliveryEventBus) {
        super(stringRedisTemplate, busProperties);
        this.stringRedisTemplate = stringRedisTemplate;
        this.taskRegistry = taskRegistry;
        this.pushMsgStreamRedisScript = pushMsgStreamRedisScript;
        this.rLock = rLock;
        this.deliveryEventBus = deliveryEventBus;
        this.delayZetKey = String.format(RedisConstant.BUS_DELAY_PREFIX, busProperties.getEnv(), busProperties.getServiceId());
        this.pollLockKey = String.format(RedisConstant.BUS_DELAY_LOCK_PREFIX, busProperties.getEnv(), busProperties.getServiceId());
        this.delayStreamKey = String.format(RedisConstant.BUS_DELAY_SUBSCRIBE_PREFIX, busProperties.getEnv(), busProperties.getServiceId());
    }

    @Override
    protected List<RedisListener> getListeners() {
        return RedisListener.redisDelaySubscriber(config.getServiceId(), config.getEnv(), config.getDelayConcurrency());
    }

    @Override
    protected void deliver(RedisListener subscriber, Record<String, String> msg) {
        deliveryEventBus.deliverDelay(msg.getValue());
    }

    @Override
    public synchronized void register() {
        super.register();
        this.task = PeriodTask.create(this.getClass().getName(), POLL_MILLIS, this::pollTask);
        taskRegistry.createTask(this.task);
    }

    /**
     * 循环获取延时队列到期消息 到stream
     */
    private void pollTask() {
        boolean lock = false;
        String request = UUID.randomUUID() + ":" + Thread.currentThread().getId();
        try {
            //多节点竞争
            lock = rLock.getLock(pollLockKey, request);
            if (!lock) {
                return;
            }
            Long nextCurrentTimeMillis = stringRedisTemplate.execute(pushMsgStreamRedisScript,
                    Arrays.asList(delayZetKey, delayStreamKey),
                    // 到当前时间之前的消息 + 推送数量
                    String.valueOf(System.currentTimeMillis()), String.valueOf(MAX_PUSH_COUNT));
            if (null != nextCurrentTimeMillis) {
                setNextTriggerTimeMillis(nextCurrentTimeMillis);
            }
        } finally {
            if (lock) {
                rLock.releaseLock(pollLockKey, request);
            }
        }
    }

    /**
     * 重置轮询时间
     */
    public void setNextTriggerTimeMillis(long timeMillis) {
        this.task.refreshNextExecutionTime(timeMillis);
    }

    @Override
    public void destroy() {
        super.destroy();
        taskRegistry.removeTask(task);
    }
}
