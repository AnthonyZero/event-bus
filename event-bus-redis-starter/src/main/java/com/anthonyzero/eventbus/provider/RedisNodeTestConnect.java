package com.anthonyzero.eventbus.provider;

import com.anthonyzero.eventbus.core.base.NodeTestConnect;
import com.anthonyzero.eventbus.core.config.GlobalConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * redis连接状态测试
 * @author : jin.ping
 * @date : 2024/9/4
 */
@Slf4j
public class RedisNodeTestConnect implements NodeTestConnect {

    private final StringRedisTemplate stringRedisTemplate;
    private final String testKey;

    public RedisNodeTestConnect(StringRedisTemplate stringRedisTemplate, GlobalConfig globalConfig) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.testKey = String.format("eventbus.{%s}", globalConfig.getEnv());
    }

    @Override
    public boolean testConnect() {
        try {
            stringRedisTemplate.hasKey(testKey);
            return true;
        } catch (Exception ex) {
            log.error("redis timeout", ex);
            return false;
        }
    }
}
