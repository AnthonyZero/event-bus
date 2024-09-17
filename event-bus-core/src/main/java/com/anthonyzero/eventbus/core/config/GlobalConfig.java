package com.anthonyzero.eventbus.core.config;

import lombok.Data;
import lombok.ToString;

/**
 * eventbus全局配置
 *
 */
@Data
public class GlobalConfig {

    /**
     * 环境
     */
    protected String env;

    /**
     * 服务ID/消息来源ID，可以不用配置，默认为：spring.application.name (启动赋值)
     */
    protected String serviceId;

    /**
     * 消息引擎类别（redis、kafka、rocketmq）
     */
    protected String type;

    /**
     * 异步消息接收并发数，默认为：1
     */
    protected Integer concurrency = 1;

    /**
     * 延时消息接收并发数，默认为：2
     */
    protected Integer delayConcurrency = 2;

    /**
     * 单次获取消息数量，默认：16条
     */
    protected Integer msgBatchSize = 16;

    /**
     * 节点联通性配置
     */
    protected TestConnect testConnect = new TestConnect();

    /**
     * 消息投递失败时配置信息
     */
    protected Fail fail = new Fail();

    /**
     * 节点联通性配置
     */
    @Data
    @ToString
    public static class TestConnect {
        /**
         * 轮询检测时间间隔，单位：秒，默认：35秒进行检测一次
         */
        private Long pollSecond = 35L;

        /**
         * 丢失连接最长时间大于等于次值设置监听容器为连接断开，单位：秒，默认：120秒
         */
        private Long loseConnectMaxMilliSecond = 120L;
    }

    /**
     * 消息投递失败时配置
     */
    @Data
    @ToString
    public static class Fail {
        /**
         * 消息投递失败时，一定时间内再次进行投递的次数，默认：3次
         */
        private Integer retryCount = 3;

        /**
         * 下次触发时间，单位：秒，默认10秒 ，
         */
        private Long nextTime = 10L;
    }
}
