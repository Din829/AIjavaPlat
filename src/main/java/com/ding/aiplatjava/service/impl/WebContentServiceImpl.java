package com.ding.aiplatjava.service.impl;

import com.ding.aiplatjava.service.WebContentService;//导入WebContentService接口，用于处理网页内容
import org.jsoup.Jsoup;//导入Jsoup库，用于解析和操作HTML
import org.jsoup.nodes.Document;//导入Document类，表示HTML文档
import org.slf4j.Logger;//导入Logger类，用于日志记录
import org.slf4j.LoggerFactory;//导入LoggerFactory类，用于日志记录
import org.springframework.stereotype.Service;//导入Service注解，用于标记该类为Spring服务组件

import java.io.IOException;//导入IOException类，表示输入输出异常
import java.net.SocketTimeoutException;//导入SocketTimeoutException类，表示连接超时异常 

/**
 * WebContentService 的实现类。
 * 使用 Jsoup 库来获取和解析网页内容。
 */
@Service
public class WebContentServiceImpl implements WebContentService {

    private static final Logger log = LoggerFactory.getLogger(WebContentServiceImpl.class);//创建日志记录器
    private static final int TIMEOUT_MILLIS = 10000; // 10 秒连接和读取超时

    /**
     * 从指定的 URL 提取主要的文本内容。
     * <p>
     * 它会尝试获取网页的 HTML，然后提取 body 元素的文本。
     * 对常见的连接错误和超时进行了处理。
     *
     * @param url 目标网页的 URL 字符串。
     * @return 提取出的网页主要文本内容。如果无法获取或解析，可能返回空字符串或部分内容。
     * @throws IOException 如果发生无法处理的网络或解析错误。
     */
    @Override
    public String extractTextFromUrl(String url) throws IOException {
        log.info("尝试从URL提取文本内容: {}", url);
        try {
            // 设置超时和 User-Agent
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(TIMEOUT_MILLIS)//设置超时时间
                    .get();//获取网页内容

            // 简单地提取 body 的文本内容，可以根据需要进行更复杂的提取逻辑
            // 例如，移除脚本、样式，或者只提取特定标签 (<p>, <h1> 等)
            String text = doc.body().text();
            log.info("成功从URL提取文本内容: {}。文本长度: {}", url, text.length());
            return text;

        } catch (SocketTimeoutException e) {
            log.error("连接URL超时: {}", url, e);
            throw new IOException("连接超时: " + url, e);
        } catch (IOException e) {
            log.error("连接或解析URL时出错: {}", url, e);
            throw new IOException("处理URL时出错: " + url, e);
        } catch (Exception e) {
            // 捕获其他可能的运行时异常
            log.error("处理URL时发生意外错误: {}", url, e);
            throw new IOException("处理URL时发生意外错误: " + url, e);
        }
    }
} 