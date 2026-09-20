package cn.iocoder.sva.module.ai.framework.ai.core.model;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 基于 DashScope 原生 SDK 的 ChatModel 实现
 * 用于支持 Spring AI Alibaba 尚未支持的多模态功能（如 Qwen3 系列模型）
 * 
 * @author 科兴源码
 */
@Slf4j
public class DashScopeNativeChatModel implements ChatModel {
    
    private final String apiKey;
    private final String modelName;
    
    public DashScopeNativeChatModel(String apiKey, String modelName) {
        this.apiKey = apiKey;
        this.modelName = modelName;
    }
    
    @Override
    public ChatResponse call(Prompt prompt) {
        long startTime = System.currentTimeMillis();
        try {
            List<Message> messages = prompt.getInstructions();
            if (messages == null || messages.isEmpty()) {
                throw new IllegalArgumentException("消息列表不能为空");
            }
            
            log.info("[DashScopeNative] 开始调用，model={}, 消息数={}", modelName, messages.size());
            
            // 构建多模态消息列表
            List<MultiModalMessage> dashScopeMessages = new ArrayList<>();
            
            for (Message message : messages) {
                String content = extractMessageContent(message);
                if (content == null || content.isEmpty()) {
                    continue;
                }
                
                String role = getRoleValue(message);
                MultiModalMessage dashScopeMsg = MultiModalMessage.builder()
                        .role(role)
                        .content(Arrays.asList(
                                Collections.singletonMap("text", content)
                        ))
                        .build();
                dashScopeMessages.add(dashScopeMsg);
            }
            
            if (dashScopeMessages.isEmpty()) {
                throw new IllegalArgumentException("没有有效的消息内容");
            }
            
            // 构建请求参数
            MultiModalConversationParam param = MultiModalConversationParam.builder()
                    .apiKey(apiKey)
                    .model(modelName)
                    .messages(dashScopeMessages)  // 使用完整的消息列表
                    .build();
            
            log.info("[DashScopeNative] 发送请求到阿里云百炼，消息数={}", dashScopeMessages.size());
            
            // 调用 API - 使用多模态对话接口
            MultiModalConversation conv = new MultiModalConversation();
            MultiModalConversationResult result = conv.call(param);
            
            long callDuration = System.currentTimeMillis() - startTime;
            log.info("[DashScopeNative] API调用完成，耗时={}ms", callDuration);
            
            // 提取响应文本
            String responseText = extractResponseText(result);
            
            log.info("[DashScopeNative] 收到响应，内容长度={}", responseText != null ? responseText.length() : 0);
            
            // 构建 Spring AI 的 ChatResponse
            AssistantMessage assistantMessage = new AssistantMessage(responseText != null ? responseText : "");
            org.springframework.ai.chat.model.Generation springAiGeneration = 
                new org.springframework.ai.chat.model.Generation(assistantMessage);
            return new org.springframework.ai.chat.model.ChatResponse(
                    List.of(springAiGeneration)
            );
            
        } catch (ApiException | NoApiKeyException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[DashScopeNative] API调用失败，耗时={}ms, error={}", duration, e.getMessage(), e);
            throw new RuntimeException("Failed to call DashScope API: " + e.getMessage(), e);
        } catch (UploadFileException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[DashScopeNative] 文件上传异常，耗时={}ms", duration, e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[DashScopeNative] 未知异常，耗时={}ms", duration, e);
            throw new RuntimeException("DashScope调用异常: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从 Spring AI Message 中提取文本内容
     */
    private String extractMessageContent(Message message) {
        if (message == null) {
            return null;
        }
        return message.getText();
    }
    
    /**
     * 获取消息角色值
     */
    private String getRoleValue(Message message) {
        if (message instanceof SystemMessage) {
            return Role.SYSTEM.getValue();
        } else if (message instanceof UserMessage) {
            return Role.USER.getValue();
        } else if (message instanceof AssistantMessage) {
            return Role.ASSISTANT.getValue();
        } else {
            return Role.USER.getValue();  // 默认使用USER角色
        }
    }
    
    /**
     * 从 MultiModalConversationResult 中提取响应文本
     */
    private String extractResponseText(MultiModalConversationResult result) {
        if (result == null || result.getOutput() == null || result.getOutput().getChoices() == null) {
            return "";
        }
        
        // 尝试从 choices 中获取
        if (!result.getOutput().getChoices().isEmpty()) {
            var choice = result.getOutput().getChoices().get(0);
            if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
                // 多模态消息的 content 是一个列表
                var contentList = choice.getMessage().getContent();
                if (!contentList.isEmpty()) {
                    // 查找 text 类型的内容
                    for (var item : contentList) {
                        if (item.containsKey("text")) {
                            return item.get("text").toString();
                        }
                    }
                }
            }
        }
        
        return "";
    }
}
