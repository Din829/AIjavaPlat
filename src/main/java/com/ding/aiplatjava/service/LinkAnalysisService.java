package com.ding.aiplatjava.service;

import java.util.Map;

/**
 * 链接分析服务接口
 * 提供链接类型识别和元数据提取功能
 */
public interface LinkAnalysisService {

    /**
     * 检测链接的内容类型
     * 
     * @param url 要分析的URL
     * @return 内容类型：WEBPAGE 或 VIDEO
     */
    String detectContentType(String url);

    /**
     * 提取视频元数据
     * 仅当链接被识别为视频时调用
     * 
     * @param url 视频URL
     * @return 包含视频元数据的Map，包括title、description、duration等
     */
    Map<String, Object> extractVideoMetadata(String url);

    /**
     * 验证URL的可访问性
     * 
     * @param url 要验证的URL
     * @return 是否可访问
     */
    boolean isUrlAccessible(String url);

    /**
     * 获取网页标题
     * 仅当链接被识别为网页时调用
     * 
     * @param url 网页URL
     * @return 网页标题
     */
    String extractWebPageTitle(String url);
}
