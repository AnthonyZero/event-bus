package com.anthonyzero.eventbus.core.utils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *  自定义的线程池执行器，继承自ThreadPoolExecutor，增加了对线程池状态的检查，
 *  以确保在执行任务时线程池是处于可以接受任务的状态。
 * @author : jin.ping
 * @date : 2024/9/3
 */
public class WaitThreadPoolExecutor extends ThreadPoolExecutor {

    /**
     * 构造函数，初始化线程池。
     *
     * @param corePoolSize    核心线程数
     * @param maximumPoolSize 最大线程数
     * @param keepAliveTime   线程空闲时的存活时间
     * @param unit            时间单位
     * @param workQueue       工作队列，用于存放待执行的任务
     * @param threadFactory   线程工厂，用于创建线程
     */
    public WaitThreadPoolExecutor(int corePoolSize,
                                  int maximumPoolSize,
                                  long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    /**
     * 覆盖execute方法，添加对线程池状态的检查。
     *
     * @param r 要执行的任务
     */
    @Override
    public synchronized void execute(Runnable r) {
        // 当线程池未关闭且未终止时，检查是否可以接受新任务
        while (!isShutdown() && !isTerminated()) {
            // 如果当前线程池大小小于最大线程数，或者工作队列还有剩余容量
            if (getPoolSize() < getMaximumPoolSize()
                    || getQueue().remainingCapacity() > 0) {
                // 执行任务并退出循环
                super.execute(r);
                break;
            }
        }
    }
}
