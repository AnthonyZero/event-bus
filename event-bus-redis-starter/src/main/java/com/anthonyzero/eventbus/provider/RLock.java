package com.anthonyzero.eventbus.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/**
 * redis分布式锁
 * @author : jin.ping
 * @date : 2024/9/4
 */
@Slf4j
public class RLock {

    /**
     * 分布式锁过期时间,单位：秒
     */
    private static final Long LOCK_REDIS_TIMEOUT = 30L;
    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Boolean> lockRedisScript;
    private final DefaultRedisScript<Long> unlockRedisScript;
    private final DefaultRedisScript<Boolean> renewalRedisScript;

    public RLock(StringRedisTemplate stringRedisTemplate, DefaultRedisScript<Boolean> lockRedisScript,
                 DefaultRedisScript<Long> unlockRedisScript, DefaultRedisScript<Boolean> renewalRedisScript) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.lockRedisScript = lockRedisScript;
        this.unlockRedisScript = unlockRedisScript;
        this.renewalRedisScript = renewalRedisScript;
    }

    /**
     * 获取锁,默认超时时间30s
     *
     * @param key key
     * @return t
     */
    public boolean getLock(String key, String request) {
        return getLock(key, request, LOCK_REDIS_TIMEOUT);
    }

    /**
     * 获取锁
     *
     * @param key     key
     * @param timeout 超时时间，单位：秒
     * @return t
     */
    public boolean getLock(String key, String request, long timeout) {
        Boolean flag = stringRedisTemplate.execute(lockRedisScript, Collections.singletonList(key), request, String.valueOf(timeout));
        if(Boolean.TRUE.equals(flag)) {
            this.renewExpire(key, request, timeout);
        }
        return Boolean.TRUE.equals(flag);
    }

    private void renewExpire(String key, String request, long timeout) { //timeout = s
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Boolean flag = stringRedisTemplate.execute(renewalRedisScript, Collections.singletonList(key), request, String.valueOf(timeout));
                if(Boolean.TRUE.equals(flag)) {
                    renewExpire(key, request, timeout);
                }
            }
        }, timeout * 1000/ 3);  //ms
    }

    /**
     * 释放锁
     *
     * @param key k
     */
    public void releaseLock(String key, String request) {
        Long flag = stringRedisTemplate.execute(unlockRedisScript, Collections.singletonList(key), request);
        if(Objects.isNull(flag)) {
            throw new IllegalMonitorStateException("this lock doesn't belong to you!");
        }
    }
}
