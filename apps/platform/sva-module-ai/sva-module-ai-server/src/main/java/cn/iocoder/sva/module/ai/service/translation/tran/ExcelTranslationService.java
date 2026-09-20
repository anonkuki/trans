package cn.iocoder.sva.module.ai.service.translation.tran;

import java.util.List;
import java.util.Map;

/**
 * Excel 文档翻译服务接口
 * <p>
 * 提供 Excel 文档翻译功能，支持 XLSX/XLSM 格式。
 * 对应 Python 项目 excel_core.py。
 */
public interface ExcelTranslationService {

    /**
     * Excel 翻译结果
     */
    class ExcelResult {
        private final int segments;
        private final Map<String, Object> qcReport;
        private final String qcTxtPath;
        private final List<ExcelPair> pairs;
        private final String outputPath;
        private final int errors;
        private final String error;

        public ExcelResult(int segments, Map<String, Object> qcReport,
                           String qcTxtPath, List<ExcelPair> pairs,
                           String outputPath, int errors, String error) {
            this.segments = segments;
            this.qcReport = qcReport;
            this.qcTxtPath = qcTxtPath;
            this.pairs = pairs;
            this.outputPath = outputPath;
            this.errors = errors;
            this.error = error;
        }

        public int getSegments() { return segments; }
        public Map<String, Object> getQcReport() { return qcReport; }
        public String getQcTxtPath() { return qcTxtPath; }
        public List<ExcelPair> getPairs() { return pairs; }
        public String getOutputPath() { return outputPath; }
        public int getErrors() { return errors; }
        public String getError() { return error; }
    }

    /**
     * Excel 翻译对
     */
    class ExcelPair {
        private String sheet;
        private String cell;
        private String source;
        private String target;
        private String status;

        public ExcelPair() {}

        public ExcelPair(String sheet, String cell, String source, String target, String status) {
            this.sheet = sheet;
            this.cell = cell;
            this.source = source;
            this.target = target;
            this.status = status;
        }

        public String getSheet() { return sheet; }
        public void setSheet(String sheet) { this.sheet = sheet; }
        public String getCell() { return cell; }
        public void setCell(String cell) { this.cell = cell; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    /**
     * 翻译 Excel 文档
     *
     * @param inputPath         输入文件路径
     * @param outputPath        输出文件路径
     * @param targetLanguage    目标语言
     * @param useGlossaryReplace 是否使用术语替换
     * @param glossaryMap       术语映射表（key=源术语, value=目标术语），可为 null
     * @param progressCallback  进度回调
     * @param textCallback      文本回调
     * @param enableQc          是否启用 QC
     * @return 翻译结果
     */
    ExcelResult processExcel(
            String inputPath,
            String outputPath,
            String targetLanguage,
            boolean useGlossaryReplace,
            Map<String, String> glossaryMap,
            ProgressCallback progressCallback,
            TextCallback textCallback,
            boolean enableQc
    );

    /**
     * 翻译 Excel 文档（使用默认参数）
     */
    default ExcelResult processExcel(String inputPath, String outputPath, String targetLanguage) {
        return processExcel(inputPath, outputPath, targetLanguage, true, null, null, null, false);
    }
}
