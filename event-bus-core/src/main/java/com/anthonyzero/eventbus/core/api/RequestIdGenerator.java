package com.anthonyzero.eventbus.core.api;

/**
 * 请求ID生成接口，须保证ID的唯一性
 *
 **/
public interface RequestIdGenerator {

    /**
     * 获取请求ID
     *
     * @return 请求ID
     */
    String nextId();
}
