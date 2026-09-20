package cn.iocoder.sva.module.ai.service.translation.tran.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 翻译对模型
 * <p>
 * 对应 Python 项目中的翻译对字典：
 * <pre>{"原文": "...", "译文": "...", "状态": "...", "加入术语库": False}</pre>
 * <p>
 * 使用 @JsonProperty 注解确保 JSON 序列化/反序列化时使用中文字段名，
 * 与前端 API 保持一致。
 */
@Data
public class TranslationPair {

    /** 原文（源语言文本） */
    @JsonProperty("source")
    private String source;

    /** 译文（目标语言文本） */
    @JsonProperty("target")
    private String target;

    /**
     * 翻译状态
     * <p>
     * 可能的值参考 TranslationConstants 中的状态常量：
     * STATUS_OK（正常）、STATUS_MISS（缺译）、STATUS_NO_NEED（无需翻译）等
     */
    @JsonProperty("status")
    private String status;

    /**
     * 是否加入术语库
     * <p>
     * 用户可标记某翻译对为术语，后续翻译时自动匹配替换
     */
    @JsonProperty("addToGlossary")
    private boolean addToGlossary;

    public TranslationPair() {
    }

    public TranslationPair(String source, String target, String status, boolean addToGlossary) {
        this.source = source;
        this.target = target;
        this.status = status;
        this.addToGlossary = addToGlossary;
    }
}
