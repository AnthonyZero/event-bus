package com.anthonyzero.eventbus.core.support.task;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务类，用于执行周期性的任务。
 * 继承自Task类，增加了周期时间和时间单位的属性，用于精确控制任务的执行间隔。
 *
 */
public class PeriodTask extends Task {

    /**
     * 周期性任务的执行间隔时间
     */
    private long pollInterval;
    /**
     * 周期性任务的执行间隔时间单位
     */
    private TimeUnit timeUnit;

    /**
     * 初始化周期性任务。
     *
     * @param name         任务名称
     * @param pollInterval 任务执行的周期时间
     * @param timeUnit     任务执行周期的时间单位
     * @param runnable     要执行的任务
     */
    private void initTask(String name, long pollInterval, TimeUnit timeUnit, Runnable runnable) {
        init(name, runnable);
        this.pollInterval = pollInterval;
        this.timeUnit = timeUnit;
    }

    /**
     * 创建一个周期性任务，使用默认的时间单位（毫秒）。
     *
     * @param name         任务名称
     * @param pollInterval 任务执行的周期时间
     * @param runnable     要执行的任务
     * @return 创建的周期性任务实例
     */
    public static PeriodTask create(String name, long pollInterval, Runnable runnable) {
        return create(name, pollInterval, TimeUnit.MILLISECONDS, runnable);
    }

    /**
     * 创建一个周期性任务。
     *
     * @param name         任务名称
     * @param pollInterval 任务执行的周期时间
     * @param timeUnit     任务执行周期的时间单位
     * @param runnable     要执行的任务
     * @return 创建的周期性任务实例
     */
    public static PeriodTask create(String name, long pollInterval, TimeUnit timeUnit, Runnable runnable) {
        PeriodTask task = new PeriodTask();
        task.initTask(name, pollInterval, timeUnit, runnable);
        return task;
    }

    /**
     * 计算下一个执行时间。
     * 如果尚未执行过，则基于当前时间计算下一个执行时间。
     *
     * @return 下一个执行时间
     */
    @Override
    public long nextExecutionTime() {
        if (null == lastExecutionTime) {
            lastExecutionTime = new Date();
        }
        // 计算下一次执行时间，基于任务的周期和上次执行时间。
        return lastExecutionTime.getTime() + (pollInterval * timeUnit.toMillis(1L));
    }
}
