package com.ding.aiplatjava.service.impl;

import com.ding.aiplatjava.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AiService 的实现类。
 * 使用 Spring AI ChatModel 进行 AI 调用 (针对 Spring AI 1.0.0-M8 版本)。
 * 当前版本将使用全局配置的API Key。
 */
@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);
    // Regex to extract content from AssistantMessage.toString() like: "...textContent=YOUR_CONTENT, metadata=..."
    // It captures characters after "textContent=" until a comma followed by " metadata=" or the closing bracket of the toString representation.
    private static final Pattern TEXT_CONTENT_PATTERN = Pattern.compile("textContent=([^,]*?)(?:, metadata=|\\]$)");
    // A slightly more robust pattern that captures until the next known major field or end of string.
    // This pattern attempts to capture everything after "textContent=" up to ", metadata=" or the final closing bracket "]"
    private static final Pattern ROBUST_TEXT_CONTENT_PATTERN = Pattern.compile("textContent=(.*?)(?:, metadata=|\\]$)");
    private final ChatModel chatModel; // 使用通过构造函数注入的全局ChatModel

    @Autowired
    public AiServiceImpl(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 对给定的文本进行摘要。
     *
     * @param textToSummarize 需要摘要的原始文本。
     * @param userApiKey      用户提供的API Key。AI调用将尝试使用此Key，如果提供的话。
     * @return AI 模型生成的摘要文本。
     * @throws RuntimeException 如果调用 AI 服务时发生错误。
     */
    @Override
    public String summarizeText(String textToSummarize, String userApiKey) {
        if (userApiKey != null && !userApiKey.isEmpty()) {
            log.info("接收到用户提供的API Key，将尝试通过OpenAiChatOptions的HTTP Headers使用此Key进行AI调用。");
        } else {
            // This case should ideally be prevented by the controller if a user API key is mandatory for summarization.
            // If no userApiKey is provided, and we proceed, it will use the globally configured key by default.
            log.warn("用户API Key未提供或为空。AI调用将依赖全局配置的Key（如果存在）。");
            // Decide on behavior: throw exception, or allow fallback to global key (current implicit behavior if options are empty).
            // For now, let's assume the controller ensures a key is present if user-specific keys are enforced.
        }

        String userPrompt = "请总结以下内容，限制在400字以内：\n\n" + textToSummarize;

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder();

        // If a userApiKey is provided, attempt to set it via Authorization header
        if (userApiKey != null && !userApiKey.isEmpty()) {
            optionsBuilder.httpHeaders(Map.of("Authorization", "Bearer " + userApiKey));
            log.debug("OpenAiChatOptions: Authorization header set for user API key.");
        }
        // else, the optionsBuilder remains empty regarding httpHeaders, relying on the ChatModel's default (global key)

        // Retain any other global options if necessary, e.g., model, temperature, if they aren't already
        // part of the autoconfigured chatModel's default options that we want to override.
        // For Spring AI M8, the global model & temperature are typically part of the injected chatModel's configuration.
        // If we wanted to override them here as well, we could add:
        // optionsBuilder.model("gpt-4.1"); // Example, if it needs to be dynamic or different from global default
        // optionsBuilder.temperature(0.7f); // Example

        OpenAiChatOptions requestOptions = optionsBuilder.build();

        Prompt prompt = new Prompt(userPrompt, requestOptions);

        try {
            log.debug("正在调用 AI 模型 (M8 Version) 使用以下选项: Model={}, Temperature={}", 
                      requestOptions.getModel(), requestOptions.getTemperature()); // Log actual options being used
            ChatResponse response = this.chatModel.call(prompt);

            Generation result = response.getResult();
            if (result != null && result.getOutput() != null) {
                Object output = result.getOutput();
                String outputString = output.toString();
                log.debug("AI Service raw output.toString(): {}", outputString);

                Matcher matcher = ROBUST_TEXT_CONTENT_PATTERN.matcher(outputString);
                if (matcher.find()) {
                    String extractedContent = matcher.group(1).trim();
                    // Remove potential surrounding quotes if they are part of the capture but not the actual content
                    if (extractedContent.startsWith("'") && extractedContent.endsWith("'") && extractedContent.length() > 1) {
                        extractedContent = extractedContent.substring(1, extractedContent.length() - 1);
                    }
                    log.info("成功从AI响应中提取文本内容: {}", extractedContent);
                    return extractedContent;
                } else {
                    log.warn("无法从AI响应的toString()输出中通过正则表达式提取textContent。将返回完整的toString()内容。 Raw: {}", outputString);
                    return outputString; // Fallback to full toString if regex fails
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