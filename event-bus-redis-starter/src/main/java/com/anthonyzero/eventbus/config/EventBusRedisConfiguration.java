package com.anthonyzero.eventbus.config;

import com.anthonyzero.eventbus.core.api.RequestIdGenerator;
import com.anthonyzero.eventbus.core.config.GlobalConfig;
import com.anthonyzero.eventbus.core.config.InterceptorConfig;
import com.anthonyzero.eventbus.core.part.DeliveryEventBus;
import com.anthonyzero.eventbus.core.part.ListenerRegistry;
import com.anthonyzero.eventbus.core.part.TaskRegistry;
import com.anthonyzero.eventbus.core.utils.Assert;
import com.anthonyzero.eventbus.prop.EventBusProperties;
import com.anthonyzero.eventbus.provider.*;
import com.anthonyzero.eventbus.provider.task.RedisPendingMsgResendTask;
import com.anthonyzero.eventbus.provider.task.RedisStreamExpiredTask;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.Properties;

/**
 * @author : jin.ping
 * @date : 2024/9/5
 */
@Configuration
@ConditionalOnClass(RedisConnectionFactory.class)
@ConditionalOnProperty(prefix = "eventbus", name = "type", havingValue = "redis")
public class EventBusRedisConfiguration {

    @Bean
    @ConditionalOnMissingBean(StringRedisTemplate.class)
    public StringRedisTemplate busStringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public RedisNodeTestConnect redisNodeTestConnect(StringRedisTemplate busStringRedisTemplate, GlobalConfig config) {
        return new RedisNodeTestConnect(busStringRedisTemplate, config);
    }

    //分布式锁
    @Bean
    public RLock rLock(StringRedisTemplate busStringRedisTemplate, EventBusProperties eventBusProperties, DefaultRedisScript<Boolean> lockRedisScript,
                       DefaultRedisScript<Long> unlockRedisScript, DefaultRedisScript<Boolean> renewalRedisScript) {
        checkRedisVersion(busStringRedisTemplate, eventBusProperties);
        return new RLock(busStringRedisTemplate, lockRedisScript, unlockRedisScript, renewalRedisScript);
    }


    /**
     * 任务注册器
     */
    @Bean
    public TaskRegistry taskRegistry() {
        return new TaskRegistry();
    }

    //及时消息订阅者容器
    @Bean
    public RedisMsgSubscribeContainer redisMsgSubscribeContainer(
            StringRedisTemplate busStringRedisTemplate,
            EventBusProperties eventBusProperties, ListenerRegistry registry, DeliveryEventBus deliveryEventBus) {
        return new RedisMsgSubscribeContainer(busStringRedisTemplate, eventBusProperties, registry.getTimelyListeners(), deliveryEventBus);
    }

    //延时消息订阅者容器
    @Bean
    public RedisMsgDelayContainer redisMsgDelayContainer(
            StringRedisTemplate busStringRedisTemplate, TaskRegistry taskRegistry,
            EventBusProperties eventBusProperties,
            @Qualifier("pushMsgStreamRedisScript") DefaultRedisScript<Long> pushMsgStreamRedisScript, RLock rLock, DeliveryEventBus deliveryEventBus) {
        return new RedisMsgDelayContainer(busStringRedisTemplate, taskRegistry, eventBusProperties, pushMsgStreamRedisScript, rLock, deliveryEventBus);
    }

    //消息生产者（redis）
    @Bean
    public RedisMsgSender msgSender(StringRedisTemplate busStringRedisTemplate,
                                    GlobalConfig config,
                                    @Lazy InterceptorConfig interceptorConfig,
                                    @Qualifier("zsetAddRedisScript")
                                    DefaultRedisScript<Long> zsetAddRedisScript,
                                    TaskRegistry taskRegistry, RequestIdGenerator requestIdGenerator, @Lazy ListenerRegistry registry) {
        return new RedisMsgSender(busStringRedisTemplate, config, interceptorConfig, zsetAddRedisScript, taskRegistry, requestIdGenerator, registry);
    }

    //PEL
    @Bean
    public RedisPendingMsgResendTask redisPendingMsgResendTask(
            StringRedisTemplate busStringRedisTemplate, TaskRegistry taskRegistry,
            EventBusProperties eventBusProperties, ListenerRegistry registry, RLock rLock, RedisMsgSender msgSender) {
        return new RedisPendingMsgResendTask(busStringRedisTemplate, taskRegistry, eventBusProperties, registry.getTimelyListeners(), rLock, msgSender);
    }

    //XTRIM
    @Bean
    public RedisStreamExpiredTask redisStreamExpiredTask(
            StringRedisTemplate busStringRedisTemplate, TaskRegistry taskRegistry, EventBusProperties eventBusProperties, ListenerRegistry registry, RLock rLock) {
        return new RedisStreamExpiredTask(busStringRedisTemplate, taskRegistry, eventBusProperties, registry.getTimelyListeners(), rLock);
    }

    //lua脚本
    @Configuration
    public static class ScriptAutoConfiguration {
        /**
         * redis锁脚本
         */
        @Bean
        public DefaultRedisScript<Boolean> lockRedisScript() {
            DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("script/lock.lua")));
            redisScript.setResultType(Boolean.class);
            return redisScript;
        }


        /**
         * redis解锁脚本
         */
        @Bean
        public DefaultRedisScript<Long> unlockRedisScript() {
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("script/unlock.lua")));
            redisScript.setResultType(Long.class);
            return redisScript;
        }


        /**
         * redis加锁续期脚本
         */
        @Bean
        public DefaultRedisScript<Boolean> renewalRedisScript() {
            DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("script/renewal.lua")));
            redisScript.setResultType(Boolean.class);
            return redisScript;
        }

        /**
         * redis 延时消息添加脚本
         */
        @Bean
        public DefaultRedisScript<Long> zsetAddRedisScript() {
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("script/zsetAdd.lua")));
            redisScript.setResultType(Long.class);
            return redisScript;
        }

        /**
         * redis 延时消息转移脚本
         */
        @Bean
        public DefaultRedisScript<Long> pushMsgStreamRedisScript() {
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("script/pushMsgStream.lua")));
            redisScript.setResultType(Long.class);
            return redisScript;
        }
    }

    /**
     * 校验 Redis 版本号，是否满足最低的版本号要求！
     */
    private static void checkRedisVersion(StringRedisTemplate busStringRedisTemplate, EventBusProperties eventBusProperties) {
        // 获得 Redis 版本
        Properties info = busStringRedisTemplate.execute((RedisCallback<Properties>) RedisServerCommands::info);
        Assert.notEmpty(info, "Redis 版本信息为空！");
        assert info != null;
        String version = info.getProperty("redis_version");
        eventBusProperties.getRedis().setRedisVersion(version);
        // 校验最低版本必须大于等于 5.0.0
        boolean isValid = false;
        String[] versions = version.split("\\.");
        if (versions[0].compareTo("5") >= 0) {
            isValid = true;
        }
        if (!isValid) {
            throw new IllegalStateException(String.format("您当前的 Redis 版本为 %s，小于最低要求的 5.0 版本！", version));
        }
    }
}
