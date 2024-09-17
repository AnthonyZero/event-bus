package com.anthonyzero.eventbus.core.constant;

/**
 * eventbus常量
 *
 */
public class EventBusConstant {
    private EventBusConstant() {
    }

    /**
     * 接口订阅器接收方法名
     *
     */
    public static final String ON_MESSAGE = "onMessage";

    /**
     * thread name
     */
    public static final String TASK_NAME = "eventbus-task-pool-";

    /**
     * subscribe thread name
     */
    public static final String THREAD_NAME = "eventbus-msg-pool-";
}
