package cn.iocoder.sva.module.ai.service.translation.tran.context;

import lombok.extern.slf4j.Slf4j;

/**
 * 提示词上下文
 * <p>
 * 用于在翻译任务的异步执行过程中传递和缓存系统提示词，避免重复查询数据库
 */
@Slf4j
public class PromptContext {

    private static final InheritableThreadLocal<String> PROMPT_HOLDER = new InheritableThreadLocal<>();

    /**
     * 设置提示词
     *
     * @param prompt 系统提示词
     */
    public static void set(String prompt) {
        PROMPT_HOLDER.set(prompt);
        log.debug("[PromptContext] 设置提示词，长度: {}", prompt != null ? prompt.length() : 0);
    }

    /**
     * 获取提示词
     *
     * @return 系统提示词
     */
    public static String get() {
        String prompt = PROMPT_HOLDER.get();
        if (prompt != null) {
            log.debug("[PromptContext] 获取提示词");
        }
        return prompt;
    }

    /**
     * 清除提示词
     */
    public static void clear() {
        String prompt = PROMPT_HOLDER.get();
        PROMPT_HOLDER.remove();
        log.debug("[PromptContext] 清除提示词");
    }
}
