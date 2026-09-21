package cn.iocoder.sva.module.ai.service.translation.tran.impl;

import cn.iocoder.sva.module.ai.service.translation.tran.*;
import cn.iocoder.sva.module.ai.service.translation.tran.common.DocQualityChecker;
import cn.iocoder.sva.module.ai.service.translation.tran.config.TransDocProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Excel 文档翻译服务实现
 * <p>
 * 核心特性：
 * <ul>
 *   <li>遍历整个工作簿，收集所有需要翻译的字符串单元格 + sheet 名</li>
 *   <li>按"唯一字符串"去重后并发翻译，降低 LLM 调用次数</li>
 *   <li>翻译结果缓存后回填到各个单元格，保持样式不动</li>
 *   <li>sheet 名本身也参与翻译</li>
 * </ul>
 */
@Slf4j
@Service
public class ExcelTranslationServiceImpl implements ExcelTranslationService {

    private final TransDocProperties properties;
    private final LlmClientService llmClient;
    private final GlossaryService glossaryService;
    private final cn.iocoder.sva.module.ai.service.translation.tran.config.TranslationModeConfig translationModeConfig;

    /** 数字/日期/电话号码样式正则（含括号以匹配电话号码格式如 (240) 5060359） */
    private static final Pattern NUMERIC_LIKE = Pattern.compile(
            "^[0-9\\uFF10-\\uFF19\\-\\+/.:,\\s()年月日T]+$");

    /** Sheet 名非法字符 */
    private static final Pattern INVALID_SHEET_CHARS = Pattern.compile("[:\\\\/?*\\[\\]]");

    /** 预编译正则：避免 String.matches() 对长文本触发 StackOverflowError */
    private static final Pattern HAS_CHINESE = Pattern.compile("[\\u4e00-\\u9fff]");
    private static final Pattern HAS_ANY_LETTER = Pattern.compile("\\p{L}");
    private static final Pattern HAS_ASCII_LETTER = Pattern.compile("[A-Za-z]");
    private static final Pattern PURE_ASCII_LETTERS = Pattern.compile("^[A-Za-z]+$");

    public ExcelTranslationServiceImpl(TransDocProperties properties,
                                       LlmClientService llmClient,
                                       GlossaryService glossaryService,
                                       cn.iocoder.sva.module.ai.service.translation.tran.config.TranslationModeConfig translationModeConfig) {
        this.properties = properties;
        this.llmClient = llmClient;
        this.glossaryService = glossaryService;
        this.translationModeConfig = translationModeConfig;
    }

    @Override
    public ExcelResult processExcel(
            String inputPath,
            String outputPath,
            String targetLanguage,
            boolean useGlossaryReplace,
            Map<String, String> glossaryMap,
            ProgressCallback progressCallback,
            TextCallback textCallback,
            boolean enableQc) {

        // 调整 Apache POI 的安全阈值，以处理复杂的文件
        ZipSecureFile.setMaxFileCount(100000);  // 增加到100000
        ZipSecureFile.setMinInflateRatio(0.001);  // 降低压缩比检查

        String jobId = UUID.randomUUID().toString().substring(0, 8);
        long startTime = System.currentTimeMillis();

        int concurrency = Math.max(1, properties.getConcurrency());
        log.info("[{}] EXCEL START file='{}' -> '{}', target='{}', concurrency={}",
                jobId, inputPath, outputPath, targetLanguage, concurrency);

        List<ExcelPair> pairs = new CopyOnWriteArrayList<>();
        AtomicInteger errorCount = new AtomicInteger(0);

        try (Workbook wb = WorkbookFactory.create(new FileInputStream(inputPath))) {
            // 1. 收集所有待翻译的单元格 & sheet 名
            // cellsInfo: List[CellInfo]
            List<CellInfo> cellsInfo = new ArrayList<>();
            // sheetTitleMap: { 原始sheet名 -> [sheet对象] }
            Map<String, List<Sheet>> sheetTitleMap = new HashMap<>();

            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet sheet = wb.getSheetAt(i);
                String titleSrc = sheet.getSheetName().trim();
                if (!titleSrc.isEmpty()) {
                    sheetTitleMap.computeIfAbsent(titleSrc, k -> new ArrayList<>()).add(sheet);
                }

                // 遍历单元格
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        CellType cellType = cell.getCellType();
                        if (cellType != CellType.STRING) {
                            continue;
                        }
                        String src = cell.getStringCellValue();
                        if (src == null || src.isBlank()) {
                            continue;
                        }
                        src = src.strip();
                        // 公式单元格跳过
                        if (src.startsWith("=")) {
                            continue;
                        }
                        // 纯数字/日期样式的字符串跳过
                        if (looksNumericLike(src)) {
                            continue;
                        }
                        cellsInfo.add(new CellInfo(sheet.getSheetName(), cell.getRowIndex(),
                                cell.getColumnIndex(), src));
                    }
                }
            }

            // 无内容需要翻译
            if (cellsInfo.isEmpty() && sheetTitleMap.isEmpty()) {
                log.warn("[{}] Excel文档内容为空，无法翻译", jobId);
                return new ExcelResult(0, Map.of(), "",
                        List.of(), outputPath, 0, "无法翻译空文档，请上传包含有效内容的文档");
            }

            // 2. 按字符串内容去重
            // textMap: { 原始字符串 -> [ (sheetName, row, col) ... ] }
            Map<String, List<CellLocation>> textMap = new HashMap<>();
            for (CellInfo ci : cellsInfo) {
                textMap.computeIfAbsent(ci.text, k -> new ArrayList<>())
                        .add(new CellLocation(ci.sheetName, ci.row, ci.col));
            }

            // 所有唯一字符串 = 单元格文本 ∪ sheet 名
            Set<String> uniqueTextsSet = new HashSet<>(textMap.keySet());
            uniqueTextsSet.addAll(sheetTitleMap.keySet());
            List<String> uniqueTexts = new ArrayList<>(uniqueTextsSet);
            int totalUnique = uniqueTexts.size();

            log.info("[{}] EXCEL collect: unique_strings={}, cell_texts={}, sheets={}",
                    jobId, totalUnique, textMap.size(), sheetTitleMap.size());

            // 3. 使用传入的术语库
            Map<String, String> glossary;
            if (useGlossaryReplace && glossaryMap != null && !glossaryMap.isEmpty()) {
                glossary = new HashMap<>(glossaryMap);
                log.info("[{}] 使用传入的术语库，共 {} 条术语", jobId, glossary.size());
            } else {
                glossary = new HashMap<>();
                log.info("[{}] 未提供术语库，使用空术语列表", jobId);
            }

            Map<String, String> finalGlossary = glossary;

            // 翻译缓存：{ src -> TransCacheEntry }
            Map<String, TransCacheEntry> transCache = new ConcurrentHashMap<>();

            // 4. 并发翻译所有唯一字符串（使用 CompletionService 按完成顺序收集结果）
            ExecutorService executor = Executors.newFixedThreadPool(concurrency);
            CompletionService<Map.Entry<String, TransResult>> completionService =
                    new ExecutorCompletionService<>(executor);

            for (String src : uniqueTexts) {
                final String finalSrc = src;
                completionService.submit(() -> {
                    TransResult result = translateOne(finalSrc, targetLanguage, finalGlossary,
                            useGlossaryReplace, jobId);
                    return new AbstractMap.SimpleEntry<>(finalSrc, result);
                });
            }

            AtomicInteger done = new AtomicInteger(0);
            for (int i = 0; i < totalUnique; i++) {
                try {
                    // take() 按完成顺序返回，谁先翻译完谁先回调，避免被慢任务阻塞
                    Future<Map.Entry<String, TransResult>> future = completionService.take();
                    Map.Entry<String, TransResult> entry = future.get();
                    String src = entry.getKey();
                    TransResult result = entry.getValue();

                    transCache.put(src, new TransCacheEntry(result.translated, result.status));

                    int current = done.incrementAndGet();
                    safeCallback(progressCallback, current, totalUnique,
                            String.format("唯一字符串 %d/%d 完成", current, totalUnique));
                    safeCallback(textCallback, src, result.translated, result.status);

                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    log.error("[{}] EXCEL future_error: {}", jobId, e.getMessage());
                }
            }

            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    log.warn("[{}] Excel翻译任务超时，强制关闭线程池", jobId);
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
                log.error("[{}] 等待Excel线程池关闭时被中断", jobId, e);
            }

            // 5. 回填到各个单元格
            for (Map.Entry<String, List<CellLocation>> entry : textMap.entrySet()) {
                String src = entry.getKey();
                TransCacheEntry info = transCache.getOrDefault(src,
                        new TransCacheEntry(src, "未翻译缓存缺失"));
                String tgt = info.translated;
                String status = info.status;

                for (CellLocation loc : entry.getValue()) {
                    Sheet sheet = wb.getSheet(loc.sheetName);
                    if (sheet == null) continue;
                    Row row = sheet.getRow(loc.row);
                    if (row == null) continue;
                    Cell cell = row.getCell(loc.col);
                    if (cell == null) continue;

                    // 保留样式，只改 value
                    cell.setCellValue(tgt);

                    pairs.add(new ExcelPair(loc.sheetName,
                            cell.getAddress().formatAsString(),
                            src, tgt, status));
                }
            }

            // 5.2 sheet 名回填
            for (Map.Entry<String, List<Sheet>> entry : sheetTitleMap.entrySet()) {
                String src = entry.getKey();
                TransCacheEntry info = transCache.getOrDefault(src,
                        new TransCacheEntry(src, "未翻译缓存缺失"));
                String tgt = info.translated;
                String status = info.status;

                for (Sheet sheet : entry.getValue()) {
                    safeSetSheetTitle(wb, sheet, tgt);
                    pairs.add(new ExcelPair("(sheet_name)", "", src, tgt, status));
                }
            }

            // 保存结果
            Path outDir = Paths.get(outputPath).getParent();
            if (outDir != null) {
                Files.createDirectories(outDir);
            }
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                wb.write(fos);
            }

        } catch (EncryptedDocumentException e) {
            // 加密文档异常
            log.error("[{}] Excel 文档已加密，无法翻译: {}", jobId, e.getMessage());
            return new ExcelResult(0, Map.of(), "",
                    pairs, outputPath, errorCount.get(), "无法翻译加密文档，请先解除文档加密保护后再上传");
        } catch (NotOfficeXmlFileException e) {
            // NotOfficeXmlFileException - 统一提示为加密文档
            log.error("[{}] Excel 文档解析失败（可能是加密）: {}", jobId, e.getMessage());
            return new ExcelResult(0, Map.of(), "",
                    pairs, outputPath, errorCount.get(), "无法翻译加密文档，请先解除文档加密保护后再上传");
        } catch (Exception e) {
            log.error("[{}] EXCEL 处理失败: {}", jobId, e.getMessage(), e);
            // 统一提示为加密文档
            return new ExcelResult(0, Map.of(), "",
                    pairs, outputPath, errorCount.get(), "无法翻译加密文档，请先解除文档加密保护后再上传");
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[{}] EXCEL END unique={} errors={} elapsed={:.2f}s",
                jobId, pairs.size(), errorCount.get(), elapsed / 1000.0);

        return new ExcelResult(pairs.size(), Map.of(), "",
                pairs, outputPath, errorCount.get(), null);
    }

    // ===================== 内部方法 =====================

    /**
     * 翻译单个字符串
     * <p>
     * 支持两种翻译模式：
     * 1. 术语替换模式（旧模式）：将中文术语直接替换为英文后发给大模型翻译
     * 2. 术语约束翻译模式（新模式）：发送纯净中文原文 + 术语表，让大模型在理解句意后使用术语
     */
    private TransResult translateOne(String src, String targetLanguage,
                                     Map<String, String> glossary, boolean useGlossaryReplace,
                                     String jobId) {
        // 判断当前使用的翻译模式
        boolean isConstraintMode = translationModeConfig.isConstraintMode();

        if (isConstraintMode) {
            // 新模式：术语约束翻译
            return translateWithGlossaryConstraint(src, targetLanguage, glossary, useGlossaryReplace, jobId);
        } else {
            // 旧模式：术语替换
            return translateWithTermReplacement(src, targetLanguage, glossary, useGlossaryReplace, jobId);
        }
    }

    /**
     * 旧模式：术语替换翻译
     */
    private TransResult translateWithTermReplacement(String src, String targetLanguage,
                                                      Map<String, String> glossary, boolean useGlossaryReplace,
                                                      String jobId) {
        // 术语替换
        GlossaryService.ReplaceResult replaceResult = useGlossaryReplace
                ? glossaryService.replaceTermsMaxCover(src, glossary)
                : new GlossaryService.ReplaceResult(src, 0,
                src.replaceAll("\\s+", "").length());

        String replaced = replaceResult.getText();
        int covered = replaceResult.getCovered();
        int totalClean = replaceResult.getTotalClean();

        // 空白/纯符号串
        if (totalClean == 0) {
            return new TransResult(src, "未命中");
        }

        // 原文本身已经是目标语言
        if (isPureTarget(src, targetLanguage)) {
            log.info("[{}][无需翻译-术语模式] targetLang='{}', text='{}'",
                    jobId, targetLanguage, src.length() > 100 ? src.substring(0, 100) + "..." : src);
            return new TransResult(src, "无需翻译");
        }

        // 完全由术语覆盖
        if (covered == totalClean) {
            return new TransResult(replaced, "完全命中");
        }

        // 大部分是术语，剩下只有符号/数字
        String coreRest = replaced.replaceAll("[\\s\\d\\W_]+", "");
        if (covered > 0 && coreRest.isEmpty()) {
            return new TransResult(replaced, "完全命中");
        }

        // 走 LLM
        if (covered == 0) {
            log.debug("[{}][术语未命中][调用LLM] totalClean={}", jobId, totalClean);
        } else {
            log.debug("[{}][术语部分命中][调用LLM] covered={}, totalClean={}",
                    jobId, covered, totalClean);
        }

        LlmClientService.TranslateResult llmResult = llmClient.cachedCall(
                LlmClientService.CallKind.NORMAL, replaced, targetLanguage);

        // 清洗或重试
        LlmClientService.TranslateResult sanitized = llmClient.sanitizeOrRetry(
                replaced, targetLanguage, llmResult.getContent(), llmResult.getUsage());

        // 质量检查
        DocQualityChecker.QcResult qcResult = DocQualityChecker.check(replaced, sanitized.getContent());
        // 根据covered判断状态：0=未命中，>0=部分命中
        // WARN 级别（如缩写翻译为目标语言）不阻断，但仍显示警告信息
        String status;
        if (qcResult.isOk()) {
            status = (covered == 0) ? "未命中" : "部分命中";
            // 如果有 WARN 信息，追加显示
            if (qcResult.getStatus() != null && !"OK".equals(qcResult.getStatus())) {
                status = status + " (" + qcResult.getStatus() + ")";
            }
        } else {
            status = qcResult.getStatus();
        }

        return new TransResult(
                sanitized.getContent() != null ? sanitized.getContent() : src,
                status
        );
    }

    /**
     * 新模式：术语约束翻译
     */
    private TransResult translateWithGlossaryConstraint(String src, String targetLanguage,
                                                         Map<String, String> glossary, boolean useGlossaryReplace,
                                                         String jobId) {
        // 无论是否启用术语，优先检查是否已是目标语言或纯符号（无需翻译）
        if (isPureTarget(src, targetLanguage)) {
            log.info("[{}][无需翻译] targetLang='{}', text='{}'",
                    jobId, targetLanguage, src.length() > 100 ? src.substring(0, 100) + "..." : src);
            return new TransResult(src, "无需翻译");
        }

        // 如果禁用术语功能，直接使用普通翻译
        if (!useGlossaryReplace || glossary == null || glossary.isEmpty()) {
            log.debug("[{}][术语约束翻译] 未启用术语功能，使用普通翻译", jobId);
            LlmClientService.TranslateResult llmResult = llmClient.cachedCall(
                    LlmClientService.CallKind.NORMAL, src, targetLanguage);

            LlmClientService.TranslateResult sanitized = llmClient.sanitizeOrRetry(
                    src, targetLanguage, llmResult.getContent(), llmResult.getUsage());

            return new TransResult(
                    sanitized.getContent() != null ? sanitized.getContent() : src,
                    sanitized.getStatus()
            );
        }

        // 先执行术语替换，计算covered和totalClean（用于状态判断）
        GlossaryService.ReplaceResult replaceResult = glossaryService.replaceTermsMaxCover(src, glossary);
        int covered = replaceResult.getCovered();
        int totalClean = replaceResult.getTotalClean();

        // 空白/纯符号串
        if (totalClean == 0) {
            return new TransResult(src, "未命中");
        }

        // 完全由术语覆盖 - 直接返回术语替换结果，不调用LLM
        if (covered == totalClean) {
            log.info("[{}][术语约束翻译-完全命中] covered={}, totalClean={}", jobId, covered, totalClean);
            return new TransResult(replaceResult.getText(), "完全命中");
        }

        // 大部分是术语，剩下只有符号/数字 - 也视为完全命中
        String coreRest = replaceResult.getText().replaceAll("[\\s\\d\\W_]+", "");
        if (covered > 0 && coreRest.isEmpty()) {
            log.info("[{}][术语约束翻译-完全命中(仅符号)] covered={}, totalClean={}", jobId, covered, totalClean);
            return new TransResult(replaceResult.getText(), "完全命中");
        }

        // 动态检索当前文本实际出现的术语
        Map<String, String> relevantGlossary = extractRelevantTerms(src, glossary);

        log.info("[{}][术语约束翻译] 原文长度={}, 总术语数={}, 相关术语数={}",
                jobId, src.length(), glossary.size(), relevantGlossary.size());

        // 调用术语约束翻译方法
        LlmClientService.TranslateResult constraintResult =
                llmClient.translateWithGlossaryConstraint(src, targetLanguage, relevantGlossary);

        // 清洗或重试
        LlmClientService.TranslateResult sanitized = llmClient.sanitizeOrRetry(
                src, targetLanguage, constraintResult.getContent(), constraintResult.getUsage());

        // 质量检查
        DocQualityChecker.QcResult qcResult = DocQualityChecker.check(src, sanitized.getContent());

        // 根据covered判断状态：0=未命中，>0=部分命中
        // WARN 级别（如缩写翻译为目标语言）不阻断，但仍显示警告信息
        String status;
        if (qcResult.isOk()) {
            status = (covered == 0) ? "未命中" : "部分命中";
            // 如果有 WARN 信息，追加显示
            if (qcResult.getStatus() != null && !"OK".equals(qcResult.getStatus())) {
                status = status + " (" + qcResult.getStatus() + ")";
            }
        } else {
            status = qcResult.getStatus();
        }

        return new TransResult(
                sanitized.getContent() != null ? sanitized.getContent() : src,
                status
        );
    }

    /**
     * 从全文术语库中提取当前文本实际出现的相关术语
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

            // 简单子串匹配
            if (text.contains(chineseTerm)) {
                relevantTerms.put(chineseTerm, englishTerm);
            }
        }

        return relevantTerms;
    }

    /**
     * 校验译文是否使用了术语表中的英文术语
     */
    private String validateGlossaryUsage(String translatedText, Map<String, String> glossary) {
        if (translatedText == null || translatedText.isEmpty() || glossary == null || glossary.isEmpty()) {
            return "OK";
        }

        List<String> missingTerms = new ArrayList<>();

        // 检查每个术语的英文是否在译文中出现
        for (Map.Entry<String, String> entry : glossary.entrySet()) {
            String englishTerm = entry.getValue();

            // 简单子串匹配
            if (!translatedText.contains(englishTerm)) {
                missingTerms.add(entry.getKey() + "→" + englishTerm);
            }
        }

        if (missingTerms.isEmpty()) {
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
     * 判断是否类似数字/日期
     */
    private boolean looksNumericLike(String s) {
        if (s == null || s.isBlank()) return false;
        s = s.strip();
        if (s.startsWith("=")) return false;
        return NUMERIC_LIKE.matcher(s).matches();
    }

    /**
     * 判断是否已经是目标语言
     * <p>
     * 只在能高置信度判断时才跳过翻译，避免将西班牙语/法语等非英语拉丁文本误判为"无需翻译"。
     */
    private boolean isPureTarget(String text, String targetLanguage) {
        if (text == null || text.isBlank()) return true;

        // 纯符号/空白（注意：Java \W 不匹配中文，需要单独处理）
        // 只有空白、标点、数字，不含任何字母或中文
        String stripped = text.replaceAll("[\\s\\p{P}\\p{N}]+", "");
        if (stripped.isEmpty()) return true;

        boolean result;
        String langLower = targetLanguage.toLowerCase();
        // 使用预编译 Pattern + find() 代替 String.matches(".*xxx.*")
        // 避免长文本时 String.matches() 递归导致 StackOverflowError
        boolean hasChinese = HAS_CHINESE.matcher(text).find();
        boolean hasAnyLetter = HAS_ANY_LETTER.matcher(text).find();

        if (langLower.startsWith("english")) {
            result = PURE_ASCII_LETTERS.matcher(stripped).matches();
        } else if (langLower.startsWith("chinese")) {
            if (hasChinese) {
                result = true;
            } else {
                result = !hasAnyLetter;
            }
        } else {
            result = false;
        }
        return result;
    }

    /**
     * 安全设置 sheet 名
     */
    private void safeSetSheetTitle(Workbook wb, Sheet sheet, String newTitle) {
        if (newTitle == null || newTitle.isBlank()) return;
        String title = newTitle.strip();
        // Excel 限制：最大长度 31
        if (title.length() > 31) {
            title = title.substring(0, 31);
        }
        // 去掉非法字符
        title = INVALID_SHEET_CHARS.matcher(title).replaceAll("_");
        try {
            wb.setSheetName(wb.getSheetIndex(sheet), title);
        } catch (Exception e) {
            log.warn("set sheet title failed: '{}' -> '{}': {}", newTitle, title, e.getMessage());
        }
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

    private static class CellInfo {
        final String sheetName;
        final int row;
        final int col;
        final String text;

        CellInfo(String sheetName, int row, int col, String text) {
            this.sheetName = sheetName;
            this.row = row;
            this.col = col;
            this.text = text;
        }
    }

    private static class CellLocation {
        final String sheetName;
        final int row;
        final int col;

        CellLocation(String sheetName, int row, int col) {
            this.sheetName = sheetName;
            this.row = row;
            this.col = col;
        }
    }

    private static class TransCacheEntry {
        final String translated;
        final String status;

        TransCacheEntry(String translated, String status) {
            this.translated = translated;
            this.status = status;
        }
    }

    private static class TransResult {
        final String translated;
        final String status;

        TransResult(String translated, String status) {
            this.translated = translated;
            this.status = status;
        }
    }
}
