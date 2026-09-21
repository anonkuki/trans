package cn.iocoder.sva.module.ai.service.translation.tran.impl;

import cn.iocoder.sva.module.ai.service.translation.tran.*;
import cn.iocoder.sva.module.ai.service.translation.tran.common.DocQualityChecker;
import cn.iocoder.sva.module.ai.service.translation.tran.common.DocxUtils;
import cn.iocoder.sva.module.ai.service.translation.tran.config.TransDocProperties;
import cn.iocoder.sva.module.ai.service.translation.tran.model.TranslationPair;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
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
    @Autowired
    private cn.iocoder.sva.module.ai.service.translation.tran.config.TranslationModeConfig translationModeConfig;

    /** 文档写入锁 */
    private static final ReentrantLock DOC_WRITE_LOCK = new ReentrantLock();

    /** CJK 字符正则 */
    private static final Pattern CJK_PATTERN = Pattern.compile("[\\u4e00-\\u9fff]");
    /** 英文字母正则 */
    private static final Pattern ALPHA_PATTERN = Pattern.compile("[A-Za-z]");
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
            boolean enableComparison,
            boolean translationFirst) {

        // 调整 Apache POI 的安全阈值，以处理复杂的 PDF 转换文件
        ZipSecureFile.setMaxFileCount(100000);  // 增加到100000
        ZipSecureFile.setMinInflateRatio(0.001);  // 降低压缩比检查

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

            // 检查文档是否为空
            if (transIndices.isEmpty()) {
                log.warn("[{}] 文档内容为空，无法翻译", jobId);
                return new TranslationResult(0, new HashMap<>(), "", pairs, outputPath, contrastOutputPath,
                        0, "无法翻译空文档，请上传包含有效内容的文档");
            }

            int total = transIndices.size();
            AtomicInteger done = new AtomicInteger(0);

            // 使用传入的术语库
            Map<String, String> glossary;
            if (useGlossaryReplace && glossaryMap != null && !glossaryMap.isEmpty()) {
                glossary = new HashMap<>(glossaryMap);
                log.info("[{}] 使用传入的术语库，共 {} 条术语", jobId, glossary.size());
            } else {
                glossary = new HashMap<>();
                log.info("[{}] 未提供术语库，使用空术语列表", jobId);
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

            // 确定字体
            String fontName = null;
            if (targetLanguage.toLowerCase().startsWith("english")) {
                fontName = "Times New Roman";
            } else if (targetLanguage.toLowerCase().startsWith("chinese")) {
                fontName = "SimSun";
            }

            // 目标语言非中文时，将中文自动编号（如“第一章、第一条、（一）”）
            // 转换为目标语言编号。这些序号是 Word 基于 numbering.xml 渲染的自动编号，
            // 不在段落文本内，段落翻译无法触达，需在编号定义层面转换
            boolean convertNumbering = !targetLanguage.toLowerCase().startsWith("chinese");

            // 构建原文→译文列表（保持顺序，支持重复文本）
            List<Map.Entry<String, String>> sourceToTarget = new ArrayList<>();
            for (ParagraphResult res : results) {
                if ("skip".equals(res.mode) || "error".equals(res.mode)) {
                    continue;
                }
                sourceToTarget.add(Map.entry(res.source, res.translated));
            }

            // 使用 ZIP 直接写入方式（不用 POI write），保证 Word 2007 兼容性
            DOC_WRITE_LOCK.lock();
            try {
                DocxUtils.writeTranslatedDocxViaZip(inputPath, outputPath, sourceToTarget, fontName, false, convertNumbering);
                log.info("[{}] 纯英文文档已保存: {}", jobId, outputPath);
            } finally {
                DOC_WRITE_LOCK.unlock();
            }

            // 写回对照版本文档
            if (enableComparison && contrastOutputPath != null) {
                List<Map.Entry<String, String>> contrastList = new ArrayList<>();
                for (ParagraphResult res : results) {
                    if ("skip".equals(res.mode) || "error".equals(res.mode)) {
                        continue;
                    }
                    // 双语对照模式：原文段落保持不变，只传入译文文本
                    // 译文将作为独立段落插入到原文段落之后
                    String tgtClean = res.translated.strip().replaceAll("\n{2,}", "\n");
                    contrastList.add(Map.entry(res.source, tgtClean));
                }

                DOC_WRITE_LOCK.lock();
                try {
                    // 双语对照文档不强制覆盖字体（传 null），保留原文档字体设置。
                    // 因为双语段落同时包含原文和译文两种语言，若按目标语言设置字体
                    // （如英文→Times New Roman），会导致原文中的中文在 Office 2016 等
                    // 旧版本中因字体回退机制不完善而显示乱码。
                    // 编号转换与纯译文文档保持一致：目标语言非中文时，
                    // “第一章、（一）”等自动编号同样转为目标语言编号
                    DocxUtils.writeTranslatedDocxViaZip(inputPath, contrastOutputPath, contrastList, null, translationFirst, convertNumbering);
                    log.info("[{}] 双语对照文档已保存: {}", jobId, contrastOutputPath);
                } finally {
                    DOC_WRITE_LOCK.unlock();
                }
            }

        } catch (EncryptedDocumentException e) {
            // 加密文档异常
            log.error("[{}] 文档已加密，无法翻译: {}", jobId, e.getMessage());
            errorCount.incrementAndGet();
            return new TranslationResult(0, new HashMap<>(), "", pairs, outputPath, contrastOutputPath,
                    errorCount.get(), "无法翻译加密文档，请先解除文档加密保护后再上传");
        } catch (NotOfficeXmlFileException e) {
            // NotOfficeXmlFileException - 统一提示为加密文档
            log.error("[{}] 文档解析失败（可能是加密）: {}", jobId, e.getMessage());
            errorCount.incrementAndGet();
            return new TranslationResult(0, new HashMap<>(), "", pairs, outputPath, contrastOutputPath,
                    errorCount.get(), "无法翻译加密文档，请先解除文档加密保护后再上传");
        } catch (Exception e) {
            log.error("[{}] 处理文档失败: {}", jobId, e.getMessage(), e);
            errorCount.incrementAndGet();
            // 检查是否是POI安全限制相关的异常
            if (e.getMessage() != null && e.getMessage().contains("MAX_FILE_COUNT")) {
                return new TranslationResult(0, new HashMap<>(), "", pairs, outputPath, contrastOutputPath,
                        errorCount.get(), "PDF解析失败");
            }
            // 其他异常统一提示为文档处理错误
            return new TranslationResult(0, new HashMap<>(), "", pairs, outputPath, contrastOutputPath,
                    errorCount.get(), "文档处理失败: " + e.getMessage());
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

            // 检查是否为目录段落（暂时注释，目录也进行翻译）
            // if (DocxUtils.isTocFieldParagraph(p) && !DocxUtils.isTocHeadingText(p)) {
            //     return new ParagraphResult(idx, "skip", src, src, "目录跳过");
            // }

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

            // 【关键修复】不能用空格拼接！原文是各 run 文本的直接拼接（无分隔符），
            // 翻译结果也必须保持一致，否则 ZIP 回写时 textMap 匹配失败。
            String translated = String.join("", translatedParts);
            groupResults.add(new GroupResult(group.getStyleKey(), translated));
            statusList.add(mergeStatuses(partStatuses));
        }

        String mergedStatus = mergeStatuses(statusList);
        // 【关键修复】不能用空格拼接样式组翻译结果！
        // 原文（POI getText()）是各 run 文本的直接拼接（无分隔符），
        // 如果用空格拼接翻译结果，会导致与原文不匹配，
        // 后续 ZIP 回写时 textMap 查找失败，翻译无法写入文档。
        String mergedText = String.join("", groupResults.stream().map(g -> g.text != null ? g.text : "").toList());

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
     * <p>
     * 支持两种翻译模式：
     * 1. 术语替换模式（旧模式）：将中文术语直接替换为英文后发给大模型翻译
     * 2. 术语约束翻译模式（新模式）：发送纯净中文原文 + 术语表，让大模型在理解句意后使用术语
     */
    private TranslateResult translateText(
            String text,
            String targetLanguage,
            Map<String, String> glossary,
            boolean useGlossaryReplace) {

        // 判断当前使用的翻译模式
        boolean isConstraintMode = translationModeConfig.isConstraintMode();

        if (isConstraintMode) {
            // 新模式：术语约束翻译
            return translateWithGlossaryConstraint(text, targetLanguage, glossary, useGlossaryReplace);
        } else {
            // 旧模式：术语替换
            return translateWithTermReplacement(text, targetLanguage, glossary, useGlossaryReplace);
        }
    }

    /**
     * 旧模式：术语替换翻译
     * <p>
     * 将中文术语直接替换为英文后发给大模型翻译
     */
    private TranslateResult translateWithTermReplacement(
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

        if (totalClean == 0) {
            return new TranslateResult(text, "未命中");
        }

        // 检查是否已经是目标语言
        if (isPureTarget(text, targetLanguage)) {
            return new TranslateResult(text, "无需翻译");
        }

        // 完全命中术语库 - 直接返回，不调用 LLM，也不使用缓存
        if (covered == totalClean) {
            return new TranslateResult(replaced, "完全命中");
        }

        // 未命中或部分命中术语库 - 调用 LLM

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
     * 新模式：术语约束翻译
     * <p>
     * 发送纯净中文原文 + 术语表，让大模型在理解句意后使用术语
     */
    private TranslateResult translateWithGlossaryConstraint(
            String text,
            String targetLanguage,
            Map<String, String> glossary,
            boolean useGlossaryReplace) {

        // 无论是否启用术语，优先检查是否已是目标语言或纯符号（无需翻译）
        if (isPureTarget(text, targetLanguage)) {
            return new TranslateResult(text, "无需翻译");
        }

        // 如果禁用术语功能，直接使用普通翻译
        if (!useGlossaryReplace || glossary == null || glossary.isEmpty()) {
            log.debug("[术语约束翻译] 未启用术语功能，使用普通翻译");
            LlmClientService.TranslateResult llmResult = llmClient.cachedCall(
                    LlmClientService.CallKind.NORMAL, text, targetLanguage);

            LlmClientService.TranslateResult sanitized = llmClient.sanitizeOrRetry(
                    text, targetLanguage, llmResult.getContent(), llmResult.getUsage());

            return new TranslateResult(
                    sanitized.getContent() != null ? sanitized.getContent() : text,
                    sanitized.getStatus()
            );
        }

        // 先执行术语替换，计算covered和totalClean（用于状态判断）
        GlossaryService.ReplaceResult replaceResult = glossaryService.replaceTermsMaxCover(text, glossary);
        int covered = replaceResult.getCovered();
        int totalClean = replaceResult.getTotalClean();

        // 空白/纯符号串
        if (totalClean == 0) {
            return new TranslateResult(text, "未命中");
        }

        // 完全由术语覆盖 - 直接返回术语替换结果，不调用LLM
        if (covered == totalClean) {
            return new TranslateResult(replaceResult.getText(), "完全命中");
        }

        // 大部分是术语，剩下只有符号/数字 - 也视为完全命中
        String coreRest = replaceResult.getText().replaceAll("[\\s\\d\\W_]+", "");
        if (covered > 0 && coreRest.isEmpty()) {
            return new TranslateResult(replaceResult.getText(), "完全命中");
        }

        // 动态检索当前段落实际出现的术语
        Map<String, String> relevantGlossary = extractRelevantTerms(text, glossary);

        // 调用术语约束翻译方法
        LlmClientService.TranslateResult constraintResult =
                llmClient.translateWithGlossaryConstraint(text, targetLanguage, relevantGlossary);

        // 清洗或重试
        LlmClientService.TranslateResult sanitized = llmClient.sanitizeOrRetry(
                text, targetLanguage, constraintResult.getContent(), constraintResult.getUsage());

        // 质量检查
        DocQualityChecker.QcResult qcResult = DocQualityChecker.check(text, sanitized.getContent());

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
     * 从全文术语库中提取当前段落实际出现的相关术语
     *
     * @param text    当前段落文本
     * @param glossary 全文术语库
     * @return 相关术语映射
     */
    private Map<String, String> extractRelevantTerms(String text, Map<String, String> glossary) {
        Map<String, String> relevantTerms = new HashMap<>();

        if (text == null || text.isEmpty() || glossary == null || glossary.isEmpty()) {
            return relevantTerms;
        }

        // 遍历术语库，检查每个术语是否在文本中出现
        for (Map.Entry<String, String> entry : glossary.entrySet()) {
            String chineseTerm = entry.getKey();
            String englishTerm = entry.getValue();

            // 简单子串匹配（可以根据需要优化为更智能的匹配）
            if (text.contains(chineseTerm)) {
                relevantTerms.put(chineseTerm, englishTerm);
            }
        }

        log.debug("[提取相关术语] 原文前50字符='{}', 找到 {} 个相关术语",
                text.substring(0, Math.min(50, text.length())), relevantTerms.size());

        return relevantTerms;
    }

    /**
     * 校验译文是否使用了术语表中的英文术语
     *
     * @param translatedText 译文
     * @param glossary       术语表
     * @return 校验结果描述
     */
    private String validateGlossaryUsage(String translatedText, Map<String, String> glossary) {
        if (translatedText == null || translatedText.isEmpty() || glossary == null || glossary.isEmpty()) {
            return "OK";
        }

        List<String> missingTerms = new ArrayList<>();

        // 检查每个术语的英文是否在译文中出现
        for (Map.Entry<String, String> entry : glossary.entrySet()) {
            String englishTerm = entry.getValue();

            // 简单子串匹配（可以优化为更智能的边界匹配）
            if (!translatedText.contains(englishTerm)) {
                missingTerms.add(entry.getKey() + "→" + englishTerm);
            }
        }

        if (missingTerms.isEmpty()) {
            log.debug("[术语校验] 所有术语均已正确使用");
            return "OK";
        } else {
            log.warn("[术语校验] 以下术语未在译文中找到: {}", String.join(", ", missingTerms));
            return "术语缺失: " + String.join(", ", missingTerms.subList(0, Math.min(3, missingTerms.size())));
        }
    }

    /**
     * 合并状态
     */
    private String mergeStatus(String llmStatus, String validationStatus) {
        if ("OK".equals(validationStatus)) {
            return llmStatus;
        }

        // 如果有验证问题，优先显示验证状态
        return validationStatus;
    }

    /**
     * 应用严格格式翻译
     * <p>
     * 对于 PDF 转换的文档，就地修改 run 可能导致 XML 结构异常（因为 Python 转换工具
     * 生成的 docx 内部结构可能与 POI 期望的不一致）。因此改为：清除段落所有 run，
     * 按样式组重建 run 并设置译文，同时保留每个样式组的格式信息。
     * 这样既能保持格式，又能“清洗”掉 PDF 转换文档中的异常 XML 结构。
     * <p>
     * 注意：必须在 removeRun 之前提取样式信息，否则 run 的 XML 节点会被断开，
     * 再访问 getFontName() 等属性会抛出 XmlValueDisconnectedException。
     */
    private void applyStrictFormatTranslation(XWPFParagraph p, List<GroupResult> groupResults) {
        List<DocxUtils.StyleGroup> groups = DocxUtils.groupRunsByStyle(p);

        // 如果组数不匹配（PDF转换文档常见问题），降级为整体替换
        if (groups.size() != groupResults.size()) {
            log.debug("[严格格式翻译] 样式组数不匹配(groups={}, results={})，降级为整体替换",
                    groups.size(), groupResults.size());
            String mergedText = groupResults.stream()
                    .map(g -> g.text != null ? g.text : "")
                    .reduce("", String::concat);
            DocxUtils.applyTranslationToParagraph(p, mergedText);
            return;
        }

        // 【关键】在清除 run 之前，先提取每个样式组的格式信息
        // 因为 removeRun 会导致原始 run 的 XML 节点断开（orphaned），
        // 之后再访问 getFontName() 等属性会抛出 XmlValueDisconnectedException
        List<RunStyleInfo> styleInfos = new ArrayList<>();
        for (DocxUtils.StyleGroup sg : groups) {
            RunStyleInfo info = new RunStyleInfo();
            List<XWPFRun> origRuns = sg.getRuns();
            if (!origRuns.isEmpty()) {
                XWPFRun origRun = origRuns.get(0);
                try {
                    info.fontName = origRun.getFontName();
                    info.fontSize = origRun.getFontSizeAsDouble();
                    info.bold = origRun.isBold();
                    info.italic = origRun.isItalic();
                    info.underline = origRun.getUnderline();
                    info.color = origRun.getColor();
                } catch (Exception e) {
                    log.debug("[严格格式翻译] 提取样式信息失败，使用空样式: {}", e.getMessage());
                }
            }
            styleInfos.add(info);
        }

        // 清除段落中所有现有的 run
        List<XWPFRun> existingRuns = new ArrayList<>(p.getRuns());
        for (int i = existingRuns.size() - 1; i >= 0; i--) {
            p.removeRun(i);
        }

        // 按样式组重建 run，保留格式并写入译文
        for (int i = 0; i < groups.size(); i++) {
            GroupResult gr = groupResults.get(i);
            String textToSet = gr.text != null ? gr.text : "";
            RunStyleInfo info = styleInfos.get(i);

            XWPFRun newRun = p.createRun();

            // 从预提取的样式信息恢复格式属性
            if (info.fontName != null) {
                newRun.setFontFamily(info.fontName);
            }
            if (info.fontSize != null) {
                newRun.setFontSize(info.fontSize);
            }
            if (info.bold) {
                newRun.setBold(true);
            }
            if (info.italic) {
                newRun.setItalic(true);
            }
            if (info.underline != null && info.underline != UnderlinePatterns.NONE) {
                newRun.setUnderline(info.underline);
            }
            if (info.color != null) {
                newRun.setColor(info.color);
            }

            newRun.setText(textToSet);
        }
    }

    /**
     * Run 样式信息（用于在清除 run 前保存格式，避免 XmlValueDisconnectedException）
     */
    private static class RunStyleInfo {
        String fontName;
        Double fontSize;
        boolean bold;
        boolean italic;
        UnderlinePatterns underline;
        String color;
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
            // 目标是中文：如果文本含有汉字，认为已是中文（无需翻译）
            // 如果文本含英文字母但没有汉字，认为是英文（需要翻译）
            if (CJK_PATTERN.matcher(text).find()) return true;
            return !ALPHA_PATTERN.matcher(text).find();
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
