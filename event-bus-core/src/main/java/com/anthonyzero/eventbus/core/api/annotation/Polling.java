package com.anthonyzero.eventbus.core.api.annotation;

import com.anthonyzero.eventbus.core.utils.Assert;
import com.anthonyzero.eventbus.core.utils.CalculateUtil;
import com.anthonyzero.eventbus.core.utils.Func;
import lombok.experimental.UtilityClass;

import java.lang.annotation.*;

/**
 *  定义了一个注解@Polling，用于标注在方法上以控制消息订阅的轮询行为。
 *  轮询可以通过注解的属性count和interval进行配置。
 *  收到消息后 决定进行轮询投递（延时消息）
 * @author : jin.ping
 * @date : 2024/9/3
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Polling {

    /**
     * 定义了轮询的次数。默认值为-1，表示不进行轮询。
     * 如果设置了具体的轮询次数，则方法会在接收到消息后按照指定次数进行轮询。
     * 可通过{@link Polling.Keep#over()}编码方式调用，提前结束轮询任务。
     *
     * @return 轮询次数，默认为-1。
     */
    int count() default -1;

    /**
     * 定义了轮询的时间间隔，支持表达式配置。
     * 表达式中可以使用两个变量：count（当前轮询次数）和intervalTime（本次轮询与上次轮询的时间间隔，单位为秒）。
     * 这使得可以灵活地根据轮询次数和时间间隔来动态确定下一次轮询的时间。
     * 示例：
     * 1. interval=7，表示轮询间隔为7秒。
     * 2. interval=$count*$intervalTime，表示轮询间隔为当前轮询次数与上次轮询的时间间隔的乘积。
     *
     * @return 轮询时间间隔的表达式，默认为空字符串。
     */
    String interval() default "";

    /**
     * Keep类提供了轮询控制的静态方法，用于标记轮询应该结束。
     *
     */
    @UtilityClass
    class Keep {
        /**
         * 使用ThreadLocal存储轮询是否应该结束的标记。
         */
        private static final ThreadLocal<Boolean> OVER = new ThreadLocal<>();

        /**
         * 检查当前轮询是否应该结束。
         *
         * @return 如果轮询应该结束，则返回true；否则返回false。
         */
        public boolean isOver() {
            return OVER.get() != null && OVER.get();
        }

        /**
         * 设置轮询应该结束的标记。
         */
        public void over() {
            OVER.set(Boolean.TRUE);
        }

        /**
         * 清除轮询结束的标记，并返回之前标记的状态。
         *
         * @return 如果之前标记为轮询结束，则返回true；否则返回false。
         */
        public boolean clear() {
            boolean isOver = isOver();
            OVER.remove();
            return isOver;
        }
    }

    /**
     * ValidatorInterval类提供了验证轮询时间间隔表达式的方法。
     *
     * @author likavn
     * @date 2024/07/27
     * @since 2.3.2
     */
    @UtilityClass
    class ValidatorInterval {
        /**
         * 验证指定的轮询时间间隔表达式是否合法。
         *
         * @param interval 轮询时间间隔的表达式。
         */
        public void isValid(String interval) {
            if (Func.isEmpty(interval)) {
                return;
            }
            interval = interval.replace("$count", "1")
                    .replace("$intervalTime", "1");
            double v = CalculateUtil.evalExpression(interval);
            Assert.isTrue(v > 0, "interval must be greater than 0");
        }
    }
}
