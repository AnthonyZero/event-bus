package com.anthonyzero.eventbus.provider.support;

import com.anthonyzero.eventbus.core.base.Lifecycle;
import com.anthonyzero.eventbus.core.constant.EventBusConstant;
import com.anthonyzero.eventbus.core.metadata.MsgType;
import com.anthonyzero.eventbus.core.utils.Func;
import com.anthonyzero.eventbus.core.utils.NamedThreadFactory;
import com.anthonyzero.eventbus.core.utils.PollThreadPoolExecutor;
import com.anthonyzero.eventbus.core.utils.WaitThreadPoolExecutor;
import com.anthonyzero.eventbus.prop.EventBusProperties;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author : jin.ping
 * @date : 2024/9/5
 */
@Slf4j
public abstract class AbstractStreamListenerContainer implements Lifecycle {

    protected final StringRedisTemplate redisTemplate;
    protected final EventBusProperties config;
    protected ThreadPoolExecutor pollExecutor;
    protected StreamMessageListenerContainer<String, ObjectRecord<String, String>> container;

    protected AbstractStreamListenerContainer(StringRedisTemplate redisTemplate, EventBusProperties config) {
        this.redisTemplate = redisTemplate;
        this.config = config;
    }

    @Override
    public void register() {
        if (null != container) {
            container.start();
            return;
        }
        //获取所有监听器 （及时和延时的实现）
        List<RedisListener> listeners = getListeners();
        if (Func.isEmpty(listeners)) {
            return;
        }
        boolean isBlock = config.getRedis().getPollBlock();
        ThreadPoolExecutor[] executors = createExecutor(listeners, isBlock);
        pollExecutor = executors[0];
        // 阻塞轮询或延时任务轮询间隔都设置为2000ms
        MsgType type = listeners.get(0).getType();
        long pollTimeout = isBlock || type.isDelay() ? 2000 : 5;
        // 创建配置对象
        var options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                .executor(pollExecutor)
                // 一次性最多拉取多少条消息
                .batchSize(config.getMsgBatchSize())
                // 消息消费异常的handler
                .errorHandler(t -> log.error("[Eventbus error] ", t))
                // 超时时间，设置为0，表示不超时（超时后会抛出异常）
                .pollTimeout(Duration.ofMillis(pollTimeout))
                // 序列化器
                .serializer(new StringRedisSerializer())
                .targetType(String.class)
                .build();
        // 根据配置对象创建监听容器对象
        RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
        Assert.notNull(connectionFactory, "RedisConnectionFactory must not be null!");
        container = isBlock ? StreamMessageListenerContainer.create(connectionFactory, options)
                : new XDefaultStreamMessageListenerContainer<>(connectionFactory, options, executors[1]);
        // 添加消费者
        createConsumer(container, listeners);
        // 启动监听
        container.start();
    }

    /**
     * 创建线程池
     *
     * @param listeners listeners
     * @return 线程池
     */
    private ThreadPoolExecutor[] createExecutor(List<RedisListener> listeners, boolean isBlock) {
        NamedThreadFactory factory = new NamedThreadFactory(this.getClass().getSimpleName() + "-");
        // 创建线程池
        int concurrency = listeners.stream().map(RedisListener::getConcurrency).reduce(Integer::sum).orElse(1);
        // 根据配置创建不同的线程池
        if (isBlock) {
            ThreadPoolExecutor executor = new ThreadPoolExecutor(concurrency, concurrency, 1, TimeUnit.MINUTES, new LinkedBlockingDeque<>(), factory);
            return new ThreadPoolExecutor[]{executor};
        } else {
            //默认非阻塞
            EventBusProperties.RedisProperties redis = config.getRedis();
            int poolSize = Math.min(concurrency, redis.getPollThreadPoolSize()); //min最小值

            // 拉取消息的线程池
            ThreadPoolExecutor executor = new PollThreadPoolExecutor(1, 1, 1,
                    TimeUnit.MINUTES, new LinkedBlockingDeque<>(concurrency), factory);

            // 分发消息的工作线程池
            ThreadPoolExecutor excExecutor = new WaitThreadPoolExecutor(1, poolSize, redis.getPollThreadKeepAliveTime(),
                    TimeUnit.SECONDS, new LinkedBlockingDeque<>(concurrency), new NamedThreadFactory(this.getClass().getSimpleName() + ".exc-"));
            return new ThreadPoolExecutor[]{executor, excExecutor};
        }
    }

    /**
     * 注册消费者
     *
     * @param container 监听容器
     * @param listeners listeners
     */
    private void createConsumer(StreamMessageListenerContainer<String, ObjectRecord<String, String>> container, List<RedisListener> listeners) {
        String hostAddress = Func.getHostAddress();
        // 初始化组
        createGroup(listeners);
        for (RedisListener listener : listeners) {
            Func.pollRun(listener.getConcurrency(), () ->
                    container.receive(
                            Consumer.from(listener.getGroup(), hostAddress),
                            StreamOffset.create(listener.getStreamKey(), ReadOffset.lastConsumed()),
                            // 使用监听容器对象开始监听消费（使用的是手动确认方式）
                            msg -> deliverMsg(listener, msg)));
        }
    }

    /**
     * 消费消息
     *
     * @param listener listeners
     * @param msg      msg
     */
    private void deliverMsg(RedisListener listener, Record<String, String> msg) {
        String oldName = Func.reThreadName(EventBusConstant.THREAD_NAME);
        try {
            //正式开始投递入口
            deliver(listener, msg); //(业务里会try failHandle)
            //ack (pel) 消息就会从消费组的PEL中移除。
            redisTemplate.opsForStream().acknowledge(listener.getStreamKey(), listener.getGroup(), msg.getId());
            //如果系统异常没有ack, RedisPendingMsgResendTask会拿出消费组里的PEL数据重新send一次且ACK 可能性：eventbus错误或者系统到这执行error
        } catch (Exception e) {
            log.error("[Eventbus error] ", e);
        } finally {
            // 恢复线程名称
            Thread.currentThread().setName(oldName);
        }
    }

    /**
     * 获取消费者
     *
     * @return 消费者
     */
    protected abstract List<RedisListener> getListeners();

    /**
     * 消费消息
     *
     * @param subscriber 消费者
     * @param msg        消息体
     */
    protected abstract void deliver(RedisListener subscriber, Record<String, String> msg);

    @Override
    public void destroy() {
        if (null != container) {
            container.stop();
        }

        // 轮询线程关闭
        if (pollExecutor instanceof PollThreadPoolExecutor) {
            ((PollThreadPoolExecutor) pollExecutor).terminated();
        }
        pollExecutor.purge();
    }

    /**
     * 创建消费者组。
     * 遍历给定的订阅者列表，为每个订阅者所指定的流创建消费者组，如果该消费者组还未在对应的流上存在的话。
     *
     * @param listeners 订阅者列表，每个订阅者包含流的键和消费者组的名称。
     */
    private void createGroup(List<RedisListener> listeners) {
        // 根据流的键对订阅者进行分组
        listeners.stream().collect(Collectors.groupingBy(RedisListener::getStreamKey)).forEach((streamKey, subs) -> {
            // 检查流的键是否在Redis中存在
            if (Boolean.TRUE.equals(redisTemplate.hasKey(streamKey))) {
                // 获取当前流上已存在的消费者组信息
                StreamInfo.XInfoGroups groups = redisTemplate.opsForStream().groups(streamKey);
                // 从订阅者列表中移除那些组名已经存在于流上的订阅者
                subs = subs.stream().filter(t -> {
                    long count = groups.stream().filter(g -> g.groupName().equals(t.getGroup())).count();
                    return count <= 0;
                }).collect(Collectors.toList());
            }
            // 为剩余的订阅者（即组名在流上不存在的订阅者）创建新的消费者组
            if (!subs.isEmpty()) {
                subs.forEach(t -> {
                    try {
                        //group serviceId
                        redisTemplate.opsForStream().createGroup(t.getStreamKey(), t.getGroup()); //mkStream = true
                    } catch (Exception e) {
                        if (!e.getMessage().contains("Group name already exists")) {
                            throw e;
                        }
                    }
                });
            }
        });
    }
}
