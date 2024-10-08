package com.anthonyzero.eventbus.core.support.task;

import com.anthonyzero.eventbus.core.part.TaskRegistry;
import com.anthonyzero.eventbus.core.utils.WaitThreadPoolExecutor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 * 任务类，抽象类，继承自TimerTask，用于定义和管理任务。
 * 提供了任务初始化、执行、以及下次执行时间刷新等功能。
 *
 */
@Slf4j
public abstract class Task extends TimerTask {

    /**
     * 任务名称。
     */
    @Getter
    private String name;

    /**
     * 任务执行的Runnable对象。
     */
    private Runnable runnable;

    /**
     * 上次任务执行的时间。
     */
    protected Date lastExecutionTime;

    /**
     * 任务注册表，用于管理任务。
     */
    private TaskRegistry taskRegistry;

    /**
     * 任务执行的线程池。
     */
    private WaitThreadPoolExecutor poolExecutor;

    /**
     * 标志任务是否已初始化。
     */
    @Getter
    private boolean initialized = false;

    /**
     * 初始化任务。
     *
     * @param name     任务名称。
     * @param runnable 任务执行体。
     */
    protected void init(String name, Runnable runnable) {
        this.name = name;
        this.runnable = runnable;
        this.nextExecutionTime = System.currentTimeMillis();
        this.initialized = true;
    }

    /**
     * 任务执行的方法。
     * 将任务提交给线程池执行，并更新下次执行时间。
     */
    @Override
    public void run() {
        this.lastExecutionTime = new Date();
        try {
            poolExecutor.execute(runnable);
            super.nextExecutionTime = nextExecutionTime();
        } catch (Exception e) {
            log.error("task run error", e);
        }
    }

    /**
     * 设置任务注册表。
     * 通过任务注册表获取线程池，并注册当前任务。
     *
     * @param taskRegistry 任务注册表。
     */
    public void setTaskRegistry(TaskRegistry taskRegistry) {
        this.poolExecutor = taskRegistry.getPoolExecutor();
        this.taskRegistry = taskRegistry;
    }

    /**
     * 刷新下次执行时间。
     * 如果传入的下次执行时间早于当前设定的下次执行时间，则更新下次执行时间，并通知任务注册表刷新。
     *
     * @param nextExecutionTime 下次执行时间。
     */
    public void refreshNextExecutionTime(long nextExecutionTime) {
        if (0 < nextExecutionTime && nextExecutionTime < this.nextExecutionTime) {
            this.nextExecutionTime = nextExecutionTime;
            this.taskRegistry.refresh();
        }
    }

    /**
     * 计算并返回下次任务执行的时间。
     * 该方法为抽象方法，由子类实现具体的计算逻辑。
     *
     * @return 下次任务执行的时间。
     */
    public abstract long nextExecutionTime();
}
