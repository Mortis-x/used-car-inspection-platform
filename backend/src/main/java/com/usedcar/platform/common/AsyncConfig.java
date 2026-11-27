package com.usedcar.platform.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    /**
     * 附件处理线程池：有界队列 + 自定义拒绝策略（降级为调用线程同步执行）。
     * 避免 50 张原图同时处理把内存和带宽打满。
     */
    @Bean(name = "attachmentExecutor", destroyMethod = "shutdown")
    public ThreadPoolExecutor attachmentExecutor() {
        AtomicInteger seq = new AtomicInteger();
        return new ThreadPoolExecutor(
                4, 8, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200),
                runnable -> {
                    Thread thread = new Thread(runnable, "attach-worker-" + seq.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                },
                (runnable, executor) -> {
                    log.warn("附件处理队列已满，降级为同步执行");
                    runnable.run();
                });
    }
}
