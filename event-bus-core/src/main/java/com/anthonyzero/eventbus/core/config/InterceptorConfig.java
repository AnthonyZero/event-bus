package com.anthonyzero.eventbus.core.config;

import com.anthonyzero.eventbus.core.api.interceptor.*;
import com.anthonyzero.eventbus.core.metadata.Request;
import com.anthonyzero.eventbus.core.utils.Func;
import lombok.Setter;

/**
 * 拦截器配置信息
 * @author : jin.ping
 * @date : 2024/9/4
 */
@Setter
@SuppressWarnings("all")
public class InterceptorConfig {

    private final SendBeforeInterceptor sendBeforeInterceptor;
    private final SendAfterInterceptor sendAfterInterceptor;
    private final DeliverSuccessInterceptor deliverSuccessInterceptor;
    private final DeliverThrowableEveryInterceptor deliverThrowableEveryInterceptor;
    private final DeliverThrowableInterceptor deliverThrowableInterceptor;

    public InterceptorConfig(SendBeforeInterceptor sendBeforeInterceptor,
                             SendAfterInterceptor sendAfterInterceptor,
                             DeliverSuccessInterceptor deliverSuccessInterceptor,
                             DeliverThrowableEveryInterceptor deliverThrowableEveryInterceptor, DeliverThrowableInterceptor deliverThrowableInterceptor) {
        this.sendBeforeInterceptor = sendBeforeInterceptor;
        this.sendAfterInterceptor = sendAfterInterceptor;
        this.deliverSuccessInterceptor = deliverSuccessInterceptor;
        this.deliverThrowableEveryInterceptor = deliverThrowableEveryInterceptor;
        this.deliverThrowableInterceptor = deliverThrowableInterceptor;
    }

    /**
     * 发送前拦截
     *
     * @param request 请求
     */
    public void sendBeforeExecute(Request<?> request) {
        // 只有第一次发送才执行拦截器
        if (sendBeforeInterceptor != null && request.getDeliverCount() <= 1) {
            convertRequest(request);
            sendBeforeInterceptor.execute((Request<String>) request);
        }
    }

    /**
     * 发送后拦截
     *
     * @param request 请求
     */
    public void sendAfterExecute(Request<?> request) {
        // 只有第一次发送才执行拦截器
        if (sendAfterInterceptor != null && request.getDeliverCount() <= 1) {
            convertRequest(request);
            sendAfterInterceptor.execute((Request<String>) request);
        }
    }

    /**
     * 接收成功拦截
     *
     * @param request 请求
     */
    public void deliverSuccessExecute(Request<?> request) {
        if (deliverSuccessInterceptor != null) {
            convertRequest(request);
            deliverSuccessInterceptor.execute((Request<String>) request);
        }
    }

    /**
     * 接收异常拦截
     * 每次投递消息异常时都会调用
     *
     * @param request   请求
     * @param throwable 异常
     */
    public void deliverThrowableEveryExecute(Request<?> request, Throwable throwable) {
        if (deliverThrowableEveryInterceptor != null) {
            convertRequest(request);
            deliverThrowableEveryInterceptor.execute((Request<String>) request, throwable);
        }
    }

    /**
     * 接收异常拦截
     *
     * @param request   请求
     * @param throwable 异常
     */
    public void deliverThrowableExecute(Request<?> request, Throwable throwable) {
        if (deliverThrowableInterceptor != null) {
            convertRequest(request);
            deliverThrowableInterceptor.execute((Request<String>) request, throwable);
        }
    }

    /**
     * 将请求体转换为json
     *
     * @param request 请求
     */
    private void convertRequest(Request request) {
        if (!(request.getBody() instanceof String)) {
            request.setBody(Func.toJson(request.getBody()));
        }
    }
}
