package cn.iocoder.sva.module.ai.service.translation.tran.impl;

import cn.iocoder.sva.module.ai.service.translation.tran.*;
import cn.iocoder.sva.module.ai.service.translation.tran.common.DocQualityChecker;
import cn.iocoder.sva.module.ai.service.translation.tran.config.TransDocProperties;
import lombok.extern.slf4j.Slf4j;
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

    /** 数字/日期样式正则 */
    private static final Pattern NUMERIC_LIKE = Pattern.compile(
            "^[0-9\\uFF10-\\uFF19\\-\\+/.:,\\s年月日T]+$");

    /** Sheet 名非法字符 */
    private static final Pattern INVALID_SHEET_CHARS = Pattern.compile("[:\\\\/?*\\[\\]]");

    public ExcelTranslationServiceImpl(TransDocProperties properties,
                                       LlmClientService llmClient,
                                       GlossaryService glossaryService) {
        this.properties = properties;
        this.llmClient = llmClient;
        this.glossaryService = glossaryService;
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
                Path outDir = Paths.get(outputPath).getParent();
                if (outDir != null) {
                    Files.createDirectories(outDir);
                }
                try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                    wb.write(fos);
                }
                log.info("[{}] EXCEL no text to translate", jobId);
                return new ExcelResult(0, Map.of(), "",
                        List.of(), outputPath, 0, null);
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
            
            if (targetLanguage.toLowerCase().startsWith("chinese")) {
                // 翻译成中文，翻转术语表
                Map<String, String> reversed = new HashMap<>();
                for (Map.Entry<String, String> e : glossary.entrySet()) {
                    reversed.put(e.getValue(), e.getKey());
                }
                glossary = reversed;
            }

            Map<String, String> finalGlossary = glossary;

            // 翻译缓存：{ src -> TransCacheEntry }
            Map<String, TransCacheEntry> transCache = new ConcurrentHashMap<>();

            // 4. 并发翻译所有唯一字符串
            ExecutorService executor = Executors.newFixedThreadPool(concurrency);
            Map<Future<TransResult>, String> futures = new ConcurrentHashMap<>();

            for (String src : uniqueTexts) {
                futures.put(executor.submit(() -> translateOne(src, targetLanguage, finalGlossary,
                        useGlossaryReplace, jobId)), src);
            }

            AtomicInteger done = new AtomicInteger(0);
            for (Future<TransResult> future : futures.keySet()) {
                String src = futures.get(future);
                try {
                    TransResult result = future.get();
                    transCache.put(src, new TransCacheEntry(result.translated, result.status));

                    int current = done.incrementAndGet();
                    safeCallback(progressCallback, current, totalUnique,
                            String.format("唯一字符串 %d/%d 完成", current, totalUnique));
                    safeCallback(textCallback, src, result.translated, result.status);

                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    log.error("[{}] EXCEL future_error for src='{}': {}", jobId, src, e.getMessage());
                    transCache.put(src, new TransCacheEntry(src, "[ERROR] " + e.getMessage()));
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

        } catch (Exception e) {
            log.error("[{}] EXCEL 处理失败: {}", jobId, e.getMessage(), e);
            return new ExcelResult(0, Map.of(), "",
                    pairs, outputPath, errorCount.get(), e.getMessage());
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
     */
    private TransResult translateOne(String src, String targetLanguage,
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
        String status;
        if (qcResult.isOk()) {
            status = (covered == 0) ? "未命中" : "部分命中";
        } else {
            status = qcResult.getStatus();
        }

        return new TransResult(
                sanitized.getContent() != null ? sanitized.getContent() : src,
                status
        );
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
     */
    private boolean isPureTarget(String text, String targetLanguage) {
        if (text == null || text.isBlank()) return true;

        // 纯符号/空白（注意：Java \W 不匹配中文，需要单独处理）
        // 只有空白、标点、数字，不含任何字母或中文
        String stripped = text.replaceAll("[\\s\\p{P}\\p{N}]+", "");
        if (stripped.isEmpty()) return true;

        if (targetLanguage.toLowerCase().startsWith("english")) {
            // 目标是英文，如果不含中文，则认为是纯英文（无需翻译）
            // 如果含中文，则不是纯英文（需要翻译）
            return !text.matches(".*[\\u4e00-\\u9fff].*");
        }
        if (targetLanguage.toLowerCase().startsWith("chinese")) {
            // 目标是中文，如果不含连续6个以上英文字母，则认为是纯中文（无需翻译）
            return !text.matches(".*[A-Za-z]{6,}.*");
        }
        return false;
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
