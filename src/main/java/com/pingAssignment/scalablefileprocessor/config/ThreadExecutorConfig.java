package com.pingAssignment.scalablefileprocessor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class ThreadExecutorConfig {
    @Value("${fileprocessor.maxThreads}")
    private int maxThreads;

    @Bean(name = "fileProcessingExecutor")
    public ThreadPoolTaskExecutor fileProcessorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(maxThreads);
        executor.setMaxPoolSize(maxThreads);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("fileProcessor-");
        executor.initialize();
        return executor;
    }
}
