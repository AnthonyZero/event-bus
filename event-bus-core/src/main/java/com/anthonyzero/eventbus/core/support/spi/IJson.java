package com.anthonyzero.eventbus.core.support.spi;


import com.anthonyzero.eventbus.core.utils.Func;

import java.lang.reflect.Type;

/**
 * JSON处理接口。
 * 提供了检查字符串是否为JSON格式、激活性检查、类名获取、对象转JSON字符串、JSON字符串转对象和获取处理顺序等方法。
 *
 */
public interface IJson {
    /**
     * 定义正则表达式，用于匹配JSON格式的字符串
     */
    String PATTERN_JSON = "(\\{.*\\}|\\[.*\\])";

    /**
     * 检查给定字符串是否为JSON格式。
     *
     * @param val 待检查的字符串
     * @return 如果字符串为JSON格式，则返回true；否则返回false。
     */
    default boolean isJson(String val) {
        if (Func.isEmpty(val)) {
            return false;
        }
        return val.matches(PATTERN_JSON);
    }

    /**
     * 检查当前实现类是否激活。
     * 通过尝试加载实现类的类名来判断其是否可用。
     *
     * @return 如果类可用，则返回true；否则返回false。
     */
    default boolean active() {
        try {
            Class.forName(className());
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }

    /**
     * 获取当前实现类的类名。
     * 用于激活检查和可能的实例化操作。
     *
     * @return 当前实现类的类名。
     */
    String className();

    /**
     * 将对象转换为JSON格式的字符串。
     *
     * @param value 待转换的对象
     * @return 对象的JSON字符串表示
     */
    String toJsonString(Object value);

    /**
     * 将JSON字符串解析为指定类型的对象。
     *
     * @param text JSON字符串
     * @param type 目标对象的类型
     * @param <T>  目标对象的泛型类型
     * @return 解析后的对象
     */
    <T> T parseObject(String text, Type type);

    /**
     * 获取当前实现的处理顺序。
     * 用于在多个实现存在时确定处理的优先级,order越小越优先。
     *
     * @return 处理顺序的整数表示
     */
    int getOrder();
}
