package com.ding.aiplatjava.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;

/**
 * 视频处理微服务配置类
 * 配置与视频处理和Whisper微服务的通信
 * 
 * @author Ding
 * @since 2024-12-28
 */
@Configuration
public class VideoProcessingConfig {

    @Value("${video.processing.service.url:http://localhost:9000}")
    private String videoServiceUrl;

    @Value("${whisper.service.url:http://localhost:9999}")
    private String whisperServiceUrl;

    @Value("${video.processing.timeout.connect:30000}")
    private int connectTimeout;

    @Value("${video.processing.timeout.read:300000}")
    private int readTimeout;

    /**
     * 配置用于视频处理微服务通信的RestTemplate
     * 设置较长的超时时间以适应视频处理的耗时特性
     * 
     * @return 配置好的RestTemplate实例
     */
    @Bean("videoProcessingRestTemplate")
    public RestTemplate videoProcessingRestTemplate() {
        // 创建HTTP客户端配置
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeout))
                .setResponseTimeout(Timeout.ofMilliseconds(readTimeout))
                .build();

        // 创建HTTP客户端
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        // 创建请求工厂
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClient);

        return new RestTemplate(factory);
    }

    /**
     * 获取视频处理服务URL
     * 
     * @return 视频处理服务的基础URL
     */
    public String getVideoServiceUrl() {
        return videoServiceUrl;
    }

    /**
     * 获取Whisper服务URL
     * 
     * @return Whisper服务的基础URL
     */
    public String getWhisperServiceUrl() {
        return whisperServiceUrl;
    }

    /**
     * 获取连接超时时间
     * 
     * @return 连接超时时间（毫秒）
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * 获取读取超时时间
     * 
     * @return 读取超时时间（毫秒）
     */
    public int getReadTimeout() {
        return readTimeout;
    }
}
