package cn.iocoder.sva.module.ai.service.translation.tran.impl;

import cn.iocoder.sva.module.ai.service.translation.helper.ConvertByPythonHelper;
import cn.iocoder.sva.module.ai.service.translation.tran.DocxTranslationService;
import cn.iocoder.sva.module.ai.service.translation.tran.PdfTranslationService;
import cn.iocoder.sva.module.ai.service.translation.tran.ProgressCallback;
import cn.iocoder.sva.module.ai.service.translation.tran.TextCallback;
import cn.iocoder.sva.module.ai.service.translation.tran.config.TransDocProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * PDF 文档翻译服务实现
 * <p>
 * 采用先将 PDF 转换为 Word,然后使用现有 Word 翻译程序的思路。
 * 支持两种PDF转Word方式：
 * 1. 使用 Apache PDFBox 提取文本，创建 Word 文档（默认）
 * 2. 调用 Python 服务进行转换（当配置 pdfConvertMode=1 时）
 */
@Slf4j
@Service
public class PdfTranslationServiceImpl implements PdfTranslationService {

    private final DocxTranslationService docxTranslationService;

    @Autowired
    private TransDocProperties transDocProperties;

    @Autowired(required = false)
    private ConvertByPythonHelper convertByPythonHelper;

    public PdfTranslationServiceImpl(DocxTranslationService docxTranslationService) {
        this.docxTranslationService = docxTranslationService;
    }

    @Override
    public PdfResult processPdf(
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

        log.info("[{}] START PDF file='{}' -> '{}', target='{}', strict={}, comparison={}",
                jobId, inputPath, outputPath, targetLanguage, strictFormat, enableComparison);

        // 临时 Word 文件路径
        Path tempDocx = null;
        try {
            // 第一步：将 PDF 转换为 Word
            tempDocx = Paths.get(inputPath).resolveSibling(
                    Paths.get(inputPath).getFileName() + "_temp.docx");

            log.info("[{}] 正在将 PDF 转换为 Word: {}", jobId, tempDocx);

            // 根据配置选择转换方式
            Integer convertMode = transDocProperties.getPdfConvertMode();
            boolean convertSuccess;

            try {
                if (convertMode != null && convertMode == 1 && convertByPythonHelper != null) {
                    log.info("[{}] 使用 Python 服务进行 PDF 转换", jobId);
                    convertSuccess = convertPdfToWordByPython(inputPath, tempDocx.toString(), jobId);

                    // OCR 模式默认失败即失败，避免认证、网络或 schema 错误被静默隐藏。
                    if (!convertSuccess && transDocProperties.isPythonFallbackEnabled()) {
                        log.warn("[{}] Python 服务转换失败，按显式配置降级到 PDFBox 方式", jobId);
                        convertSuccess = convertPdfToWord(inputPath, tempDocx.toString(), jobId);
                    }
                } else {
                    log.info("[{}] 使用 PDFBox 进行 PDF 转换", jobId);
                    convertSuccess = convertPdfToWord(inputPath, tempDocx.toString(), jobId);
                }
            } catch (RuntimeException e) {
                // 检查是否是加密文档异常
                if (e.getMessage() != null && e.getMessage().startsWith("ENCRYPTED_PDF:")) {
                    log.error("[{}] PDF 文档已加密，无法翻译", jobId);
                    return new PdfResult(0, Map.of(), "",
                            List.of(), "", "", "无法翻译加密文档，请先解除文档加密保护后再上传");
                }
                throw e;
            }

            if (!convertSuccess || !Files.exists(tempDocx)) {
                log.error("[{}] PDF 转 Word 失败: 临时文件未生成", jobId);
                return new PdfResult(0, Map.of(), "",
                        List.of(), "", "", "PDF 转 Word 失败：临时文件未生成");
            }

            log.info("[{}] PDF 转 Word 完成", jobId);

            // 第二步：使用现有的 Word 翻译程序翻译
            String outputDocx = outputPath;
            if (!outputDocx.toLowerCase().endsWith(".docx")) {
                outputDocx = outputDocx.replaceAll("\\.[^.]+$", ".docx");
            }

            final String finalOutputDocx = outputDocx;
            final Path finalTempDocx = tempDocx;

            log.info("[{}] 正在翻译转换后的 Word 文档 (对比模式: {})", jobId, enableComparison);

            DocxTranslationService.TranslationResult docxResult = docxTranslationService.processDocument(
                    tempDocx.toString(),
                    outputDocx,
                    targetLanguage,
                    useGlossaryReplace,
                    glossaryMap,
                    progressCallback,
                    textCallback,
                    strictFormat,
                    enableQc,
                    enableComparison,
                    translationFirst
            );

            log.info("[{}] Word 文档翻译完成", jobId);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[{}] END PDF translation segments={} out='{}' contrast='{}' elapsed={:.2f}s",
                    jobId, docxResult.getSegments(),
                    finalOutputDocx, docxResult.getContrastPath(), elapsed / 1000.0);

            return new PdfResult(
                    docxResult.getSegments(),
                    docxResult.getQcReport() != null ? docxResult.getQcReport() : Map.of(),
                    docxResult.getQcTxtPath() != null ? docxResult.getQcTxtPath() : "",
                    docxResult.getPairs() != null ? convertPairs(docxResult.getPairs()) : List.of(),
                    finalOutputDocx,
                    docxResult.getContrastPath() != null ? docxResult.getContrastPath() : "",
                    docxResult.getError()
            );

        } catch (Exception e) {
            // 检查是否是加密文档
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("encrypt") || errorMsg.contains("password") ||
                errorMsg.contains("decrypt") || errorMsg.contains("protected"))) {
                log.error("[{}] PDF 文档已加密，无法翻译: {}", jobId, e.getMessage());
                return new PdfResult(0, Map.of(), "",
                        List.of(), "", "", "无法翻译加密文档，请先解除文档加密保护后再上传");
            }
            // 检查是否是PDF转换相关的错误
            if (errorMsg != null && (errorMsg.contains("MAX_FILE_COUNT") || errorMsg.contains("ZipSecureFile") ||
                errorMsg.contains("potentially malicious") || errorMsg.contains("internal file entries"))) {
                log.error("[{}] PDF转换后文件结构复杂: {}", jobId, e.getMessage());
                return new PdfResult(0, Map.of(), "",
                        List.of(), "", "", "PDF解析失败");
            }
            // 检查是否是无效的 PDF 文件
            if (errorMsg != null && (errorMsg.contains("Invalid") || errorMsg.contains("corrupt") || errorMsg.contains("damaged"))) {
                log.error("[{}] PDF 文档无效或损坏: {}", jobId, e.getMessage());
                return new PdfResult(0, Map.of(), "",
                        List.of(), "", "", "PDF 文档无效或损坏，请检查文件是否完整");
            }
            log.error("[{}] PDF 翻译失败: {}", jobId, e.getMessage(), e);
            return new PdfResult(0, Map.of(), "",
                    List.of(), "", "", "PDF 翻译失败: " + e.getMessage());
        } finally {
            // 清理临时文件
            if (tempDocx != null && Files.exists(tempDocx)) {
                try {
                    Files.delete(tempDocx);
                    log.info("[{}] 已清理临时文件: {}", jobId, tempDocx);
                } catch (IOException e) {
                    log.warn("[{}] 清理临时文件失败: {}", jobId, e.getMessage());
                }
            }
        }
    }

    /**
     * 使用 Python 服务将 PDF 转换为 Word
     */
    private boolean convertPdfToWordByPython(String pdfPath, String docxPath, String jobId) {
        File convertedFile = null;
        try {
            if (convertByPythonHelper == null) {
                log.error("[{}] ConvertByPythonHelper 未注入，无法使用 Python 转换模式", jobId);
                return false;
            }

            // 读取 PDF 文件
            File pdfFile = new File(pdfPath);

            // 调用 Python 服务转换，返回临时文件
            convertedFile = convertByPythonHelper.convertPdfToDocx(pdfFile);

            // 复制转换后的文件到目标路径
            Path outDir = Paths.get(docxPath).getParent();
            if (outDir != null) {
                Files.createDirectories(outDir);
            }
            Files.copy(convertedFile.toPath(), Paths.get(docxPath));

            log.info("[{}] Python 服务转换成功，文件大小: {} bytes", jobId, convertedFile.length());
            return true;

        } catch (Exception e) {
            log.error("[{}] Python 服务 PDF 转 Word 失败: {}", jobId, e.getMessage(), e);
            return false;
        } finally {
            if (convertedFile != null) {
                try {
                    Files.deleteIfExists(convertedFile.toPath());
                } catch (IOException cleanupError) {
                    log.warn("[{}] 清理 Python OCR 临时文件失败: {}", jobId, cleanupError.getMessage());
                }
            }
        }
    }

    /**
     * 将 PDF 转换为 Word
     * <p>
     * 使用 PDFBox 提取文本，创建 Word 文档。
     * 改进版本：更好地保留段落结构
     */
    private boolean convertPdfToWord(String pdfPath, String docxPath, String jobId) {
        try (PDDocument document = Loader.loadPDF(new java.io.File(pdfPath));
             XWPFDocument wordDoc = new XWPFDocument()) {

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(document.getNumberOfPages());

            // 按页提取文本
            int totalPages = document.getNumberOfPages();
            log.info("[{}] PDF 共 {} 页", jobId, totalPages);

            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                stripper.setStartPage(pageNum);
                stripper.setEndPage(pageNum);

                String pageText = stripper.getText(document);

                if (pageText != null && !pageText.isBlank()) {
                    // 调试：输出前500个字符，查看文本格式
                    if (pageNum == 1) {
                        String preview = pageText.length() > 500 ? pageText.substring(0, 500) : pageText;
                        log.info("[{}] 第{}页文本预览（显示换行符）: {}", jobId, pageNum,
                                preview.replace("\n", "\\n").replace("\r", "\\r"));
                    }

                    // 使用改进的段落分割策略
                    List<String> paragraphs = splitIntoParagraphsImproved(pageText);

                    log.info("[{}] 第{}页识别出 {} 个段落", jobId, pageNum, paragraphs.size());

                    for (String para : paragraphs) {
                        if (para != null && !para.isBlank()) {
                            // 清理段落内容
                            String cleanedPara = cleanParagraphText(para);
                            if (!cleanedPara.isEmpty()) {
                                XWPFParagraph paragraph = wordDoc.createParagraph();
                                paragraph.createRun().setText(cleanedPara);
                            }
                        }
                    }

                    // 添加分页符（除了最后一页）
                    if (pageNum < totalPages) {
                        XWPFParagraph pageBreak = wordDoc.createParagraph();
                        pageBreak.setPageBreak(true);
                    }
                }
            }

            // 保存 Word 文档
            Path outDir = Paths.get(docxPath).getParent();
            if (outDir != null) {
                Files.createDirectories(outDir);
            }
            wordDoc.write(new FileOutputStream(docxPath));

            return true;

        } catch (Exception e) {
            // 检查是否是加密文档
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("encrypt") || errorMsg.contains("password") || errorMsg.contains("decrypt"))) {
                log.error("[{}] PDF 文档已加密，无法转换: {}", jobId, e.getMessage());
                // 设置一个标记，让调用方知道是加密文档
                throw new RuntimeException("ENCRYPTED_PDF: " + e.getMessage(), e);
            }
            log.error("[{}] PDF 转 Word 失败: {}", jobId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 改进的段落分割策略
     * <p>
     * 通用策略：
     * 1. 按双换行符（空行）分割 - 最明确的段落边界
     * 2. 对于每个块，直接合并所有行（不加空格）
     * 3. 如果合并后文本过长，按句子边界适当分段
     *
     * @param text 原始文本
     * @return 段落列表
     */
    private List<String> splitIntoParagraphsImproved(String text) {
        List<String> paragraphs = new ArrayList<>();

        // 第一步：按双换行符（空行）分割
        String[] blocks = text.split("\\n\\s*\\n");

        for (String block : blocks) {
            String trimmed = block.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            // 第二步：合并块内的所有行（直接连接，不加空格）
            String merged = mergeAllLines(trimmed);

            if (!merged.isEmpty()) {
                // 第三步：如果文本很长，按句子边界分段
                if (merged.length() > 300) {
                    // 长文本：按句子分组
                    List<String> subParagraphs = splitBySentences(merged);
                    paragraphs.addAll(subParagraphs);
                } else {
                    // 短文本：作为一段
                    paragraphs.add(cleanParagraphText(merged));
                }
            }
        }

        return paragraphs;
    }

    /**
     * 合并所有行，直接连接（不加空格）
     */
    private String mergeAllLines(String text) {
        String[] lines = text.split("\\n");

        StringBuilder result = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (result.length() > 0) {
                // 处理英文连字符断词
                if (result.toString().endsWith("-")) {
                    result.setLength(result.length() - 1);
                    result.append(trimmed);
                } else {
                    // 直接连接，不加空格（适合中文，英文单词间原本就有空格）
                    result.append(trimmed);
                }
            } else {
                result.append(trimmed);
            }
        }

        return result.toString();
    }

    /**
     * 将长文本按句子边界分成多个段落
     */
    private List<String> splitBySentences(String text) {
        List<String> paragraphs = new ArrayList<>();

        // 按句子结束符分割（保留结束符）
        String[] sentences = text.split("(?<=[。！？.!?])");

        if (sentences.length <= 2) {
            // 句子太少，不分段
            paragraphs.add(cleanParagraphText(text));
            return paragraphs;
        }

        // 将句子分组，每组形成一个段落
        StringBuilder currentParagraph = new StringBuilder();
        int currentLength = 0;

        // 目标段落长度：150-250字符
        int targetLength = 200;

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            int sentenceLength = trimmed.length();

            // 如果当前段落已有内容，且加上新句子会超过目标长度
            if (currentLength > 0 && (currentLength + sentenceLength) > targetLength) {
                // 保存当前段落
                paragraphs.add(cleanParagraphText(currentParagraph.toString()));
                currentParagraph.setLength(0);
                currentLength = 0;
            }

            currentParagraph.append(trimmed);
            currentLength += sentenceLength;
        }

        // 添加最后一个段落
        if (currentParagraph.length() > 0) {
            paragraphs.add(cleanParagraphText(currentParagraph.toString()));
        }

        return paragraphs;
    }

    /**
     * 清理段落文本
     * <p>
     * 移除多余空白字符，标准化空格和标点符号
     *
     * @param text 原始段落文本
     * @return 清理后的文本
     */
    private String cleanParagraphText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // 替换多个连续空格为单个空格
        String cleaned = text.replaceAll("\\s+", " ");

        // 移除首尾空白
        cleaned = cleaned.trim();

        return cleaned;
    }

    /**
     * 判断文本是否以句子终止符结尾
     */
    private boolean endsWithSentenceTerminator(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        char lastChar = text.charAt(text.length() - 1);
        return lastChar == '.' || lastChar == '!' || lastChar == '?' ||
               lastChar == '。' || lastChar == '！' || lastChar == '？';
    }

    /**
     * 转换翻译对格式
     */
    private List<Map<String, String>> convertPairs(List<DocxTranslationService.TranslationPair> pairs) {
        List<Map<String, String>> result = new ArrayList<>();
        for (DocxTranslationService.TranslationPair pair : pairs) {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("原文", pair.getSource());
            map.put("译文", pair.getTarget());
            map.put("状态", pair.getStatus());
            result.add(map);
        }
        return result;
    }
}
