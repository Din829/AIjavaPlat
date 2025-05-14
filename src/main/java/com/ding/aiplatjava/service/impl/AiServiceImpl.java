package com.ding.aiplatjava.service.impl;

import com.ding.aiplatjava.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * AiService 的实现类。
 * 使用 Spring AI ChatModel 进行 AI 调用 (针对 Spring AI 1.0.0-M8 版本)。
 * 当前版本将使用全局配置的API Key。
 */
@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    private final ChatModel chatModel; // 使用通过构造函数注入的全局ChatModel

    @Autowired
    public AiServiceImpl(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 对给定的文本进行摘要。
     *
     * @param textToSummarize 需要摘要的原始文本。
     * @param userApiKey      用户提供的API Key (当前版本中此参数未使用，AI调用将使用全局配置的Key)。
     * @return AI 模型生成的摘要文本。
     * @throws RuntimeException 如果调用 AI 服务时发生错误。
     */
    @Override
    public String summarizeText(String textToSummarize, String userApiKey) {
        if (userApiKey != null && !userApiKey.isEmpty()) {
            log.info("接收到用户提供的API Key，但在当前AiService实现中，将使用全局配置的API Key进行AI调用。");
        } else {
            log.info("用户未提供API Key，将使用全局配置的API Key进行AI调用。");
        }

        String userPrompt = "请总结以下内容，限制在400字以内：\n\n" + textToSummarize;

        OpenAiChatOptions requestOptions = OpenAiChatOptions.builder().build();
        // 模型等通常在Spring Boot的application.properties中为全局ChatModel配置，此处不再重复设置以避免冲突或覆盖
        // 例如: spring.ai.openai.chat.options.model=gpt-4o

        Prompt prompt = new Prompt(userPrompt, requestOptions);

        try {
            log.debug("正在使用全局配置的ChatModel和提示调用 AI 模型 (M8 Version)...");
            ChatResponse response = this.chatModel.call(prompt);

            Generation result = response.getResult();
            if (result != null && result.getOutput() != null) {
                if (result.getOutput() instanceof AssistantMessage assistantMessage) {
                    // 对于Spring AI 1.0.0-M8, AssistantMessage可能没有直接的getContent()或getMessage()方法来获取纯文本。
                    // .toString() 是最后的手段，它通常会包含一些元数据。
                    String summary = assistantMessage.toString();
                    log.info("成功生成 AI 摘要 (M8)。AssistantMessage.toString() 返回: {}", summary);
                    // 注意: summary此时可能包含 "AssistantMessage{messageType=ASSISTANT, content=\'摘要内容...\'}" 之类的格式
                    // 后续可能需要解析此字符串以提取纯净的 content 部分，或者接受当前格式。
                    // 这是一个M8版本的典型处理方式，如果toString()返回的不是预期内容，需要进一步研究M8的API或考虑升级Spring AI。
                    return summary;
                } else {
                    log.warn("AI输出类型不是AssistantMessage: {}。尝试对输出对象调用toString()作为备选方案。", result.getOutput().getClass().getName());
                    String fallbackContent = result.getOutput().toString();
                    log.info("AI 摘要 (M8, fallback, output.toString())。内容: {}", fallbackContent);
                    return fallbackContent;
                }
            } else {
                log.warn("AI 服务返回了空结果或输出 (M8)。");
                return "AI服务未返回有效摘要内容。";
            }
        } catch (Exception e) {
            log.error("调用 AI 服务进行摘要时出错 (M8): {}", e.getMessage(), e);
            if (e.getMessage() != null && (e.getMessage().contains("AuthenticationException") || e.getMessage().contains("Unauthorized"))) {
                 throw new RuntimeException("AI服务认证失败，请检查全局配置的API Key是否有效或额度充足。(M8)", e);
            }
            throw new RuntimeException("文本摘要生成失败 (M8): " + e.getMessage(), e);
        }
    }
}