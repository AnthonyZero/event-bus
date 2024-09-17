package com.anthonyzero.eventbus.provider.task;

import com.anthonyzero.eventbus.core.base.Lifecycle;
import com.anthonyzero.eventbus.core.part.TaskRegistry;
import com.anthonyzero.eventbus.core.support.Listener;
import com.anthonyzero.eventbus.core.support.task.CronTask;
import com.anthonyzero.eventbus.prop.EventBusProperties;
import com.anthonyzero.eventbus.provider.support.RedisListener;
import com.anthonyzero.eventbus.provider.RLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * redis stream过期消息处理
 * @author : jin.ping
 * @date : 2024/9/5
 */
@Slf4j
public class RedisStreamExpiredTask implements Runnable, Lifecycle {
    /**
     * 数据清理定时任务，默认：每个小时29分进行清理
     */
    private static final String CRON = "0 29 1/1 * * ?";
    private final EventBusProperties eventBusProperties;
    private final RLock rLock;
    private final StringRedisTemplate redisTemplate;
    private final List<RedisListener> redisSubscribers;
    private final DefaultRedisScript<Long> script;
    private boolean versionGE6_2 = false;
    private CronTask task;
    private final TaskRegistry taskRegistry;

    public RedisStreamExpiredTask(StringRedisTemplate redisTemplate,
                                  TaskRegistry taskRegistry,
                                  EventBusProperties eventBusProperties,
                                  List<Listener> subscribers,
                                  RLock rLock) {
        this.eventBusProperties = eventBusProperties;
        this.taskRegistry = taskRegistry;
        this.rLock = rLock;
        this.redisTemplate = redisTemplate;

        // 及时消息订阅者 + 固定一个的延时消息订阅者
        this.redisSubscribers = RedisListener.fullRedisSubscriber(subscribers, eventBusProperties.getEnv(), eventBusProperties.getServiceId());
        String redisVersion = eventBusProperties.getRedis().getRedisVersion();
        // 判断最低版本是否大于等于 6.2
        if (redisVersion.contains("-")) {
            redisVersion = redisVersion.substring(0, redisVersion.indexOf("-"));
        }
        String[] versions = redisVersion.split("\\.");
        String bigVersion = versions[0];
        if (bigVersion.compareTo("6") >= 0
                && (bigVersion.compareTo("7") >= 0 || (versions.length >= 2 && versions[1].compareTo("2") >= 0))) {
            versionGE6_2 = true;
        }
        // 过期消息处理脚本
        String cmd;
        if(versionGE6_2) {
            cmd = "'MINID'";
        } else {
            //是否精确裁剪
            cmd =  Boolean.TRUE.equals(eventBusProperties.getRedis().getStreamPrecisePruning()) ? "'MAXLEN'" : "'MAXLEN', '~'";
        }
        this.script = new DefaultRedisScript<>("return redis.call('XTRIM', KEYS[1]," + cmd + ", ARGV[1]);", Long.class);
    }

    @Override
    public void register() {
        task = CronTask.create(this.getClass().getName(), CRON, this);
        taskRegistry.createTask(task);
    }

    @Override
    public void run() {
        redisSubscribers.stream()
                .map(RedisListener::getStreamKey)
                .distinct()
                .forEach(this::cleanExpired);
    }


    /**
     * 调整消息的可见性和范围来实现的。通过XTRIM命令，可以指定保留的消息范围，超出这个范围的旧消息将不再被新的消费者读取
     * 截取过期的消息
     */
    private void cleanExpired(String streamKey) {
        String lockKey = streamKey + ".deleteExpiredLock";
        String request = UUID.randomUUID().toString();
        boolean lock = rLock.getLock(lockKey, request);
        try {
            if (!lock) {
                return;
            }
            String param;
            if (versionGE6_2) {
                // stream 过期时间，单位：小时
                Long expiredHours = eventBusProperties.getRedis().getStreamExpiredHours();
                // 过期时间毫秒数
                long expiredMillis = System.currentTimeMillis() - (1000L * 60 * 60 * expiredHours);
                param = expiredMillis + "-0";
            } else {
                Long streamExpiredLength = eventBusProperties.getRedis().getStreamExpiredLength();
                param = null == streamExpiredLength ? null : streamExpiredLength.toString();
            }
            if (null == param) {
                return;
            }
            Long deleteCount = redisTemplate.execute(script, Collections.singletonList(streamKey), param);
            log.debug("clean expired：streamKey={}, deleteCount={}", streamKey, deleteCount);
        } catch (Exception e) {
            log.error("clean expired：streamKey={}", streamKey, e);
        } finally {
            if (lock) {
                rLock.releaseLock(lockKey, request);
            }
        }
    }

    @Override
    public void destroy() {
        taskRegistry.removeTask(task);
    }
}
