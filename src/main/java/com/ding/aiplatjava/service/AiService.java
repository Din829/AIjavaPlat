package com.ding.aiplatjava.service;

/**
 * AI 服务接口。
 * 提供调用底层 AI 模型进行处理的功能，例如文本摘要。
 */
public interface AiService {

    /**
     * 对给定的文本进行摘要。
     *
     * @param textToSummarize 需要摘要的原始文本。
     * @param userApiKey      用于调用 AI 服务的用户 API Key。
     * @return AI 模型生成的摘要文本。
     * @throws RuntimeException 如果调用 AI 服务时发生错误。
     */
    String summarizeText(String textToSummarize, String userApiKey);

} 