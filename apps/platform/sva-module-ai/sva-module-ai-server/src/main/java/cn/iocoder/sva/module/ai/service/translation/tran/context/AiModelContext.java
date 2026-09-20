package cn.iocoder.sva.module.ai.service.translation.tran.context;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;

/**
 * AI 模型调用上下文
 * <p>
 * 用于在翻译任务中传递 ChatModel 和模型元数据（如 modelId、modelName）
 * 基于 InheritableThreadLocal 实现线程父子关系的传递
 */
@Slf4j
@Data
public class AiModelContext {

    private static final InheritableThreadLocal<ModelInfo> MODEL_HOLDER = new InheritableThreadLocal<>();

    /**
     * 模型信息
     */
    @Data
    public static class ModelInfo {
        private Long modelId;
        private String modelName;
        private String modelCode;  // 实际调用的模型代码，如 deepseek-v3
        private ChatModel chatModel;
    }

    /**
     * 设置当前线程的模型信息
     *
     * @param modelInfo 模型信息
     */
    public static void set(ModelInfo modelInfo) {
        MODEL_HOLDER.set(modelInfo);
        log.debug("[AiModelContext] 设置模型信息: modelId={}, modelName={}, modelCode={}", 
                modelInfo != null ? modelInfo.getModelId() : null,
                modelInfo != null ? modelInfo.getModelName() : null,
                modelInfo != null ? modelInfo.getModelCode() : null);
    }

    /**
     * 获取当前线程的模型信息
     *
     * @return 模型信息，如果未设置则返回 null
     */
    public static ModelInfo get() {
        ModelInfo modelInfo = MODEL_HOLDER.get();
        if (modelInfo != null) {
            log.debug("[AiModelContext] 获取模型信息: modelId={}, modelCode={}", 
                    modelInfo.getModelId(), modelInfo.getModelCode());
        }
        return modelInfo;
    }

    /**
     * 清除当前线程的模型信息
     * <p>
     * 重要：必须在任务完成后调用，防止内存泄漏
     */
    public static void clear() {
        ModelInfo modelInfo = MODEL_HOLDER.get();
        MODEL_HOLDER.remove();
        log.debug("[AiModelContext] 清除模型信息: modelId={}", 
                modelInfo != null ? modelInfo.getModelId() : null);
    }

    /**
     * 判断当前线程是否设置了模型信息
     *
     * @return true 如果已设置
     */
    public static boolean hasModel() {
        return MODEL_HOLDER.get() != null;
    }
}
