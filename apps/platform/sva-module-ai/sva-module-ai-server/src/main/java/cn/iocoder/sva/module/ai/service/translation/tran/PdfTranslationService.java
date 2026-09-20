package cn.iocoder.sva.module.ai.service.translation.tran;

import java.util.List;
import java.util.Map;

/**
 * PDF 文档翻译服务接口
 * <p>
 * 提供 PDF 文档翻译功能。
 * 采用先将 PDF 转换为 Word，然后使用现有 Word 翻译程序的思路。
 * 对应 Python 项目 pdf_core.py。
 */
public interface PdfTranslationService {

    /**
     * PDF 翻译结果
     */
    class PdfResult {
        private final int segments;
        private final Map<String, Object> qcReport;
        private final String qcTxtPath;
        private final List<Map<String, String>> pairs;
        private final String outputPath;
        private final String contrastPath;
        private final String error;

        public PdfResult(int segments, Map<String, Object> qcReport,
                         String qcTxtPath, List<Map<String, String>> pairs,
                         String outputPath, String contrastPath, String error) {
            this.segments = segments;
            this.qcReport = qcReport;
            this.qcTxtPath = qcTxtPath;
            this.pairs = pairs;
            this.outputPath = outputPath;
            this.contrastPath = contrastPath;
            this.error = error;
        }

        public int getSegments() { return segments; }
        public Map<String, Object> getQcReport() { return qcReport; }
        public String getQcTxtPath() { return qcTxtPath; }
        public List<Map<String, String>> getPairs() { return pairs; }
        public String getOutputPath() { return outputPath; }
        public String getContrastPath() { return contrastPath; }
        public String getError() { return error; }
    }

    /**
     * 翻译 PDF 文档
     *
     * @param inputPath         输入文件路径
     * @param outputPath        输出文件路径（会自动转为 .docx）
     * @param targetLanguage    目标语言
     * @param useGlossaryReplace 是否使用术语替换
     * @param glossaryMap       术语映射表（key=源术语, value=目标术语），可为 null
     * @param progressCallback  进度回调
     * @param textCallback      文本回调
     * @param strictFormat      是否严格保留格式
     * @param enableQc          是否启用 QC
     * @param enableComparison  是否启用双语对照
     * @return 翻译结果
     */
    PdfResult processPdf(
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
     * 翻译 PDF 文档（使用默认参数）
     */
    default PdfResult processPdf(String inputPath, String outputPath, String targetLanguage) {
        return processPdf(inputPath, outputPath, targetLanguage, true, null, null, null, false, false, false);
    }
}
