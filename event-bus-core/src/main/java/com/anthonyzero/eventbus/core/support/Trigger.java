package com.anthonyzero.eventbus.core.support;

import com.anthonyzero.eventbus.core.metadata.Message;
import com.anthonyzero.eventbus.core.metadata.Request;
import com.anthonyzero.eventbus.core.utils.Assert;
import com.anthonyzero.eventbus.core.utils.Func;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.*;

/**
 * 触发器实体 核心
 * @author : jin.ping
 * @date : 2024/9/3
 */
@Data
@Slf4j
public class Trigger {

    /**
     * 调用对象
     */
    private Object invokeBean;

    /**
     * 方法
     */
    private Method method;

    /**
     * 接收数据所在参数列表位置
     */
    private Integer messageDataIndex;

    /**
     * 接收数据所在参数数据类型
     */
    private Type messageDataType;

    /**
     * 异常所在参数列表位置
     */
    private Integer throwableIndex;

    /**
     * 参数数量
     */
    private int paramsCount;


    protected Trigger(Object invokeBean, Method method) {
        this.invokeBean = invokeBean;
        this.method = method;
        // 构建参数
        buildParams(method);
    }

    /**
     * 构建触发器
     *
     * @param invokeBean 调用对象
     * @param method     方法
     * @return 触发器
     */
    public static Trigger of(Object invokeBean, Method method) {
        return new Trigger(invokeBean, method);
    }

    /**
     * 投递ID
     */
    public String getDeliverId() {
        return Func.getDeliverId(Func.primitiveClass(invokeBean), method.getName());
    }

    /**
     * 触发调用
     *
     * @param message 消息
     */
    @SuppressWarnings("all")
    public void invoke(Message message) throws InvocationTargetException, IllegalAccessException {
        invoke(message, null);
    }

    /**
     * 触发调用
     *
     * @param message   消息
     * @param throwable 异常
     */
    @SuppressWarnings("all")
    public void invoke(Message message, Throwable throwable) throws InvocationTargetException, IllegalAccessException {
        Request request = (Request) message; //sender
        Object oldBody = request.getBody();
        try {
            Object[] args = new Object[this.paramsCount];
            if (this.paramsCount > 0) {
                if (this.messageDataIndex >= 0) {
                    request.setBody(Func.parseObject(message.getBody(), messageDataType));
                    args[this.messageDataIndex] = message;
                }
                if (this.throwableIndex >= 0) {
                    args[this.throwableIndex] = throwable;
                }
            }
            method.invoke(invokeBean, args); //invoke
        } finally {
            request.setBody(oldBody);
        }
    }

    /**
     * 构建参数
     */
    private void buildParams(Method method) {
        if (null == method) {
            return;
        }
        int modifiers = method.getModifiers();
        try {
            Assert.isTrue(Modifier.isPublic(modifiers),
                    String.format("Method %s of %s must be public", method.getName(), method.getDeclaringClass().getName()));
            Class<?> primitiveClass = Func.primitiveClass(invokeBean);
            method = primitiveClass.getMethod(method.getName(), method.getParameterTypes());
            Type[] parameterTypes = method.getGenericParameterTypes();
            this.paramsCount = parameterTypes.length;
            this.messageDataIndex = -1;
            this.throwableIndex = -1;
            for (int index = 0; index < parameterTypes.length; index++) {
                String typeName = parameterTypes[index].getTypeName();
                // 接收消息
                if (typeName.contains(Message.class.getName())) {
                    messageDataIndex = index;
                    messageDataType = ((ParameterizedType) parameterTypes[index]).getActualTypeArguments()[0];
                }
                // 接收异常
                else if (typeName.contains(Throwable.class.getName())) {
                    throwableIndex = index;
                }
            }
        } catch (Exception e) {
            log.error("Trigger.buildParams", e);
            System.exit(1);
        }
    }
}
