package com.anthonyzero.eventbus.prop;

import com.anthonyzero.eventbus.core.config.GlobalConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author : jin.ping
 * @date : 2024/9/5
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties(prefix = "eventbus")
public class EventBusProperties extends GlobalConfig {

    /**
     * redis配置
     */
    private RedisProperties redis = new RedisProperties();

    /**
     * redis配置
     */
    @Data
    public static class RedisProperties {
        /**
         * 是否为阻塞轮询，开启阻塞轮询会占用redis连接的线程池。占用线程数量=消费者并发总数，默认为：否，不开启阻塞和轮询
         */
        private Boolean pollBlock = false;
        /**
         * 非阻塞轮询时，接收消息的线程池中线程最大数，默认为：5个
         */
        private Integer pollThreadPoolSize = 5;
        /**
         * 非阻塞轮询时，接收消息的线程池中空闲线程存活时长，单位：秒，默认为：300s
         */
        private Integer pollThreadKeepAliveTime = 300;
        /**
         * 消息超时时间，超时消息未被确认，才会被重新投递，单位：秒，默认：5分钟
         */
        private Long deliverTimeout = 60 * 5L;
        /**
         * 未确认消息，重新投递时每次最多拉取多少条待确认消息数据，默认：100条
         */
        private Integer pendingMessagesBatchSize = 100;
        /**
         * stream 过期时间，6.2及以上版本支持，单位：小时，默认：5 天
         */
        private Long streamExpiredHours = 24 * 5L;

        /**
         * stream 过期数据截取，搭配streamExpiredLength 是否精确修剪，默认：False
         */
        private Boolean streamPrecisePruning = false;

        /**
         * stream 过期数据截取，值为当前保留的消息数，5.0~<6.2版本支持，单位：条，默认：50000条
         */
        private Long streamExpiredLength = 50000L;

        /**
         * redis版本号，不用配置，系统自动设定
         */
        private String redisVersion;
    }
}
