package com.ding.aiplatjava.service.impl;

import com.ding.aiplatjava.service.LinkAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 链接分析服务实现类
 * 提供链接类型识别和元数据提取功能
 */
@Slf4j
@Service
public class LinkAnalysisServiceImpl implements LinkAnalysisService {

    /**
     * 已知的视频平台域名列表
     */
    private static final List<String> VIDEO_DOMAINS = Arrays.asList(
        "youtube.com", "youtu.be", "www.youtube.com",
        "bilibili.com", "www.bilibili.com", "b23.tv",
        "vimeo.com", "www.vimeo.com",
        "dailymotion.com", "www.dailymotion.com",
        "twitch.tv", "www.twitch.tv"
    );

    /**
     * HTTP客户端，用于发送请求
     */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String detectContentType(String url) {
        try {
            log.info("开始分析链接类型: {}", url);
            
            // 解析URL
            URI uri = new URI(url);
            String host = uri.getHost();
            
            if (host == null) {
                log.warn("无法解析URL的主机名: {}", url);
                return "WEBPAGE"; // 默认为网页
            }
            
            host = host.toLowerCase();
            log.debug("解析到的主机名: {}", host);
            
            // 检查是否为已知视频平台
            for (String videoDomain : VIDEO_DOMAINS) {
                if (host.equals(videoDomain) || host.endsWith("." + videoDomain)) {
                    log.info("识别为视频链接，匹配域名: {}", videoDomain);
                    return "VIDEO";
                }
            }
            
            // 检查URL路径中是否包含视频相关关键词
            String path = uri.getPath();
            if (path != null) {
                path = path.toLowerCase();
                if (path.contains("/watch") || path.contains("/video") || 
                    path.contains("/v/") || path.contains("/embed/")) {
                    log.info("根据URL路径识别为视频链接: {}", path);
                    return "VIDEO";
                }
            }
            
            // 检查查询参数中是否包含视频ID
            String query = uri.getQuery();
            if (query != null && query.toLowerCase().contains("v=")) {
                log.info("根据查询参数识别为视频链接");
                return "VIDEO";
            }
            
            log.info("识别为网页链接: {}", url);
            return "WEBPAGE";
            
        } catch (Exception e) {
            log.error("分析链接类型时发生错误: {}", url, e);
            return "WEBPAGE"; // 出错时默认为网页
        }
    }

    @Override
    public Map<String, Object> extractVideoMetadata(String url) {
        Map<String, Object> metadata = new HashMap<>();
        
        try {
            log.info("开始提取视频元数据: {}", url);
            
            // 这里暂时返回基础信息，后续会集成yt-dlp等工具
            metadata.put("url", url);
            metadata.put("title", "待提取");
            metadata.put("description", "待提取");
            metadata.put("duration", 0);
            metadata.put("platform", detectVideoPlatform(url));
            
            log.info("视频元数据提取完成: {}", metadata);
            
        } catch (Exception e) {
            log.error("提取视频元数据时发生错误: {}", url, e);
            metadata.put("error", e.getMessage());
        }
        
        return metadata;
    }

    @Override
    public boolean isUrlAccessible(String url) {
        try {
            log.debug("检查URL可访问性: {}", url);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(10))
                    .build();
            
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            boolean accessible = response.statusCode() < 400;
            
            log.debug("URL可访问性检查结果: {} - {}", url, accessible);
            return accessible;
            
        } catch (Exception e) {
            log.warn("检查URL可访问性时发生错误: {}", url, e);
            return false;
        }
    }

    @Override
    public String extractWebPageTitle(String url) {
        try {
            log.debug("开始提取网页标题: {}", url);
            
            Document doc = Jsoup.connect(url)
                    .timeout(10000)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get();
            
            String title = doc.title();
            log.debug("提取到网页标题: {}", title);
            
            return title != null && !title.trim().isEmpty() ? title.trim() : "无标题";
            
        } catch (Exception e) {
            log.error("提取网页标题时发生错误: {}", url, e);
            return "标题提取失败";
        }
    }

    /**
     * 检测视频平台
     * 
     * @param url 视频URL
     * @return 平台名称
     */
    private String detectVideoPlatform(String url) {
        try {
            String host = new URI(url).getHost().toLowerCase();
            
            if (host.contains("youtube")) return "YouTube";
            if (host.contains("bilibili")) return "Bilibili";
            if (host.contains("vimeo")) return "Vimeo";
            if (host.contains("dailymotion")) return "Dailymotion";
            if (host.contains("twitch")) return "Twitch";
            
            return "Unknown";
            
        } catch (Exception e) {
            log.warn("检测视频平台时发生错误: {}", url, e);
            return "Unknown";
        }
    }
}
