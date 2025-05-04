package com.ding.aiplatjava.service.impl;

import com.ding.aiplatjava.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage; // Import AssistantMessage
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

/**
 * AiService 的实现类。
 * 使用 Spring AI ChatModel 进行 AI 调用。
 */
@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    private final ChatModel chatModel;//创建ChatModel实例

    @Autowired
    public AiServiceImpl(ChatModel chatModel) {//注入ChatModel实例
        this.chatModel = chatModel;
    }

    /**
     * 对给定的文本进行摘要。
     *
     * @param textToSummarize 需要摘要的原始文本。
     * @param userApiKey      用于调用 AI 服务的用户 API Key。
     * @return AI 模型生成的摘要文本。
     * @throws RuntimeException 如果调用 AI 服务时发生错误。
     */
    @Override
    public String summarizeText(String textToSummarize, String userApiKey) {
        // 注意：暂时忽略用户API密钥，使用全局配置的API密钥
        log.info("Summarizing text using global API key configuration (ignoring user API key)");

        // 1. 构建用户提示
        String userPrompt = "请总结以下内容，限制在400字以内：\n\n" + textToSummarize;

        // 2. 创建运行时选项，使用全局配置的API密钥
        OpenAiChatOptions requestOptions = OpenAiChatOptions.builder()
                // 可以按需添加其他运行时选项，例如模型:
                .model("gpt-4o") // 如果需要覆盖 application.properties 中的默认模型
                .build();

        // 3. 创建 Prompt，包含用户提示和运行时选项
        Prompt prompt = new Prompt(userPrompt, requestOptions);

        try {
            // 4. 调用 AI 模型
            log.debug("正在使用提示调用 AI 模型...");
            ChatResponse response = chatModel.call(prompt);//调用AI模型

            // 5. 提取并返回结果 (使用 1.0.0 GA API)
            Generation result = response.getResult();
            if (result != null && result.getOutput() != null) {
                // M8 API 差异: result.getOutput() 似乎是 AssistantMessage
                // 如果没有 getContent() 方法,尝试通过 AssistantMessage API 访问内容
                log.debug("原始输出类型: {}", result.getOutput().getClass().getName());
                log.debug("原始输出 toString(): {}", result.getOutput().toString());

                if (result.getOutput() instanceof AssistantMessage assistantMessage) {
                    // M8 版本 AssistantMessage 可能没有 getContent()，直接使用 toString()
                    String summary = assistantMessage.toString();
                    log.info("使用 AssistantMessage.toString() 成功生成 AI 摘要。内容: {}", summary);
                    return summary;
                } else {
                     log.warn("输出不是 AssistantMessage: {}", result.getOutput().getClass());
                     // 作为最后的手段，尝试调用原始输出的 toString()
                     String fallbackContent = result.getOutput().toString();
                     log.warn("作为备选方案返回原始输出的 toString(): {}", fallbackContent);
                     return fallbackContent;
                }
            } else {
                log.warn("AI 服务返回了空结果或输出。");
                return ""; // 或抛出异常
            }

        } catch (Exception e) {
            log.error("调用 AI 服务进行摘要时出错", e);
            // 考虑重新抛出更具体的自定义异常
            throw new RuntimeException("文本摘要生成失败: " + e.getMessage(), e);
        }
    }
}