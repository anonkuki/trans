package cn.iocoder.sva.module.ai.service.translation.tran;

import java.util.List;
import java.util.Map;

/**
 * 质量检查服务接口
 * <p>
 * 提供文档质量检查功能，包括：
 * <ul>
 *   <li>确定性 QC：数字守恒、单位缺失、术语一致性、标签分隔符缺失</li>
 *   <li>LLM QC：使用 LLM 检查翻译质量问题</li>
 *   <li>一致性扫描：组名、日期、剂量等的一致性</li>
 *   <li>流利度扫描：语法、流畅性检查</li>
 * </ul>
 */
public interface QcService {

    /**
     * QC 检查结果
     */
    class QcReport {
        private String version;
        private Map<String, Object> stats;
        private List<Map<String, Object>> issues;
        private String docTxtPath;

        public QcReport() {}

        public QcReport(String version, Map<String, Object> stats, List<Map<String, Object>> issues) {
            this.version = version;
            this.stats = stats;
            this.issues = issues;
        }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public Map<String, Object> getStats() { return stats; }
        public void setStats(Map<String, Object> stats) { this.stats = stats; }
        public List<Map<String, Object>> getIssues() { return issues; }
        public void setIssues(List<Map<String, Object>> issues) { this.issues = issues; }
        public String getDocTxtPath() { return docTxtPath; }
        public void setDocTxtPath(String docTxtPath) { this.docTxtPath = docTxtPath; }
    }

    /**
     * 执行完整的 QC 检查
     *
     * @param srcPath 源文件路径
     * @param tgtPath 目标文件路径
     * @param glossary 术语库
     * @return QC 报告
     */
    QcReport qcCheck(String srcPath, String tgtPath, Map<String, String> glossary);

    /**
     * 运行确定性 QC 检查
     *
     * @param srcPath 源文件路径
     * @param tgtPath 目标文件路径
     * @param glossary 术语库
     * @return QC 报告
     */
    QcReport runDeterministicQc(String srcPath, String tgtPath, Map<String, String> glossary);

    /**
     * 运行一致性扫描
     *
     * @param srcPath 源文件路径
     * @param tgtPath 目标文件路径
     * @param glossary 术语库
     * @return QC 报告
     */
    QcReport runConsistencyScan(String srcPath, String tgtPath, Map<String, String> glossary);

    /**
     * 运行流利度扫描（LLM）
     *
     * @param tgtPath 目标文件路径
     * @return QC 报告
     */
    QcReport runFluencyScan(String tgtPath);
}
