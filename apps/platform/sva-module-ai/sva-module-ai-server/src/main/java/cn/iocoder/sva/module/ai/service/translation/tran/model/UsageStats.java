package cn.iocoder.sva.module.ai.service.translation.tran.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 用量统计模型
 * <p>
 * 对应 Python 项目 types.py 中的 UsageStats 数据类。
 * 用于跟踪 LLM 调用的 token 用量，并计算对应的人民币费用。
 * <p>
 * 计费逻辑（基于 DeepSeek 定价）：
 * <ul>
 *   <li>输入缓存命中：0.2 元 / 百万 token</li>
 *   <li>输入缓存未命中：2.0 元 / 百万 token</li>
 *   <li>输出 token：3.0 元 / 百万 token</li>
 * </ul>
 * 如果 promptCacheMissTokens 为 0（某些模型不返回此字段），
 * 则使用 promptTokens 作为输入 token 的回退值。
 */
public class UsageStats {

    /** 提示词 token 数（输入） */
    private long promptTokens;

    /** 补全 token 数（输出） */
    private long completionTokens;

    /** 总 token 数 */
    private long totalTokens;

    /** 提示词缓存命中 token 数 — 命中的部分按更低价计费 */
    private long promptCacheHitTokens;

    /** 提示词缓存未命中 token 数 — 未命中部分按标准价计费 */
    private long promptCacheMissTokens;

    public UsageStats() {
    }

    // ===================== Getters & Setters =====================

    public long getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(long promptTokens) {
        this.promptTokens = promptTokens;
    }

    public long getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(long completionTokens) {
        this.completionTokens = completionTokens;
    }

    public long getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(long totalTokens) {
        this.totalTokens = totalTokens;
    }

    public long getPromptCacheHitTokens() {
        return promptCacheHitTokens;
    }

    public void setPromptCacheHitTokens(long promptCacheHitTokens) {
        this.promptCacheHitTokens = promptCacheHitTokens;
    }

    public long getPromptCacheMissTokens() {
        return promptCacheMissTokens;
    }

    public void setPromptCacheMissTokens(long promptCacheMissTokens) {
        this.promptCacheMissTokens = promptCacheMissTokens;
    }

    // ===================== 核心方法 =====================

    /**
     * 累加另一个 UsageStats 的所有字段到当前对象
     * <p>
     * 用于多次 LLM 调用后汇总总用量。
     *
     * @param other 另一个 UsageStats 实例，其字段值将被累加到当前对象
     */
    public void add(UsageStats other) {
        if (other == null) {
            return;
        }
        this.promptTokens += other.promptTokens;
        this.completionTokens += other.completionTokens;
        this.totalTokens += other.totalTokens;
        this.promptCacheHitTokens += other.promptCacheHitTokens;
        this.promptCacheMissTokens += other.promptCacheMissTokens;
    }

    /**
     * 从 LLM 返回的 usage Map 中累加各字段
     * <p>
     * LLM API（如 DeepSeek）返回的 JSON 中包含 usage 对象，反序列化后为 Map。
     * 本方法遍历 Map 中与当前对象同名的字段，将值累加到对应属性上。
     * 如果字段不存在或值无法转为整数，则跳过该字段（不抛异常）。
     *
     * @param usage LLM 返回的 usage 映射，key 为字段名（如 "prompt_tokens"），value 为对应数值
     */
    public void addFromMap(Map<String, Object> usage) {
        if (usage == null) {
            return;
        }
        // 字段名映射：LLM返回的snake_case → Java的camelCase
        Map<String, Runnable> fieldMap = new HashMap<>();
        fieldMap.put("prompt_tokens", () -> this.promptTokens += toLong(usage.get("prompt_tokens")));
        fieldMap.put("completion_tokens", () -> this.completionTokens += toLong(usage.get("completion_tokens")));
        fieldMap.put("total_tokens", () -> this.totalTokens += toLong(usage.get("total_tokens")));
        fieldMap.put("prompt_cache_hit_tokens", () -> this.promptCacheHitTokens += toLong(usage.get("prompt_cache_hit_tokens")));
        fieldMap.put("prompt_cache_miss_tokens", () -> this.promptCacheMissTokens += toLong(usage.get("prompt_cache_miss_tokens")));

        for (Map.Entry<String, Runnable> entry : fieldMap.entrySet()) {
            if (usage.containsKey(entry.getKey())) {
                try {
                    entry.getValue().run();
                } catch (Exception e) {
                    // 转换失败则跳过该字段
                }
            }
        }
    }

    /**
     * 计算本次用量对应的人民币费用
     * <p>
     * 计费规则：
     * <ol>
     *   <li>输入缓存命中部分：promptCacheHitTokens × 0.2 / 1,000,000</li>
     *   <li>输入缓存未命中部分：如果 promptCacheMissTokens > 0 则用该值，
     *       否则回退使用 promptTokens（兼容不返回缓存字段的模型），
     *       乘以 2.0 / 1,000,000</li>
     *   <li>输出部分：completionTokens × 3.0 / 1,000,000</li>
     * </ol>
     * 最终结果四舍五入到小数点后6位。
     *
     * @return 人民币费用（元），精度6位小数
     */
    public double costCny() {
        // 缓存命中的输入 token 费用
        long inHit = this.promptCacheHitTokens;
        // 缓存未命中的输入 token — 如果模型未返回缓存未命中字段，则用 promptTokens 兜底
        long inMiss = this.promptCacheMissTokens > 0 ? this.promptCacheMissTokens : this.promptTokens;

        // 输入费用 = 命中部分×命中单价 + 未命中部分×未命中单价
        double costIn = (inHit / 1_000_000.0) * 0.2 + (inMiss / 1_000_000.0) * 2.0;
        // 输出费用 = 输出token数×输出单价
        double costOut = (this.completionTokens / 1_000_000.0) * 3.0;

        // 四舍五入到6位小数
        return Math.round((costIn + costOut) * 1_000_000.0) / 1_000_000.0;
    }

    /**
     * 将当前用量统计转换为 Map（便于 JSON 序列化或日志输出）
     *
     * @return 包含所有字段的 Map，key 为 snake_case 格式
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("prompt_tokens", promptTokens);
        map.put("completion_tokens", completionTokens);
        map.put("total_tokens", totalTokens);
        map.put("prompt_cache_hit_tokens", promptCacheHitTokens);
        map.put("prompt_cache_miss_tokens", promptCacheMissTokens);
        return map;
    }

    /**
     * 安全地将对象转为 long 值
     * <p>
     * LLM 返回的 usage 值可能是 Integer、Long 或 String 等类型，
     * 本方法统一处理转换，失败返回 0。
     *
     * @param value 待转换的值
     * @return 转换后的 long 值，转换失败返回 0
     */
    private long toLong(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
