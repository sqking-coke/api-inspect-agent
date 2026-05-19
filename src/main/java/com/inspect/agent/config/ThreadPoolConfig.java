package com.inspect.agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池配置，提供巡检并行执行所需的线程池。
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * 巡检专用线程池。
     * <p>核心线程数 = 配置的 parallelThreads，最大线程数 = core * 2，
     * 空闲 60s 回收，有界队列 500，拒绝策略为 CallerRunsPolicy。
     */
    @Bean("inspectExecutor")
    public ThreadPoolExecutor inspectExecutor(AgentConfig config) {
        int core = config.getParallelThreads();
        return new ThreadPoolExecutor(
                core, core * 2,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(500),
                r -> {
                    Thread t = new Thread(r, "inspect-worker");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}