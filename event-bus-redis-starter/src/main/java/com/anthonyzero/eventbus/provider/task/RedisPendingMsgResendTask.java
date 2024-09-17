package com.anthonyzero.eventbus.provider.task;

import com.anthonyzero.eventbus.prop.EventBusProperties;
import com.anthonyzero.eventbus.provider.support.RedisListener;
import com.anthonyzero.eventbus.constant.RedisConstant;
import com.anthonyzero.eventbus.core.base.Lifecycle;
import com.anthonyzero.eventbus.core.metadata.Request;
import com.anthonyzero.eventbus.core.part.TaskRegistry;
import com.anthonyzero.eventbus.core.support.Listener;
import com.anthonyzero.eventbus.core.support.task.CronTask;
import com.anthonyzero.eventbus.core.utils.Func;
import com.anthonyzero.eventbus.provider.RLock;
import com.anthonyzero.eventbus.provider.RedisMsgSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.UUID;

/**
 * 重新发送超时待确认消息任务 (异常未ACK的情况)
 *  PEL(正在处理的消息)
 * @author : jin.ping
 * @date : 2024/9/5
 */
@Slf4j
public class RedisPendingMsgResendTask implements Runnable, Lifecycle {

    private static final long POLLING_INTERVAL = 35L;
    private static final String CRON = POLLING_INTERVAL + " * * * * ?";
    private final EventBusProperties eventBusProperties;
    private final RLock rLock;
    private final RedisMsgSender msgSender;
    private final StringRedisTemplate stringRedisTemplate;
    /**
     * 延时消息流key
     */
    private final String delayStreamKey;
    private final List<RedisListener> redisSubscribers;
    private CronTask task;
    private final TaskRegistry taskRegistry;

    public RedisPendingMsgResendTask(StringRedisTemplate stringRedisTemplate, TaskRegistry taskRegistry,
                                     EventBusProperties eventBusProperties, List<Listener> subscribers,
                                     RLock rLock,
                                     RedisMsgSender msgSender) {
        // 一分钟执行一次,这里选择每分钟的35秒执行，是为了避免整点任务过多的问题
        this.stringRedisTemplate = stringRedisTemplate;
        this.taskRegistry = taskRegistry;
        this.eventBusProperties = eventBusProperties;
        this.rLock = rLock;
        this.msgSender = msgSender;
        this.delayStreamKey = String.format(RedisConstant.BUS_DELAY_SUBSCRIBE_PREFIX, eventBusProperties.getEnv(), eventBusProperties.getServiceId());
        // 全部消息订阅者
        this.redisSubscribers = RedisListener.fullRedisSubscriber(subscribers, eventBusProperties.getEnv(), eventBusProperties.getServiceId());
    }

    @Override
    public void register() {
        task = CronTask.create(this.getClass().getName(), CRON, this);
        taskRegistry.createTask(task);
    }

    @Override
    public void run() {
        this.redisSubscribers.forEach(subscriber -> {
            String lockKey = subscriber.getStreamKey() + ".pendingMsgResendLock." + subscriber.getGroup();
            String request = UUID.randomUUID().toString();
            // 获取锁,并锁定一定间隔时长，此处故意不释放锁，防止重复执行
            boolean lock = false;
            try {
                lock = rLock.getLock(lockKey, request,POLLING_INTERVAL * 2);
                if (!lock) {
                    return;
                }
                pendingMessagesResendExecute(subscriber);
            } catch (Exception e) {
                log.error("pending消息重发异常", e);
            } finally {
                if (lock) {
                    try {
                        rLock.releaseLock(lockKey, request);
                    } catch (Exception e) {
                        log.error("pending.releaseLock", e);
                    }
                }
            }
        });
    }

    /**
     * 重新发送pending消息
     *
     * @param subscriber 消费者
     */
    private void pendingMessagesResendExecute(RedisListener subscriber) {
        StreamOperations<String, String, String> sops = stringRedisTemplate.opsForStream();
        // 获取my_group中的pending消息信息
        PendingMessagesSummary stats;
        try {
            //每个消费者组下面 消费者情况
            stats = sops.pending(subscriber.getStreamKey(), subscriber.getGroup()); //指定group
        } catch (RedisSystemException e) {
            if (("" + e.getMessage()).contains("No such key")) {
                return;
            }
            log.error(e.getMessage());
            return;
        }
        if (null == stats) {
            return;
        }
        // 所有pending消息的数量
        long totalMsgNum = stats.getTotalPendingMessages();
        if (totalMsgNum <= 0) {
            return;
        }

        log.debug("消费组：{}，一共有{}条pending消息...", subscriber.getGroup(), totalMsgNum);
        // 每个消费者的pending消息数量
        stats.getPendingMessagesPerConsumer().forEach((consumerName, msgCount) -> {
            if (msgCount <= 0) {
                return;
            }
            //每个消费组情况
            log.debug("消费者：{}，一共有{}条pending消息", consumerName, msgCount);
            int pendingMessagesBatchSize = eventBusProperties.getRedis().getPendingMessagesBatchSize();
            do {
                // 读取消费者pending队列的前N条记录，从ID=0的记录开始，一直到ID最大值
                PendingMessages pendingMessages = sops.pending(subscriber.getStreamKey(),
                        Consumer.from(subscriber.getGroup(), consumerName), Range.unbounded(), pendingMessagesBatchSize);
                if (pendingMessages.isEmpty()) {
                    return;
                }
                pushMessage(subscriber, pendingMessages);
            } while ((msgCount = msgCount - pendingMessagesBatchSize) > 0);
        });
    }

    /**
     * 从pending队列中读取消息(PEL队列)
     */
    public void pushMessage(RedisListener subscriber, PendingMessages pendingMessages) {
        // 遍历所有pending消息的详情
        pendingMessages.get().parallel().forEach(message -> {
            // 消息的ID
            String recordId = message.getId().getValue();
            // 未达到订阅消息投递超时时间 不做处理
            long lastDelivery = message.getElapsedTimeSinceLastDelivery().getSeconds();
            if (lastDelivery < eventBusProperties.getRedis().getDeliverTimeout()) {
                return;
            }
            // 通过streamOperations，直接读取这条pending消息，
            List<ObjectRecord<String, String>> result = stringRedisTemplate
                    .opsForStream().range(String.class, subscriber.getStreamKey(), Range.closed(recordId, recordId));
            if (CollectionUtils.isEmpty(result)) {
                acknowledge(subscriber, message.getId());
                return;
            }
            Request<?> request = Func.convertByJson(result.get(0).getValue());
            request.setDeliverCount(request.getDeliverCount() + 1);
            // 重新投递消息
            if (subscriber.getType().isTimely()) {
                request.setDeliverId(subscriber.getTrigger().getDeliverId());
                msgSender.toSend(request);
            } else {
                msgSender.toSend(delayStreamKey, request);
            }
            acknowledge(subscriber, message.getId());
        });
    }

    /**
     * 确认消费
     *
     * @param subscriber 消费者
     * @param recordId   消息ID
     */
    private void acknowledge(RedisListener subscriber, RecordId recordId) {
        stringRedisTemplate.opsForStream().acknowledge(subscriber.getStreamKey(), subscriber.getGroup(), recordId);
    }

    @Override
    public void destroy() {
        taskRegistry.removeTask(task);
    }
}
