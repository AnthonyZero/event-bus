package com.anthonyzero.eventbus.core.base;

import com.anthonyzero.eventbus.core.api.RequestIdGenerator;

import java.util.UUID;

/**
 * 默认请求id生成器,使用UUID
 * @author : jin.ping
 * @date : 2024/9/4
 */
public class UUIDRequestIdGenerator implements RequestIdGenerator {
    @Override
    public String nextId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
