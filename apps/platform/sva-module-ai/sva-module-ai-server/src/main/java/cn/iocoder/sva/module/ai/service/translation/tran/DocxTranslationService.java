package cn.iocoder.sva.module.ai.service.translation.tran;

import java.util.List;
import java.util.Map;

/**
 * Word文档翻译服务接口
 * <p>
 * 提供 Word 文档翻译功能，支持 DOCX 格式。
 * 对应 Python 项目 core.py 中的 process_document 函数。
 */
public interface DocxTranslationService {

    /**
     * 翻译结果
     */
    class TranslationResult {
        /** 翻译段落数 */
        private final int segments;
        /** QC 报告 */
        private final Map<String, Object> qcReport;
        /** QC 文本文件路径 */
        private final String qcTxtPath;
        /** 翻译对列表 */
        private final List<TranslationPair> pairs;
        /** 输出文件路径 */
        private final String outputPath;
        /** 对照文件路径 */
        private final String contrastPath;
        /** 错误数 */
        private final int errors;
        /** 错误信息 */
        private final String error;

        public TranslationResult(int segments, Map<String, Object> qcReport,
                                 String qcTxtPath, List<TranslationPair> pairs,
                                 String outputPath, int errors) {
            this(segments, qcReport, qcTxtPath, pairs, outputPath, null, errors, null);
        }

        public TranslationResult(int segments, Map<String, Object> qcReport,
                                 String qcTxtPath, List<TranslationPair> pairs,
                                 String outputPath, String contrastPath, int errors, String error) {
            this.segments = segments;
            this.qcReport = qcReport;
            this.qcTxtPath = qcTxtPath;
            this.pairs = pairs;
            this.outputPath = outputPath;
            this.contrastPath = contrastPath;
            this.errors = errors;
            this.error = error;
        }

        public int getSegments() { return segments; }
        public Map<String, Object> getQcReport() { return qcReport; }
        public String getQcTxtPath() { return qcTxtPath; }
        public List<TranslationPair> getPairs() { return pairs; }
        public String getOutputPath() { return outputPath; }
        public String getContrastPath() { return contrastPath; }
        public int getErrors() { return errors; }
        public String getError() { return error; }
    }

    /**
     * 翻译对
     */
    class TranslationPair {
        private String source;
        private String target;
        private String status;
        private boolean addToGlossary;

        public TranslationPair() {}

        public TranslationPair(String source, String target, String status, boolean addToGlossary) {
            this.source = source;
            this.target = target;
            this.status = status;
            this.addToGlossary = addToGlossary;
        }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public boolean isAddToGlossary() { return addToGlossary; }
        public void setAddToGlossary(boolean addToGlossary) { this.addToGlossary = addToGlossary; }
    }

    /**
     * 翻译 Word 文档
     *
     * @param inputPath         输入文件路径
     * @param outputPath        输出文件路径
     * @param targetLanguage    目标语言（如 "Chinese"、"English"）
     * @param useGlossaryReplace 是否使用术语替换
     * @param glossaryMap       术语映射表（key=源术语, value=目标术语），可为 null
     * @param progressCallback  进度回调（可为 null）
     * @param textCallback      文本回调（可为 null）
     * @param strictFormat      是否保持严格格式
     * @param enableQc          是否启用 QC
     * @param enableComparison  是否启用对照模式
     * @return 翻译结果
     */
    TranslationResult processDocument(
            String inputPath,
            String outputPath,
            String targetLanguage,
            boolean useGlossaryReplace,
            Map<String, String> glossaryMap,
            ProgressCallback progressCallback,
            TextCallback textCallback,
            boolean strictFormat,
            boolean enableQc,
            boolean enableComparison
    );

    /**
     * 翻译 Word 文档（使用默认参数）
     */
    default TranslationResult processDocument(String inputPath, String outputPath, String targetLanguage) {
        return processDocument(inputPath, outputPath, targetLanguage,
                true, null, null, null, false, false, false);
    }
}
