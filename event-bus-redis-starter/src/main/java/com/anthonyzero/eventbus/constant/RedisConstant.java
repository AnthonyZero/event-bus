package com.anthonyzero.eventbus.constant;

/**
 * @author : jin.ping
 * @date : 2024/9/5
 */
public class RedisConstant {
    private RedisConstant() {
    }

    /**
     * 前缀
     */
    private static final String SUFFIX = "%s:eventbus:";

    /**
     * 消息队列前缀
     * 参数：
     * <p>
     * 1.topic(serviceId + "|" + code);
     */
    public static final String BUS_SUBSCRIBE_PREFIX = SUFFIX + "queue:{%s}";

    /**
     * 延时队列lock,延时消息处理到期 lock key
     * 参数：
     * <p>
     * 1.服务serviceId;
     */
    public static final String BUS_DELAY_LOCK_PREFIX = SUFFIX + "lock:delay:{%s}";

    /**
     * 延时队列zset key
     * 参数：
     * <p>
     * 1.服务serviceId;
     */
    public static final String BUS_DELAY_PREFIX = SUFFIX + "delay-zset:{%s}";
    /**
     * 延时消息通知Stream队列key前缀
     * 参数：
     * <p>
     * 1.服务serviceId;
     */
    public static final String BUS_DELAY_SUBSCRIBE_PREFIX = SUFFIX + "queue-delay:{%s}";
}
