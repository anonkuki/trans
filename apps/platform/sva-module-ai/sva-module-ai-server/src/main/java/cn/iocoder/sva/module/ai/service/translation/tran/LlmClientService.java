package cn.iocoder.sva.module.ai.service.translation.tran;

import cn.iocoder.sva.module.ai.service.translation.tran.model.UsageStats;
import org.springframework.ai.chat.model.ChatModel;

/**
 * LLM客户端服务接口
 * <p>
 * 提供翻译调用、缓存、质量检查等功能。
 * 对应 Python 项目 llm_client.py 中的 DeepSeekClient。
 */
public interface LlmClientService {

    /**
     * 翻译调用类型
     */
    enum CallKind {
        /** 普通翻译 */
        NORMAL,
        /** 严格翻译（temperature=0） */
        STRICT,
        /** 润色（保留术语） */
        POLISH
    }

    /**
     * 翻译结果
     */
    class TranslateResult {
        private final String content;
        private final UsageStats usage;
        private final String status;

        public TranslateResult(String content, UsageStats usage, String status) {
            this.content = content;
            this.usage = usage;
            this.status = status;
        }

        public String getContent() {
            return content;
        }

        public UsageStats getUsage() {
            return usage;
        }

        public String getStatus() {
            return status;
        }
    }

    /**
     * 执行单次翻译
     *
     * @param text           待翻译文本
     * @param targetLanguage 目标语言
     * @return 翻译结果（content + usage）
     */
    TranslateResult translateOnce(String text, String targetLanguage);

    /**
     * 执行单次翻译（支持自定义角色）
     *
     * @param text           待翻译文本
     * @param targetLanguage 目标语言
     * @param roleId         聊天角色ID（可选，用于自定义翻译提示词）
     * @return 翻译结果（content + usage）
     */
    TranslateResult translateOnce(String text, String targetLanguage, Long roleId);

    /**
     * 执行严格翻译（temperature=0）
     *
     * @param text           待翻译文本
     * @param targetLanguage 目标语言
     * @return 翻译结果
     */
    TranslateResult translateOnceStrict(String text, String targetLanguage);

    /**
     * 执行严格翻译（temperature=0，支持自定义角色）
     *
     * @param text           待翻译文本
     * @param targetLanguage 目标语言
     * @param roleId         聊天角色ID（可选，用于自定义翻译提示词）
     * @return 翻译结果
     */
    TranslateResult translateOnceStrict(String text, String targetLanguage, Long roleId);

    /**
     * 润色文本（保留术语）
     *
     * @param text           待润色文本
     * @param targetLanguage 目标语言
     * @return 润色结果
     */
    TranslateResult polishPreserveTerms(String text, String targetLanguage);

    /**
     * 带缓存的翻译调用
     *
     * @param kind           调用类型
     * @param text           待翻译文本
     * @param targetLanguage 目标语言
     * @return 翻译结果
     */
    TranslateResult cachedCall(CallKind kind, String text, String targetLanguage);

    /**
     * 清洗或重试翻译
     * <p>
     * 检查输出是否包含不良模式或违反目标语言约束，
     * 如果有问题则使用严格模式重试。
     *
     * @param text           原文
     * @param targetLanguage 目标语言
     * @param rawContent     初次翻译结果
     * @param usage          初次调用用量
     * @return 清洗后的结果
     */
    TranslateResult sanitizeOrRetry(String text, String targetLanguage,
                                    String rawContent, UsageStats usage);

    /**
     * 获取翻译缓存大小
     *
     * @return 缓存条目数
     */
    int getCacheSize();

    /**
     * 清空翻译缓存
     */
    void clearCache();

    /**
     * 创建带有指定 ChatModel 的 LLM 客户端实例
     * <p>
     * 用于支持根据 modelId 动态选择 AI 模型进行翻译
     *
     * @param chatModel Spring AI ChatModel 实例
     * @return 新的 LlmClientService 实例
     */
    LlmClientService withChatModel(ChatModel chatModel);
}
