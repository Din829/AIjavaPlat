package com.ding.aiplatjava.service;

import java.io.IOException;

/**
 * 网页内容服务接口。
 * 提供获取和解析网页文本内容的功能。
 */
public interface WebContentService {

    /**
     * 从指定的 URL 提取主要的文本内容。
     *
     * @param url 目标网页的 URL 字符串。
     * @return 提取出的网页主要文本内容。
     * @throws IOException 如果发生网络连接错误或解析错误。
     */
    String extractTextFromUrl(String url) throws IOException;

} 