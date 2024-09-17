package com.anthonyzero.eventbus.provider;

import com.anthonyzero.eventbus.constant.RedisConstant;
import com.anthonyzero.eventbus.core.api.RequestIdGenerator;
import com.anthonyzero.eventbus.core.base.AbstractSenderAdapter;
import com.anthonyzero.eventbus.core.config.GlobalConfig;
import com.anthonyzero.eventbus.core.config.InterceptorConfig;
import com.anthonyzero.eventbus.core.metadata.Request;
import com.anthonyzero.eventbus.core.part.ListenerRegistry;
import com.anthonyzero.eventbus.core.part.TaskRegistry;
import com.anthonyzero.eventbus.core.support.task.Task;
import com.anthonyzero.eventbus.core.utils.Func;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Record;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;

/**
 * redis消息生产者实现
 * @author : jin.ping
 * @date : 2024/9/4
 */
@Slf4j
public class RedisMsgSender extends AbstractSenderAdapter {

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> zsetAddRedisScript;
    private final TaskRegistry taskRegistry;
    private final GlobalConfig config;

    public RedisMsgSender(StringRedisTemplate stringRedisTemplate,
                          GlobalConfig config,
                          InterceptorConfig interceptorConfig,
                          DefaultRedisScript<Long> zsetAddRedisScript,
                          TaskRegistry taskRegistry, RequestIdGenerator requestIdGenerator, ListenerRegistry registry) {
        super(config, interceptorConfig, requestIdGenerator, registry);
        this.stringRedisTemplate = stringRedisTemplate;
        this.zsetAddRedisScript = zsetAddRedisScript;
        this.taskRegistry = taskRegistry;
        this.config = config;
    }


    @Override
    public void toSend(Request<?> request) {
        toSend(String.format(RedisConstant.BUS_SUBSCRIBE_PREFIX, config.getEnv(), request.topic()), request);
    }

    public void toSend(String streamKey, Request<?> request) {
        stringRedisTemplate.opsForStream().add(Record.of(Func.toJson(request)).withStreamKey(streamKey));
    }

    @Override
    public void toSendDelayMessage(Request<?> request) {
        // 计算延迟时间
        Long timeMillis = System.currentTimeMillis() + (1000L * request.getDelayTime());
        timeMillis = stringRedisTemplate.execute(zsetAddRedisScript,
                Collections.singletonList(String.format(RedisConstant.BUS_DELAY_PREFIX, config.getEnv(), config.getServiceId())),  //当前服务下的zset
                // 到当前时间之前的消息 + 推送数量
                String.valueOf(timeMillis), Func.toJson(request));
        // 重置延迟任务
        setNextTriggerTimeMillis(timeMillis);
    }

    /**
     * 重置轮询时间
     */
    public void setNextTriggerTimeMillis(Long timeMillis) {
        if (null == timeMillis) {
            return;
        }
        //RedisMsgDelayContainer 启动后register task
        Task task = taskRegistry.getTask(RedisMsgDelayContainer.class.getName());
        if (null != task) {
            task.refreshNextExecutionTime(timeMillis);
        }
    }
}
