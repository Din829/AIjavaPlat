package com.ding.aiplatjava.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * OCR服务配置类
 * 配置RestTemplate和异步任务执行器
 */
@Configuration
@EnableAsync
public class OcrConfig {

    /**
     * 创建RestTemplate Bean
     * 用于调用OCR微服务
     *
     * @param builder RestTemplateBuilder
     * @return RestTemplate实例
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(30))
                .setReadTimeout(Duration.ofSeconds(180))  // 增加到3分钟
                .build();
    }

    /**
     * 创建异步任务执行器
     * 用于处理OCR异步任务
     *
     * @return AsyncTaskExecutor实例
     */
    @Bean
    public AsyncTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("OcrTask-");
        executor.initialize();
        return executor;
    }
}
