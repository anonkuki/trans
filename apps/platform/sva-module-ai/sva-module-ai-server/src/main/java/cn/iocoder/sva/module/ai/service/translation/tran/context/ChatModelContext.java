package cn.iocoder.sva.module.ai.service.translation.tran.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;

/**
 * ChatModel 线程上下文
 * <p>
 * 用于在翻译任务中动态传递 ChatModel，避免硬编码依赖
 * 基于 InheritableThreadLocal 实现线程父子关系的传递
 */
@Slf4j
public class ChatModelContext {

    private static final InheritableThreadLocal<ChatModel> CHAT_MODEL_HOLDER = new InheritableThreadLocal<>();

    /**
     * 设置当前线程的 ChatModel
     *
     * @param chatModel ChatModel 实例
     */
    public static void set(ChatModel chatModel) {
        CHAT_MODEL_HOLDER.set(chatModel);
        log.debug("[ChatModelContext] 设置 ChatModel: {}", chatModel != null ? chatModel.getClass().getSimpleName() : "null");
    }

    /**
     * 获取当前线程的 ChatModel
     *
     * @return ChatModel 实例，如果未设置则返回 null
     */
    public static ChatModel get() {
        ChatModel chatModel = CHAT_MODEL_HOLDER.get();
        if (chatModel != null) {
            log.debug("[ChatModelContext] 获取 ChatModel: {}", chatModel.getClass().getSimpleName());
        }
        return chatModel;
    }

    /**
     * 清除当前线程的 ChatModel
     * <p>
     * 重要：必须在任务完成后调用，防止内存泄漏
     */
    public static void clear() {
        ChatModel chatModel = CHAT_MODEL_HOLDER.get();
        CHAT_MODEL_HOLDER.remove();
        log.debug("[ChatModelContext] 清除 ChatModel: {}", chatModel != null ? chatModel.getClass().getSimpleName() : "null");
    }

    /**
     * 判断当前线程是否设置了 ChatModel
     *
     * @return true 如果已设置
     */
    public static boolean hasChatModel() {
        return CHAT_MODEL_HOLDER.get() != null;
    }
}
