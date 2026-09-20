package cn.iocoder.sva.module.ai.service.translation.tran.impl;

import cn.iocoder.sva.module.ai.service.translation.tran.*;
import cn.iocoder.sva.module.ai.service.translation.tran.common.DocQualityChecker;
import cn.iocoder.sva.module.ai.service.translation.tran.common.DocxUtils;
import cn.iocoder.sva.module.ai.service.translation.tran.config.TransDocProperties;
import cn.iocoder.sva.module.ai.service.translation.tran.model.TranslationPair;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

/**
 * Word文档翻译服务实现
 * <p>
 * 核心特性：
 * <ul>
 *   <li>延迟写回 + 固定并发</li>
 *   <li>去重缓存</li>
 *   <li>弱边界术语优先替换</li>
 *   <li>支持严格格式模式</li>
 * </ul>
 */
@Slf4j
@Service
public class DocxTranslationServiceImpl implements DocxTranslationService {

    @Autowired
    private TransDocProperties properties;
    @Autowired
    private LlmClientService llmClient;
    @Autowired
    private GlossaryService glossaryService;
    @Autowired
    private QcService qcService;

    /** 文档写入锁 */
    private static final ReentrantLock DOC_WRITE_LOCK = new ReentrantLock();

    /** CJK 字符正则 */
    private static final Pattern CJK_PATTERN = Pattern.compile("[\\u4e00-\\u9fff]");
    /** 连续英文正则 */
    private static final Pattern ALPHA_SEQ6 = Pattern.compile("[A-Za-z]{6,}");
    /** 仅符号正则 */
    private static final Pattern ONLY_SYMBOLS = Pattern.compile("^[\\s\\W_]+$", Pattern.UNICODE_CHARACTER_CLASS);

    public DocxTranslationServiceImpl(TransDocProperties properties,
                                       LlmClientService llmClient,
                                       GlossaryService glossaryService,
                                       QcService qcService) {
        this.properties = properties;
        this.llmClient = llmClient;
        this.glossaryService = glossaryService;
        this.qcService = qcService;
    }

    @Override
    public TranslationResult processDocument(
            String inputPath,
            String outputPath,
            String targetLanguage,
            boolean useGlossaryReplace,
            Map<String, String> glossaryMap,
            ProgressCallback progressCallback,
            TextCallback textCallback,
            boolean strictFormat,
            boolean enableQc,
            boolean enableComparison) {

        String jobId = UUID.randomUUID().toString().substring(0, 8);
        long startTime = System.currentTimeMillis();

        int concurrency = Math.max(1, properties.getConcurrency());
        log.info("[{}] START file='{}' -> '{}', target='{}', strict={}, concurrency={}, enableComparison={}",
                jobId, inputPath, outputPath, targetLanguage, strictFormat, concurrency, enableComparison);

        List<TranslationPair> pairs = new CopyOnWriteArrayList<>();
        Map<String, TranslateResult> dedupCache = new ConcurrentHashMap<>();
        AtomicInteger errorCount = new AtomicInteger(0);
        String contrastOutputPath = null;

        try (FileInputStream fis = new FileInputStream(inputPath);
             XWPFDocument doc = new XWPFDocument(fis)) {
            
            // 获取所有段落
            List<XWPFParagraph> paragraphs = DocxUtils.getAllParagraphs(doc);

            // 筛选需要翻译的段落
            List<Integer> transIndices = new ArrayList<>();
            for (int i = 0; i < paragraphs.size(); i++) {
                String text = paragraphs.get(i).getText();
                if (text != null && !text.isBlank()) {
                    transIndices.add(i);
                }
            }

            int total = transIndices.size();
            AtomicInteger done = new AtomicInteger(0);

            // 使用传入的术语库
            Map<String, String> glossary;
            if (useGlossaryReplace && glossaryMap != null && !glossaryMap.isEmpty()) {
                glossary = new HashMap<>(glossaryMap);
                log.info("[{}] 使用传入的术语库，共 {} 条术语", jobId, glossary.size());
                
                // 打印术语库内容，用于调试
                log.info("[{}] ===== 传入术语库详情开始 =====", jobId);
                glossary.forEach((source, target) -> 
                    log.info("[{}][术语] '{}' -> '{}'", jobId, source, target)
                );
                log.info("[{}] ===== 传入术语库详情结束 =====", jobId);
            } else {
                glossary = new HashMap<>();
                log.info("[{}] 未提供术语库，使用空术语列表", jobId);
            }
            
            if (targetLanguage.toLowerCase().startsWith("chinese")) {
                // 翻译成中文，术语库需要反转（英文->中文）
                Map<String, String> reversed = new HashMap<>();
                for (Map.Entry<String, String> e : glossary.entrySet()) {
                    reversed.put(e.getValue(), e.getKey());
                }
                glossary = reversed;
            }

            Map<String, String> finalGlossary = glossary;

            // 并发处理
            ExecutorService executor = Executors.newFixedThreadPool(concurrency);
            List<Future<ParagraphResult>> futures = new ArrayList<>();

            for (int idx : transIndices) {
                futures.add(executor.submit(() -> processParagraph(
                        paragraphs, idx, targetLanguage, finalGlossary,
                        useGlossaryReplace, strictFormat, dedupCache, jobId)));
            }

            // 收集结果
            List<ParagraphResult> results = new ArrayList<>();
            for (Future<ParagraphResult> future : futures) {
                try {
                    ParagraphResult res = future.get();
                    results.add(res);

                    int current = done.incrementAndGet();
                    safeCallback(progressCallback, current, total,
                            String.format("段落 %d/%d 完成", current, total));

                    safeCallback(textCallback, res.source, res.translated, res.status);

                    pairs.add(new TranslationPair(res.source, res.translated, res.status, false));

                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    log.error("[{}] future_error: {}", jobId, e.getMessage(), e);  // 添加完整堆栈
                }
            }

            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    log.warn("[{}] 翻译任务超时，强制关闭线程池", jobId);
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
                log.error("[{}] 等待线程池关闭时被中断", jobId, e);
            }

            // 按索引排序
            results.sort(Comparator.comparingInt(r -> r.idx));

            // 如果需要对照模式，先创建对照文档的副本
            if (enableComparison) {
                contrastOutputPath = outputPath.replaceAll("\\.(?=[^.]+$)", "_双语对照$0");
                Files.copy(Paths.get(inputPath), Paths.get(contrastOutputPath), 
                          java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                log.info("[{}] 创建对照文档副本: {}", jobId, contrastOutputPath);
            }
            
            // 关闭原文档，释放资源
            doc.close();
                        
            // 重新加载文档用于写回（避免并发导致的内部状态污染）
            try (FileInputStream rewriteFis = new FileInputStream(inputPath);
                 XWPFDocument rewriteDoc = new XWPFDocument(rewriteFis)) {
                            
                List<XWPFParagraph> rewriteParagraphs = DocxUtils.getAllParagraphs(rewriteDoc);
            
                // 写回文档 - 纯英文版本
                DOC_WRITE_LOCK.lock();
                try {
                    for (ParagraphResult res : results) {
                        if ("skip".equals(res.mode) || "error".equals(res.mode)) {
                            continue;
                        }
            
                        XWPFParagraph p = rewriteParagraphs.get(res.idx);
            
                        // 纯英文版本：直接写入译文
                        if (strictFormat && "strict_groups".equals(res.mode)) {
                            applyStrictFormatTranslation(p, res.groupResults);
                        } else {
                            DocxUtils.applyTranslationToParagraph(p, res.translated);
                        }
                    }
            
                    // 设置字体
                    if (targetLanguage.toLowerCase().startsWith("english")) {
                        DocxUtils.setFontAll(rewriteDoc, "Times New Roman");
                    } else if (targetLanguage.toLowerCase().startsWith("chinese")) {
                        DocxUtils.setFontAll(rewriteDoc, "SimSun");
                    }
            
                    // 表格自适应
                    DocxUtils.enableTableAutofit(rewriteDoc);
            
                    // 保存纯英文文档
                    Path outputDir = Paths.get(outputPath).getParent();
                    if (outputDir != null) {
                        Files.createDirectories(outputDir);
                    }
                    try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                        rewriteDoc.write(fos);
                    }
            
                    log.info("[{}] 纯英文文档已保存: {}", jobId, outputPath);
            
                } finally {
                    DOC_WRITE_LOCK.unlock();
                }
            
                // 写回对照版本文档
                if (enableComparison && contrastOutputPath != null) {
                    try (FileInputStream contrastFis = new FileInputStream(inputPath);
                         XWPFDocument contrastDoc = new XWPFDocument(contrastFis)) {
                                    
                        List<XWPFParagraph> contrastParagraphs = DocxUtils.getAllParagraphs(contrastDoc);
            
                        DOC_WRITE_LOCK.lock();
                        try {
                            for (ParagraphResult res : results) {
                                if ("skip".equals(res.mode) || "error".equals(res.mode)) {
                                    continue;
                                }
            
                                XWPFParagraph p = contrastParagraphs.get(res.idx);
            
                                // 对照版本：写入“原文\n译文”格式
                                String comparisonText = res.source + "\n" + res.translated;
                                DocxUtils.applyTranslationToParagraph(p, comparisonText);
                            }
            
                            // 设置字体
                            if (targetLanguage.toLowerCase().startsWith("english")) {
                                DocxUtils.setFontAll(contrastDoc, "Times New Roman");
                            } else if (targetLanguage.toLowerCase().startsWith("chinese")) {
                                DocxUtils.setFontAll(contrastDoc, "SimSun");
                            }
            
                            // 表格自适应
                            DocxUtils.enableTableAutofit(contrastDoc);
            
                            // 保存对照文档
                            try (FileOutputStream contrastFos = new FileOutputStream(contrastOutputPath)) {
                                contrastDoc.write(contrastFos);
                            }
            
                            log.info("[{}] 双语对照文档已保存: {}", jobId, contrastOutputPath);
            
                        } finally {
                            DOC_WRITE_LOCK.unlock();
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("[{}] 处理文档失败: {}", jobId, e.getMessage(), e);
            errorCount.incrementAndGet();
        }

        // QC 报告
        Map<String, Object> qcReport = new HashMap<>();
        String qcTxtPath = "";

        if (enableQc && Files.exists(Paths.get(outputPath))) {
            try {
                Map<String, String> glossary = glossaryMap != null ? new HashMap<>(glossaryMap) : new HashMap<>();
                QcService.QcReport report = qcService.qcCheck(inputPath, outputPath, glossary);
                qcReport.put("version", report.getVersion());
                qcReport.put("stats", report.getStats());
                qcReport.put("issues", report.getIssues());
                qcTxtPath = report.getDocTxtPath() != null ? report.getDocTxtPath() : "";
                log.info("[{}] QC 完成: issues={}", jobId,
                        report.getStats() != null ? report.getStats().get("issues_count") : 0);
            } catch (Exception e) {
                log.warn("[{}] QC 检查失败: {}", jobId, e.getMessage());
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[{}] END segments={} errors={} elapsed={:.2f}s",
                jobId, pairs.size(), errorCount.get(), elapsed / 1000.0);

        return new TranslationResult(
                pairs.size(), qcReport, qcTxtPath,
                pairs, outputPath, contrastOutputPath, errorCount.get(), null
        );
    }

    // ===================== 内部方法 =====================

    /**
     * 全局锁：保护所有 POI 对象的并发访问
     * Apache POI 不是线程安全的，即使是不同的段落对象也可能共享内部状态
     */
    private static final Object POI_LOCK = new Object();

    /**
     * 处理单个段落
     */
    private ParagraphResult processParagraph(
            List<XWPFParagraph> paragraphs,
            int idx,
            String targetLanguage,
            Map<String, String> glossary,
            boolean useGlossaryReplace,
            boolean strictFormat,
            Map<String, TranslateResult> dedupCache,
            String jobId) {

        try {
            XWPFParagraph p = paragraphs.get(idx);
            
            // 【关键修复】使用全局锁保护所有 POI 对象访问
            String src;
            List<DocxUtils.StyleGroup> groups = null;
            
            synchronized (POI_LOCK) {
                src = p.getText() != null ? p.getText() : "";
                
                // 如果是严格格式模式，在这里就提取样式组信息
                if (strictFormat) {
                    groups = DocxUtils.groupRunsByStyle(p);
                }
            }

            // 检查是否为目录段落
            if (DocxUtils.isTocFieldParagraph(p) && !DocxUtils.isTocHeadingText(p)) {
                return new ParagraphResult(idx, "skip", src, src, "目录跳过");
            }

            // 严格格式模式（groups 已经在锁内提取）
            if (strictFormat) {
                return processStrictFormatWithGroups(p, idx, src, groups, targetLanguage, glossary,
                        useGlossaryReplace, dedupCache);
            }

            // 普通翻译
            TranslateResult result = translateText(src, targetLanguage, glossary, useGlossaryReplace);
            
            // 只有非完全命中的结果才加入去重缓存
            // 完全命中的结果每次都应该重新从术语库获取，以支持术语实时更新
            if (!"完全命中".equals(result.getStatus())) {
                String cacheKey = "P:" + targetLanguage + ":" + src;
                dedupCache.put(cacheKey, result);
            } else {
                log.debug("[段落处理][idx={}] 完全命中术语，不加入去重缓存，支持术语实时更新", idx);
            }

            return new ParagraphResult(idx, "paragraph", src, result.getContent(), result.getStatus());
            
        } catch (Exception e) {
            log.error("[{}][processParagraph] 处理段落失败, idx={}, error={}", jobId, idx, e.getMessage(), e);
            throw e;  // 重新抛出，让外层捕获
        }
    }

    /**
     * 严格格式模式处理（接收已提取的 groups）
     */
    private ParagraphResult processStrictFormatWithGroups(
            XWPFParagraph p,
            int idx,
            String src,
            List<DocxUtils.StyleGroup> groups,
            String targetLanguage,
            Map<String, String> glossary,
            boolean useGlossaryReplace,
            Map<String, TranslateResult> dedupCache) {
        
        List<GroupResult> groupResults = new ArrayList<>();
        List<String> statusList = new ArrayList<>();

        for (DocxUtils.StyleGroup group : groups) {
            String gText = group.getMergedText();

            // 空白或仅符号
            if (gText.isBlank() || isOnlyBullets(gText)) {
                groupResults.add(new GroupResult(group.getStyleKey(), gText));
                statusList.add("无需翻译");
                continue;
            }

            // 按句子分割翻译
            String[] parts = DocxUtils.SENT_SPLIT_PATTERN.split(gText);
            List<String> translatedParts = new ArrayList<>();
            List<String> partStatuses = new ArrayList<>();

            for (String part : parts) {
                if (part.isBlank()) continue;
                
                // 翻译每个片段
                TranslateResult tr = translateText(part, targetLanguage, glossary, useGlossaryReplace);
                
                // 只有非完全命中的结果才加入去重缓存
                if (!"完全命中".equals(tr.getStatus())) {
                    String cacheKey = "S:" + targetLanguage + ":" + part;
                    dedupCache.put(cacheKey, tr);
                }
                
                translatedParts.add(tr.getContent());
                partStatuses.add(tr.getStatus());
            }

            String translated = String.join(" ", translatedParts);
            groupResults.add(new GroupResult(group.getStyleKey(), translated));
            statusList.add(mergeStatuses(partStatuses));
        }

        String mergedStatus = mergeStatuses(statusList);
        String mergedText = String.join("", groupResults.stream().map(g -> g.text).toList());

        return new ParagraphResult(idx, "strict_groups", src, mergedText, mergedStatus, groupResults);
    }

    /**
     * 严格格式模式处理（旧方法，保留用于兼容性）
     * @deprecated 请使用 processStrictFormatWithGroups
     */
    @Deprecated
    private ParagraphResult processStrictFormat(
            XWPFParagraph p,
            int idx,
            String src,
            String targetLanguage,
            Map<String, String> glossary,
            boolean useGlossaryReplace,
            Map<String, TranslateResult> dedupCache) {

        // 【已废弃】此方法不再使用，因为 POI 访问已移到全局锁内
        throw new UnsupportedOperationException("请使用 processStrictFormatWithGroups 方法");
    }

    /**
     * 翻译文本
     */
    private TranslateResult translateText(
            String text,
            String targetLanguage,
            Map<String, String> glossary,
            boolean useGlossaryReplace) {

        // 术语替换 - 每次都从传入的术语库中获取最新值，不使用缓存
        GlossaryService.ReplaceResult replaceResult = useGlossaryReplace
                ? glossaryService.replaceTermsMaxCover(text, glossary)
                : new GlossaryService.ReplaceResult(text, 0, text.replaceAll("\\s+", "").length());

        String replaced = replaceResult.getText();
        int covered = replaceResult.getCovered();
        int totalClean = replaceResult.getTotalClean();

        // 添加详细日志，帮助诊断问题
        if (useGlossaryReplace && glossary != null && !glossary.isEmpty()) {
            log.debug("[术语替换] 原文='{}', 替换后='{}', covered={}, totalClean={}, 术语库大小={}", 
                    text, replaced, covered, totalClean, glossary.size());
        }

        if (totalClean == 0) {
            return new TranslateResult(text, "未命中");
        }

        // 检查是否已经是目标语言
        if (isPureTarget(text, targetLanguage)) {
            return new TranslateResult(text, "无需翻译");
        }

        // 完全命中术语库 - 直接返回，不调用 LLM，也不使用缓存
        if (covered == totalClean) {
            log.info("[术语完全命中][不缓存] 原文='{}', 替换后='{}', covered={}, totalClean={}", 
                    text.substring(0, Math.min(50, text.length())), 
                    replaced.substring(0, Math.min(50, replaced.length())),
                    covered, totalClean);
            return new TranslateResult(replaced, "完全命中");
        }

        // 未命中术语库 - covered == 0
        if (covered == 0) {
            log.debug("[术语未命中][调用LLM] 原文='{}', totalClean={}", 
                    text.substring(0, Math.min(50, text.length())), totalClean);
        } else {
            // 部分命中术语库 - covered > 0 但 < totalClean
            log.debug("[术语部分命中][调用LLM] 原文='{}', covered={}, totalClean={}", 
                    text.substring(0, Math.min(50, text.length())), covered, totalClean);
        }
        
        LlmClientService.TranslateResult llmResult = llmClient.cachedCall(
                LlmClientService.CallKind.NORMAL, replaced, targetLanguage);

        // 清洗或重试
        LlmClientService.TranslateResult sanitized = llmClient.sanitizeOrRetry(
                replaced, targetLanguage, llmResult.getContent(), llmResult.getUsage());

        // 质量检查
        DocQualityChecker.QcResult qcResult = DocQualityChecker.check(replaced, sanitized.getContent());
        // 根据covered判断状态：0=未命中，>0=部分命中
        String status;
        if (qcResult.isOk()) {
            status = (covered == 0) ? "未命中" : "部分命中";
        } else {
            status = qcResult.getStatus();
        }

        return new TranslateResult(
                sanitized.getContent() != null ? sanitized.getContent() : text,
                status
        );
    }

    /**
     * 应用严格格式翻译
     */
    private void applyStrictFormatTranslation(XWPFParagraph p, List<GroupResult> groupResults) {
        List<DocxUtils.StyleGroup> groups = DocxUtils.groupRunsByStyle(p);

        for (int i = 0; i < Math.min(groups.size(), groupResults.size()); i++) {
            DocxUtils.StyleGroup sg = groups.get(i);
            GroupResult gr = groupResults.get(i);

            List<XWPFRun> runs = sg.getRuns();
            if (!runs.isEmpty()) {
                try {
                    // 确保文本不为null，避免POI内部错误
                    String textToSet = gr.text != null ? gr.text : "";
                    runs.get(0).setText(textToSet, 0);
                    for (int j = 1; j < runs.size(); j++) {
                        runs.get(j).setText("", 0);
                    }
                } catch (Exception e) {
                    log.warn("[严格格式翻译] 设置文本失败，跳过该段落: idx={}, error={}", 
                            i, e.getMessage());
                    // 如果设置失败，保持原文不变
                }
            }
        }
    }

    /**
     * 判断是否已经是目标语言
     */
    private boolean isPureTarget(String text, String targetLanguage) {
        if (text == null || text.isBlank()) return true;
        if (ONLY_SYMBOLS.matcher(text).matches()) return true;

        if (targetLanguage.toLowerCase().startsWith("english")) {
            return !CJK_PATTERN.matcher(text).find();
        }
        if (targetLanguage.toLowerCase().startsWith("chinese")) {
            return !ALPHA_SEQ6.matcher(text).find();
        }
        return false;
    }

    /**
     * 判断是否仅包含项目符号
     */
    private boolean isOnlyBullets(String text) {
        return text.chars().allMatch(ch ->
                DocxUtils.BULLET_TOKENS.indexOf(ch) >= 0 || Character.isWhitespace(ch));
    }

    /**
     * 合并状态
     */
    private String mergeStatuses(List<String> statuses) {
        List<String> filtered = new ArrayList<>();
        for (String s : statuses) {
            if (s != null && !s.equals("空白跳过") && !s.equals("无需翻译") && !s.equals("目录跳过")) {
                filtered.add(s);
            }
        }

        if (filtered.isEmpty()) return "无需翻译";

        boolean hasFull = filtered.contains("完全命中");
        boolean hasPartial = filtered.contains("部分命中");
        boolean hasMiss = filtered.stream().anyMatch(s -> s.equals("未命中") || s.startsWith("[ERROR]"));

        if (hasPartial || (hasFull && hasMiss)) return "部分命中";
        if (hasFull && !hasMiss) return "完全命中";
        return "未命中";
    }

    /**
     * 安全回调
     */
    private void safeCallback(ProgressCallback callback, int current, int total, String message) {
        if (callback != null) {
            try {
                callback.onProgress(current, total, message);
            } catch (Exception e) {
                log.warn("回调失败: {}", e.getMessage());
            }
        }
    }

    private void safeCallback(TextCallback callback, String source, String translated, String status) {
        if (callback != null) {
            try {
                callback.onText(source, translated, status);
            } catch (Exception e) {
                log.warn("回调失败: {}", e.getMessage());
            }
        }
    }

    // ===================== 内部类 =====================

    private static class ParagraphResult {
        final int idx;
        final String mode;
        final String source;
        final String translated;
        final String status;
        final List<GroupResult> groupResults;

        ParagraphResult(int idx, String mode, String source, String translated, String status) {
            this(idx, mode, source, translated, status, null);
        }

        ParagraphResult(int idx, String mode, String source, String translated, String status,
                        List<GroupResult> groupResults) {
            this.idx = idx;
            this.mode = mode;
            this.source = source;
            this.translated = translated;
            this.status = status;
            this.groupResults = groupResults;
        }
    }

    private static class GroupResult {
        final DocxUtils.StyleKey styleKey;
        final String text;

        GroupResult(DocxUtils.StyleKey styleKey, String text) {
            this.styleKey = styleKey;
            this.text = text;
        }
    }

    private static class TranslateResult {
        private final String content;
        private final String status;

        TranslateResult(String content, String status) {
            this.content = content;
            this.status = status;
        }

        String getContent() { return content; }
        String getStatus() { return status; }
    }
}
