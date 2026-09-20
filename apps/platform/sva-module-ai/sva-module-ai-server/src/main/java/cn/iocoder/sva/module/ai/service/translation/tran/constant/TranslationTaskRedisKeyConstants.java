package cn.iocoder.sva.module.ai.service.translation.tran.constant;

/**
 * 翻译任务 Redis Key 常量类
 */
public class TranslationTaskRedisKeyConstants {

    /**
     * 翻译任务信息前缀
     * KEY 格式: translation_task:{taskId}
     * VALUE 格式: TaskInfo JSON 字符串
     */
    public static final String TRANSLATION_TASK = "translation_task:%s";

    /**
     * 翻译任务列表集合（用于管理所有活跃任务）
     * KEY 格式: translation_tasks:active
     * VALUE 格式: Set<String> taskId 集合
     */
    public static final String TRANSLATION_TASKS_ACTIVE = "translation_tasks:active";

    /**
     * 翻译任务创建时间哈希表
     * KEY 格式: translation_task:start_times
     * FIELD 格式: {taskId}
     * VALUE 格式: Instant 时间戳字符串
     */
    public static final String TRANSLATION_TASK_START_TIMES = "translation_task:start_times";

    /**
     * 获取翻译任务的 Redis Key
     *
     * @param taskId 任务ID
     * @return Redis Key
     */
    public static String getTranslationTaskKey(String taskId) {
        return String.format(TRANSLATION_TASK, taskId);
    }
}