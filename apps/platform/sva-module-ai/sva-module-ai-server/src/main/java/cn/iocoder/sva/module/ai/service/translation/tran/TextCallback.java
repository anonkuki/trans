package cn.iocoder.sva.module.ai.service.translation.tran;

/**
 * 翻译文本回调接口
 * <p>
 * 用于报告每段翻译结果的函数式接口，支持 Lambda 表达式。
 */
@FunctionalInterface
public interface TextCallback {

    /**
     * 报告翻译结果
     *
     * @param source     原文
     * @param translated 译文
     * @param status     翻译状态（如：完全命中、部分命中、未命中、[ERROR] xxx）
     */
    void onText(String source, String translated, String status);
}
