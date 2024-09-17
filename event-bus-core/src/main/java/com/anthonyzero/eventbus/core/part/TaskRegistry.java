package com.anthonyzero.eventbus.core.part;

import com.anthonyzero.eventbus.core.utils.Assert;
import com.anthonyzero.eventbus.core.utils.NamedThreadFactory;
import com.anthonyzero.eventbus.core.utils.WaitThreadPoolExecutor;
import com.anthonyzero.eventbus.core.constant.EventBusConstant;
import com.anthonyzero.eventbus.core.support.task.Task;
import com.anthonyzero.eventbus.core.support.task.Timer;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务注册中心，负责管理和调度各种定时任务。
 * @author : jin.ping
 * @date : 2024/9/4
 */
public class TaskRegistry {
    /**
     * 使用Timer进行任务定时触发
     */
    @SuppressWarnings("all")
    private final Timer timer = new Timer(false);

    /**
     * 线程池执行器，用于执行任务
     */
    @Getter
    private final WaitThreadPoolExecutor poolExecutor;
    /**
     * 任务映射，存储所有任务
     */
    private final Map<String, Task> taskMap = new ConcurrentHashMap<>();

    /**
     * 构造函数，创建一个带默认线程池的TaskRegistry。
     */
    public TaskRegistry() {
        this.poolExecutor = createDefaultPool();
    }

    /**
     * 构造函数，使用提供的线程池执行器创建TaskRegistry。
     *
     * @param poolExecutor 自定义的线程池执行器
     */
    public TaskRegistry(WaitThreadPoolExecutor poolExecutor) {
        this.poolExecutor = poolExecutor;
    }

    /**
     * 创建任务，添加到任务映射中并开始执行。
     *
     * @param task 要创建的任务
     */
    @SuppressWarnings("all")
    public void createTask(Task task) {
        Assert.isTrue(task.isInitialized(), "任务未初始化！");
        // 确保任务名称唯一
        Assert.isTrue(!taskMap.containsKey(task.getName()), "任务已存在");
        // 通过任务注册表获取线程池，并注册当前任务 task。
        task.setTaskRegistry(this);
        taskMap.put(task.getName(), task);
        // 根据任务的下次执行时间安排任务
        timer.schedule(task, 0, task.nextExecutionTime() - System.currentTimeMillis());
    }

    /**
     * 取消指定的任务。
     *
     * @param task 要取消的任务
     */
    public void removeTask(Task task) {
        if (null == task) {
            return;
        }
        task.cancel();
        taskMap.remove(task.getName());
    }

    /**
     * 刷新指定的任务。
     */
    public void refresh() {
        timer.refresh();
    }

    /**
     * 获取指定名称的任务。
     *
     * @param name 任务名称
     * @return 指定名称的任务
     */
    public Task getTask(String name) {
        return taskMap.get(name);
    }

    /**
     * 创建默认的线程池执行器。
     *
     * @return 默认的线程池执行器
     */
    private WaitThreadPoolExecutor createDefaultPool() {
        return new WaitThreadPoolExecutor(1,
                10, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10), new NamedThreadFactory(EventBusConstant.TASK_NAME));
    }
}
