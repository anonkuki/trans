package cn.iocoder.sva.module.ai.service.translation.tran.model;

import lombok.Data;

/**
 * 翻译请求参数模型
 * <p>
 * 对应 Python FastAPI 中 POST /api/translate 的 Form 参数。
 * 在 Spring Boot 中可以作为 @RequestParam 逐个接收，
 * 也可以封装为此对象后使用 @ModelAttribute 绑定。
 * <p>
 * 使用示例（Controller中）：
 * <pre>
 * &#64;PostMapping("/api/translate")
 * public Map&lt;String, String&gt; translate(TranslateRequest req) { ... }
 * </pre>
 */
@Data
public class TranslateRequest {

    /**
     * 目标语言
     * <p>
     * 如 "en"（英语）、"ja"（日语）、"ko"（韩语）等
     */
    private String targetLang;

    /**
     * 是否启用术语库替换
     * <p>
     * 开启后，翻译前会先对文档中匹配到的术语进行预替换，
     * 确保 LLM 翻译时能正确处理专业术语。
     */
    private boolean useGlossaryReplace;

    /**
     * 是否使用严格格式模式
     * <p>
     * 开启后，会要求 LLM 严格保持原文格式（如段落结构、标点等），
     * 适用于对格式敏感的文档（如法律合同、技术规范）。
     */
    private boolean strictFormat;

    /**
     * 是否启用翻译对比
     * <p>
     * 开启后，生成的下载文件中会包含原文与译文的对照版本，
     * 方便用户检查翻译质量。
     */
    private boolean enableComparison;

    /**
     * 是否启用质检（QC）
     * <p>
     * 开启后，翻译完成后会自动执行质量检查，
     * 检查漏译、数字错误、术语不一致等问题。
     * qcIssues 字段会记录发现的问题数量。
     */
    private boolean enableQc;

    public TranslateRequest() {
        // 默认值设置
        this.useGlossaryReplace = true;
        this.strictFormat = false;
        this.enableComparison = false;
        this.enableQc = false;
    }
}
