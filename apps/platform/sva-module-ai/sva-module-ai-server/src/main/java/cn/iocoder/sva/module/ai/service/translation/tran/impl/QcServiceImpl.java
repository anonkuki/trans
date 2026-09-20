package cn.iocoder.sva.module.ai.service.translation.tran.impl;

import cn.iocoder.sva.module.ai.service.translation.tran.LlmClientService;
import cn.iocoder.sva.module.ai.service.translation.tran.QcService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 质量检查服务实现
 * <p>
 * 对应 Python 项目 deterministic_qc.py + qc_extras.py
 */
@Slf4j
@Service
public class QcServiceImpl implements QcService {

    private final LlmClientService llmClient;

    /** 数字匹配正则 */
    private static final List<Pattern> NUMBER_PATTERNS = Arrays.asList(
            Pattern.compile("\\b\\d{1,3}(?:,\\d{3})+(?:\\.\\d+)?\\b"),  // 12,345.67
            Pattern.compile("\\b\\d+\\.\\d+\\b"),                      // 37.5
            Pattern.compile("\\b\\d+/\\d+\\b"),                       // 1/4
            Pattern.compile("\\b\\d+:\\d+\\b"),                       // 1:4
            Pattern.compile("\\b\\d+(?:\\.\\d+)?\\s*%\\b"),           // 10%
            Pattern.compile("\\b\\d+\\b")                             // 10
    );

    /** 单位别名映射 */
    private static final Map<String, List<String>> UNIT_ALIASES = new LinkedHashMap<>();
    static {
        UNIT_ALIASES.put("℃", Arrays.asList("℃", "°C", "Celsius", "degrees Celsius"));
        UNIT_ALIASES.put("%", Arrays.asList("%", "percent", "percentage"));
        UNIT_ALIASES.put("mg/mL", Arrays.asList("mg/mL", "mg / mL", "mg·mL^-1"));
        UNIT_ALIASES.put("mg", Arrays.asList("mg"));
        UNIT_ALIASES.put("mL", Arrays.asList("mL"));
        UNIT_ALIASES.put("μg", Arrays.asList("μg", "ug", "mcg"));
        UNIT_ALIASES.put("U", Arrays.asList("U", "Units", "units"));
    }

    /** 一致性检查正则 */
    private static final Pattern RE_GROUP = Pattern.compile(
            "\\b(?:Treatment|Study|Trial|Placebo|Control|Active|Dose|Cohort|Arm|Group)\\s+[A-Za-z]?\\d+\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern RE_DAY = Pattern.compile("\\bDay\\s*\\d+\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern RE_DOSE = Pattern.compile("\\b\\d+\\s*(?:mg/mL|mg|mL|μg|ug|mcg|U)\\b");
    private static final Pattern RE_VACC = Pattern.compile(
            "\\b(?:Dose\\s*\\d+|Injection\\s*\\d+|Shot\\s*\\d+)\\b", Pattern.CASE_INSENSITIVE);

    public QcServiceImpl(LlmClientService llmClient) {
        this.llmClient = llmClient;
    }

    // ===================== 主入口 =====================

    @Override
    public QcReport qcCheck(String srcPath, String tgtPath, Map<String, String> glossary) {
        List<Map<String, Object>> allIssues = new ArrayList<>();

        // 1. 确定性 QC（本地计算，快速）
        QcReport detReport = runDeterministicQc(srcPath, tgtPath, glossary);
        allIssues.addAll(detReport.getIssues() != null ? detReport.getIssues() : Collections.emptyList());

        // 2. 一致性扫描（本地计算，快速）
        QcReport conReport = runConsistencyScan(srcPath, tgtPath, glossary);
        allIssues.addAll(conReport.getIssues() != null ? conReport.getIssues() : Collections.emptyList());

        // 3. 和 4. 并行执行两个 LLM 相关的 QC 检查（耗时操作）
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // 提交流利度扫描任务
            Future<QcReport> fluencyFuture = executor.submit(() -> {
                try {
                    return runFluencyScan(tgtPath);
                } catch (Exception e) {
                    log.warn("流利度扫描失败: {}", e.getMessage());
                    return new QcReport("extras.v1", Map.of("issues_count", 0), Collections.emptyList());
                }
            });

            // 提交 LLM QC 任务
            Future<QcReport> llmQcFuture = executor.submit(() -> {
                try {
                    return runLlmQc(srcPath, tgtPath);
                } catch (Exception e) {
                    log.warn("LLM QC 失败: {}", e.getMessage());
                    return new QcReport("qc.v1", Map.of("issues_count", 0), Collections.emptyList());
                }
            });

            // 等待两个任务完成并收集结果
            QcReport fluReport = fluencyFuture.get(5, TimeUnit.MINUTES);
            QcReport llmReport = llmQcFuture.get(5, TimeUnit.MINUTES);

            allIssues.addAll(fluReport.getIssues() != null ? fluReport.getIssues() : Collections.emptyList());
            allIssues.addAll(llmReport.getIssues() != null ? llmReport.getIssues() : Collections.emptyList());

        } catch (TimeoutException e) {
            log.error("QC 检查超时: {}", e.getMessage());
        } catch (Exception e) {
            log.error("并行 QC 检查失败: {}", e.getMessage());
        } finally {
            executor.shutdown();
        }

        // 构建最终报告
        Map<String, Object> stats = new HashMap<>();
        stats.put("issues_count", allIssues.size());

        QcReport report = new QcReport("qc.v2", stats, allIssues);

        // 写入报告文件
        writeReportFiles(report, srcPath, tgtPath);

        return report;
    }

    // ===================== 确定性 QC =====================

    @Override
    public QcReport runDeterministicQc(String srcPath, String tgtPath, Map<String, String> glossary) {
        List<Map<String, Object>> issues = new ArrayList<>();

        try (XWPFDocument srcDoc = new XWPFDocument(new FileInputStream(srcPath));
             XWPFDocument tgtDoc = new XWPFDocument(new FileInputStream(tgtPath))) {

            List<XWPFParagraph> srcParas = srcDoc.getParagraphs();
            List<XWPFParagraph> tgtParas = tgtDoc.getParagraphs();

            int n = Math.min(srcParas.size(), tgtParas.size());
            for (int i = 0; i < n; i++) {
                String s = srcParas.get(i).getText().trim();
                String t = tgtParas.get(i).getText().trim();

                if (s.isEmpty() && t.isEmpty()) continue;
                String pid = "para_" + i;

                // 数字守恒
                List<String> sNums = extractNumbers(s);
                List<String> tNums = extractNumbers(t);
                List<String> missing = new ArrayList<>();
                for (String num : sNums) {
                    if (!tNums.contains(num)) {
                        missing.add(num);
                    }
                }
                if (!missing.isEmpty()) {
                    Map<String, Object> issue = new LinkedHashMap<>();
                    issue.put("id", pid);
                    issue.put("category", "number_mismatch");
                    issue.put("severity", "high");
                    issue.put("src", s);
                    issue.put("tgt", t);
                    issue.put("evidence", "missing numbers: " + String.join(", ", missing));
                    issue.put("source", "deterministic");
                    issues.add(issue);
                }

                // 单位保留
                List<String> unitMiss = checkUnitMissing(s, t);
                if (!unitMiss.isEmpty()) {
                    Map<String, Object> issue = new LinkedHashMap<>();
                    issue.put("id", pid);
                    issue.put("category", "unit_mismatch");
                    issue.put("severity", "medium");
                    issue.put("src", s);
                    issue.put("tgt", t);
                    issue.put("evidence", "missing units: " + String.join(", ", unitMiss));
                    issue.put("source", "deterministic");
                    issues.add(issue);
                }

                // 术语一致性
                List<String> gv = checkGlossaryViolation(s, t, glossary);
                if (!gv.isEmpty()) {
                    Map<String, Object> issue = new LinkedHashMap<>();
                    issue.put("id", pid);
                    issue.put("category", "glossary_inconsistency");
                    issue.put("severity", "medium");
                    issue.put("src", s);
                    issue.put("tgt", t);
                    issue.put("evidence", String.join("; ", gv));
                    issue.put("source", "deterministic");
                    issues.add(issue);
                }

                // 标签分隔符缺失
                if (checkLabelSepMissing(s, t)) {
                    Map<String, Object> issue = new LinkedHashMap<>();
                    issue.put("id", pid);
                    issue.put("category", "label_separator_missing");
                    issue.put("severity", "low");
                    issue.put("src", s);
                    issue.put("tgt", t);
                    issue.put("evidence", "源段含冒号但译段前80字符未见冒号，仅提示不修复");
                    issue.put("source", "deterministic");
                    issues.add(issue);
                }
            }

        } catch (Exception e) {
            log.error("确定性 QC 失败: {}", e.getMessage());
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("issues_count", issues.size());
        return new QcReport("det.v1", stats, issues);
    }

    // ===================== 一致性扫描 =====================

    @Override
    public QcReport runConsistencyScan(String srcPath, String tgtPath, Map<String, String> glossary) {
        List<Map<String, Object>> issues = new ArrayList<>();

        try (XWPFDocument tgtDoc = new XWPFDocument(new FileInputStream(tgtPath))) {
            List<String> texts = new ArrayList<>();
            for (XWPFParagraph p : tgtDoc.getParagraphs()) {
                texts.add(p.getText() != null ? p.getText() : "");
            }

            // 收集变体
            Map<String, List<int[]>> vGroup = collectVariants(texts, RE_GROUP);
            Map<String, List<int[]>> vDay = collectVariants(texts, RE_DAY);
            Map<String, List<int[]>> vDose = collectVariants(texts, RE_DOSE);
            Map<String, List<int[]>> vVacc = collectVariants(texts, RE_VACC);

            // 检测不一致
            issues.addAll(detectInconsistency(vGroup));
            issues.addAll(detectInconsistency(vDay));
            issues.addAll(detectInconsistency(vVacc));

            // 剂量单位规范化检查
            Map<String, List<String>> unitNorm = new LinkedHashMap<>();
            for (String k : vDose.keySet()) {
                String k2 = k.replace("μg", "ug").replace("mcg", "ug");
                String norm = k2.replaceAll("\\d+", "X");
                unitNorm.computeIfAbsent(norm, x -> new ArrayList<>()).add(k);
            }
            for (Map.Entry<String, List<String>> entry : unitNorm.entrySet()) {
                Set<String> uniq = new LinkedHashSet<>(entry.getValue());
                if (uniq.size() >= 2) {
                    Map<String, Object> issue = new LinkedHashMap<>();
                    issue.put("id", "global");
                    issue.put("category", "consistency");
                    issue.put("severity", "low");
                    issue.put("evidence", "Inconsistent unit spellings: " + String.join("; ", uniq));
                    issue.put("suggestion", "Normalize microgram to 'μg' or 'mcg' consistently");
                    issue.put("source", "deterministic");
                    issues.add(issue);
                }
            }

        } catch (Exception e) {
            log.error("一致性扫描失败: {}", e.getMessage());
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("issues_count", issues.size());
        return new QcReport("extras.v1", stats, issues);
    }

    // ===================== 流利度扫描 =====================

    @Override
    public QcReport runFluencyScan(String tgtPath) {
        List<Map<String, Object>> issues = new ArrayList<>();

        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(tgtPath))) {
            List<String> texts = new ArrayList<>();
            for (XWPFParagraph p : doc.getParagraphs()) {
                texts.add(p.getText() != null ? p.getText() : "");
            }

            // 分块处理
            List<List<int[]>> chunks = chunkParagraphs(texts, 7000);

            String sysPrompt = "You are a professional scientific copy editor. " +
                    "Read the following English paragraphs and find only clear fluency/coherence/style problems.\n" +
                    "- Do NOT rewrite the text. Report problems with short evidence and a concise suggestion.\n" +
                    "- Focus on grammar errors, broken sentences, awkward phrasing, tense/voice conflicts.\n" +
                    "- Output JSON array of issues with fields: id, category='fluency', severity, evidence, suggestion.\n" +
                    "- If no issues, return [].";

            for (List<int[]> chunk : chunks) {
                StringBuilder payload = new StringBuilder("[");
                boolean first = true;
                for (int[] item : chunk) {
                    int idx = item[0];
                    String t = texts.get(idx);
                    if (t != null && !t.trim().isEmpty()) {
                        if (!first) payload.append(",");
                        payload.append("{\"id\":\"para_").append(idx).append("\",\"tgt\":")
                                .append(escapeJson(t)).append("}");
                        first = false;
                    }
                }
                payload.append("]");

                if (payload.length() <= 2) continue;

                try {
                    LlmClientService.TranslateResult result = llmClient.translateOnce(payload.toString(), "English");
                    String content = result.getContent();
                    if (content != null && !content.isEmpty()) {
                        List<Map<String, Object>> parsed = parseJsonArray(content);
                        for (Map<String, Object> it : parsed) {
                            it.put("category", "fluency");
                            it.put("source", "llm");
                            issues.add(it);
                        }
                    }
                } catch (Exception e) {
                    log.debug("流利度扫描 LLM 调用失败: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("流利度扫描失败: {}", e.getMessage());
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("issues_count", issues.size());
        return new QcReport("extras.v1", stats, issues);
    }

    // ===================== LLM QC =====================

    private QcReport runLlmQc(String srcPath, String tgtPath) {
        List<Map<String, Object>> issues = new ArrayList<>();

        try (XWPFDocument srcDoc = new XWPFDocument(new FileInputStream(srcPath));
             XWPFDocument tgtDoc = new XWPFDocument(new FileInputStream(tgtPath))) {

            List<XWPFParagraph> srcParas = srcDoc.getParagraphs();
            List<XWPFParagraph> tgtParas = tgtDoc.getParagraphs();

            int n = Math.min(Math.min(srcParas.size(), tgtParas.size()), 200);

            StringBuilder pairs = new StringBuilder("[");
            for (int i = 0; i < n; i++) {
                if (i > 0) pairs.append(",");
                pairs.append("{\"id\":\"para_").append(i).append("\",\"src\":")
                        .append(escapeJson(srcParas.get(i).getText()))
                        .append(",\"tgt\":")
                        .append(escapeJson(tgtParas.get(i).getText()))
                        .append("}");
            }
            pairs.append("]");

            String sysPrompt = "你是临床文件质量审校助手，请严格检查源文(src)与译文(tgt)在数字、比例、CI、单位、术语一致性方面的问题。\n" +
                    "禁止改写译文。只输出符合 JSON Schema 的结果。\n" +
                    "输出JSON格式：{'version':'qc.v1','stats':{'issues_count':N},'issues':[{'id':..,'category':..,'severity':..,'src':..,'tgt':..,'evidence':..}]}";

            String payload = pairs.toString();
            if (payload.length() > 8000) {
                payload = payload.substring(0, 8000);
            }

            LlmClientService.TranslateResult result = llmClient.translateOnce(payload, "English");
            String content = result.getContent();

            if (content != null && !content.isEmpty()) {
                Map<String, Object> parsed = parseJsonObject(content);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> llmIssues = (List<Map<String, Object>>) parsed.get("issues");
                if (llmIssues != null) {
                    for (Map<String, Object> it : llmIssues) {
                        it.put("source", "llm");
                        issues.add(it);
                    }
                }
            }

        } catch (Exception e) {
            log.error("LLM QC 失败: {}", e.getMessage());
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("issues_count", issues.size());
        return new QcReport("qc.v1", stats, issues);
    }

    // ===================== 辅助方法 =====================

    private List<String> extractNumbers(String s) {
        Set<String> found = new LinkedHashSet<>();
        for (Pattern p : NUMBER_PATTERNS) {
            Matcher m = p.matcher(s != null ? s : "");
            while (m.find()) {
                found.add(m.group().trim());
            }
        }
        return new ArrayList<>(found);
    }

    private List<String> checkUnitMissing(String src, String tgt) {
        List<String> issues = new ArrayList<>();
        String s = src != null ? src : "";
        String t = tgt != null ? tgt : "";
        for (Map.Entry<String, List<String>> entry : UNIT_ALIASES.entrySet()) {
            boolean okSrc = false;
            for (String alias : entry.getValue()) {
                if (s.contains(alias)) { okSrc = true; break; }
            }
            if (okSrc) {
                boolean okTgt = false;
                for (String alias : entry.getValue()) {
                    if (t.contains(alias)) { okTgt = true; break; }
                }
                if (!okTgt) {
                    issues.add(entry.getKey());
                }
            }
        }
        return issues;
    }

    private List<String> checkGlossaryViolation(String src, String tgt, Map<String, String> glossary) {
        List<String> issues = new ArrayList<>();
        if (glossary == null) return issues;
        String s = src != null ? src : "";
        String t = (tgt != null ? tgt : "").toLowerCase();
        for (Map.Entry<String, String> entry : glossary.entrySet()) {
            String cn = entry.getKey();
            String en = entry.getValue();
            if (cn != null && s.contains(cn)) {
                if (en != null && !t.contains(en.toLowerCase())) {
                    issues.add("'" + cn + "'→应为 '" + en + "'");
                }
            }
        }
        return issues;
    }

    private boolean checkLabelSepMissing(String src, String tgt) {
        String headSrc = (src != null ? src : "").substring(0, Math.min(20, src != null ? src.length() : 0));
        String headTgt = (tgt != null ? tgt : "").substring(0, Math.min(80, tgt != null ? tgt.length() : 0));
        boolean hasLabel = headSrc.contains(":") || headSrc.contains("：");
        boolean missSep = !headTgt.contains(":") && !headTgt.contains("：");
        return hasLabel && missSep;
    }

    private Map<String, List<int[]>> collectVariants(List<String> texts, Pattern pattern) {
        Map<String, List<int[]>> m = new LinkedHashMap<>();
        for (int i = 0; i < texts.size(); i++) {
            Matcher matcher = pattern.matcher(texts.get(i) != null ? texts.get(i) : "");
            while (matcher.find()) {
                String raw = matcher.group().trim();
                String key = raw.toLowerCase().replace("  ", " ");
                m.computeIfAbsent(key, k -> new ArrayList<>()).add(new int[]{i});
            }
        }
        return m;
    }

    private List<Map<String, Object>> detectInconsistency(Map<String, List<int[]>> variants) {
        List<Map<String, Object>> issues = new ArrayList<>();
        Map<String, List<String>> groups = new LinkedHashMap<>();

        for (String k : variants.keySet()) {
            String canon = k.replaceAll("\\s+", "")
                    .toLowerCase()
                    .replaceAll("^(treatment|study|trial)", "");
            groups.computeIfAbsent(canon, x -> new ArrayList<>()).add(k);
        }

        for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
            if (entry.getValue().size() >= 2) {
                Map<String, Object> issue = new LinkedHashMap<>();
                issue.put("id", "global");
                issue.put("category", "consistency");
                issue.put("severity", "medium");
                issue.put("evidence", "Inconsistent labels for same concept: " + String.join("; ", entry.getValue()));
                issue.put("source", "deterministic");
                issues.add(issue);
            }
        }
        return issues;
    }

    private List<List<int[]>> chunkParagraphs(List<String> texts, int maxChars) {
        List<List<int[]>> chunks = new ArrayList<>();
        List<int[]> cur = new ArrayList<>();
        int curLen = 0;

        for (int i = 0; i < texts.size(); i++) {
            String t = texts.get(i) != null ? texts.get(i) : "";
            int add = t.length() + 10;
            if (!cur.isEmpty() && curLen + add > maxChars) {
                chunks.add(cur);
                cur = new ArrayList<>();
                curLen = 0;
            }
            cur.add(new int[]{i});
            curLen += add;
        }
        if (!cur.isEmpty()) {
            chunks.add(cur);
        }
        return chunks;
    }

    private String escapeJson(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }

    private List<Map<String, Object>> parseJsonArray(String content) {
        try {
            int start = content.indexOf('[');
            int end = content.lastIndexOf(']');
            if (start >= 0 && end >= start) {
                String json = content.substring(start, end + 1);
                // 简单解析，实际应使用 Jackson
                return new ArrayList<>();
            }
        } catch (Exception e) {
            // ignore
        }
        return new ArrayList<>();
    }

    private Map<String, Object> parseJsonObject(String content) {
        try {
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start >= 0 && end >= start) {
                String json = content.substring(start, end + 1);
                // 简单解析，实际应使用 Jackson
                return new LinkedHashMap<>();
            }
        } catch (Exception e) {
            // ignore
        }
        return new LinkedHashMap<>();
    }

    private void writeReportFiles(QcReport report, String srcPath, String tgtPath) {
        try {
            Path logDir = Paths.get("logs");
            Files.createDirectories(logDir);

            // 写入 TXT 报告
            String srcBase = Paths.get(srcPath).getFileName().toString();
            if (srcBase.contains(".")) {
                srcBase = srcBase.substring(0, srcBase.lastIndexOf('.'));
            }
            String txtName = srcBase + "qcreport.txt";
            Path txtPath = Paths.get(tgtPath).getParent().resolve(txtName);

            StringBuilder sb = new StringBuilder();
            sb.append(Paths.get(srcPath).getFileName()).append(" — QC Issues (")
                    .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .append(")\n");
            sb.append("=".repeat(86)).append("\n");

            if (report.getIssues() != null) {
                for (Map<String, Object> it : report.getIssues()) {
                    sb.append("[").append(it.getOrDefault("category", "")).append("]\n");
                    sb.append("  evidence: ").append(it.getOrDefault("evidence", "")).append("\n");
                    if (it.containsKey("suggestion")) {
                        sb.append("  suggestion: ").append(it.get("suggestion")).append("\n");
                    }
                    if (it.containsKey("src")) {
                        sb.append("  src: ").append(it.get("src")).append("\n");
                    }
                    if (it.containsKey("tgt")) {
                        sb.append("  tgt: ").append(it.get("tgt")).append("\n");
                    }
                    sb.append("\n");
                }
            }

            Files.writeString(txtPath, sb.toString());
            report.setDocTxtPath(txtPath.toString());
            log.info("QC 报告已写入: {}", txtPath);

        } catch (Exception e) {
            log.error("写入 QC 报告失败: {}", e.getMessage());
        }
    }
}
