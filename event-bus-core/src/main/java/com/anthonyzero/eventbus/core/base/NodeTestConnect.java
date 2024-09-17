package com.anthonyzero.eventbus.core.base;

/**
 * 节点连接状态检测接口，用于判断当前应用是否与节点断开连接
 * @author : jin.ping
 * @date : 2024/9/4
 */
public interface NodeTestConnect {

    /**
     * 检测确认节点是否连接
     *
     * @return true已连接、false连接断开
     */
    boolean testConnect();
}
