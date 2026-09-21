package cn.iocoder.sva.module.ai.service.translation.tran.common;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Word文档操作工具类
 * <p>
 * 对应 Python 项目 docx_utils.py，提供段落遍历、翻译应用、样式分组等功能。
 * 使用 Apache POI 库操作 Word 文档。
 */
@Slf4j
public class DocxUtils {

    /** 目录标题匹配正则 */
    private static final Pattern TOC_HEADING_PATTERN = Pattern.compile(
            "^(目录|Table of Contents|Contents)$", Pattern.CASE_INSENSITIVE);

    /** 句子分割正则（按中英文句号、问号、感叹号、分号、冒号分割） */
    public static final Pattern SENT_SPLIT_PATTERN = Pattern.compile(
            "(?<=[。！？!?；;:])\\s+(?=[^\\s])");

    /** 项目符号集合 */
    public static final String BULLET_TOKENS = "•·●○■□...▲△▼▽";

    private DocxUtils() {
        // 工具类，禁止实例化
    }

    // ===================== 段落遍历 =====================

    /**
     * 遍历文档中的所有段落
     * <p>
     * 包括：正文段落、表格内段落、页眉页脚段落
     *
     * @param doc Word文档对象
     * @return 所有段落的列表
     */
    public static List<XWPFParagraph> getAllParagraphs(XWPFDocument doc) {
        List<XWPFParagraph> paragraphs = new ArrayList<>();

        // 正文段落
        paragraphs.addAll(doc.getParagraphs());

        // 表格内段落（支持嵌套表格）
        for (XWPFTable table : doc.getTables()) {
            walkTable(table, paragraphs);
        }

        // 页眉页脚段落
        for (XWPFHeader header : doc.getHeaderList()) {
            paragraphs.addAll(header.getParagraphs());
            for (XWPFTable table : header.getTables()) {
                walkTable(table, paragraphs);
            }
        }
        for (XWPFFooter footer : doc.getFooterList()) {
            paragraphs.addAll(footer.getParagraphs());
            for (XWPFTable table : footer.getTables()) {
                walkTable(table, paragraphs);
            }
        }

        return paragraphs;
    }

    /**
     * 递归遍历表格
     */
    private static void walkTable(XWPFTable table, List<XWPFParagraph> paragraphs) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                paragraphs.addAll(cell.getParagraphs());
                // 支持嵌套表格
                for (XWPFTable nestedTable : cell.getTables()) {
                    walkTable(nestedTable, paragraphs);
                }
            }
        }
    }

    // ===================== 翻译应用到段落 =====================

    /**
     * 将翻译文本应用到段落
     * <p>
     * 清除原有的所有 runs，然后按行添加新内容（保留换行）。
     * 流程：
     * 1. 清除段落中所有 run 元素
     * 2. 将翻译文本按换行分割
     * 3. 第一行直接添加，后续行先添加换行符再添加文本
     *
     * @param paragraph 段落对象
     * @param newText   翻译后的文本
     */
    public static void applyTranslationToParagraph(XWPFParagraph paragraph, String newText) {
        if (paragraph == null) {
            return;
        }

        // 清除所有现有的 run
        List<XWPFRun> runs = new ArrayList<>(paragraph.getRuns());
        for (int i = runs.size() - 1; i >= 0; i--) {
            XWPFRun run = runs.get(i);
            run.setText("", 0);
            paragraph.removeRun(i);
        }

        if (newText == null || newText.isEmpty()) {
            return;
        }

        // 压缩连续换行，并去除首尾空白
        String cleaned = newText.strip().replaceAll("\\n{2,}", "\\n");

        // 按行添加文本，保留换行格式
        String[] lines = cleaned.split("\\n");
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                XWPFRun breakRun = paragraph.createRun();
                breakRun.addBreak();
            }
            XWPFRun textRun = paragraph.createRun();
            textRun.setText(lines[i]);
        }
    }

    // ===================== 目录检测 =====================

    /**
     * 判断段落是否为目录字段段落
     * <p>
     * 通过检查段落中的 fldSimple 元素或 instrText 元素来判断是否为自动生成的目录。
     *
     * @param paragraph 段落对象
     * @return true 如果是目录字段段落
     */
    public static boolean isTocFieldParagraph(XWPFParagraph paragraph) {
        if (paragraph == null) {
            return false;
        }

        try {
            CTP ctp = paragraph.getCTP();
            if (ctp == null) {
                return false;
            }

            // 检查 instrText 元素（复杂字段）
            for (CTR ctr : ctp.getRList()) {
                for (CTText text : ctr.getTList()) {
                    String instrText = text.getStringValue();
                    if (instrText != null) {
                        String upperInstr = instrText.toUpperCase().trim();
                        // 更严格的判断：必须是目录字段指令格式
                        // 典型的TOC字段如: "TOC \o \"1-3\" \h \z"
                        if (upperInstr.startsWith("TOC") || upperInstr.contains("TOC \\")) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 忽略异常，返回 false
        }

        return false;
    }

    /**
     * 判断段落是否为目录标题文本
     * <p>
     * 匹配"目录"、"Table of Contents"、"Contents"等标题。
     *
     * @param paragraph 段落对象
     * @return true 如果是目录标题
     */
    public static boolean isTocHeadingText(XWPFParagraph paragraph) {
        if (paragraph == null) {
            return false;
        }

        String text = paragraph.getText();
        if (text == null) {
            return false;
        }

        return TOC_HEADING_PATTERN.matcher(text.strip()).matches();
    }

    // ===================== 样式分组 =====================

    /**
     * Run样式键
     * <p>
     * 用于标识具有相同样式的 run，以便在严格格式模式下保持样式一致性。
     */
    public static class StyleKey {
        private final String styleName;
        private final Boolean bold;
        private final Boolean italic;
        private final Boolean underline;
        private final Integer fontSize;
        private final String fontName;
        private final String fontColor;
        private final boolean isHyperlink;

        public StyleKey(String styleName, Boolean bold, Boolean italic, Boolean underline,
                        Integer fontSize, String fontName, String fontColor, boolean isHyperlink) {
            this.styleName = styleName;
            this.bold = bold;
            this.italic = italic;
            this.underline = underline;
            this.fontSize = fontSize;
            this.fontName = fontName;
            this.fontColor = fontColor;
            this.isHyperlink = isHyperlink;
        }

        public boolean isHyperlink() {
            return isHyperlink;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StyleKey styleKey = (StyleKey) o;
            if (isHyperlink != styleKey.isHyperlink) return false;
            if (styleName != null ? !styleName.equals(styleKey.styleName) : styleKey.styleName != null)
                return false;
            if (bold != null ? !bold.equals(styleKey.bold) : styleKey.bold != null) return false;
            if (italic != null ? !italic.equals(styleKey.italic) : styleKey.italic != null) return false;
            if (underline != null ? !underline.equals(styleKey.underline) : styleKey.underline != null)
                return false;
            if (fontSize != null ? !fontSize.equals(styleKey.fontSize) : styleKey.fontSize != null)
                return false;
            if (fontName != null ? !fontName.equals(styleKey.fontName) : styleKey.fontName != null)
                return false;
            return fontColor != null ? fontColor.equals(styleKey.fontColor) : styleKey.fontColor == null;
        }

        @Override
        public int hashCode() {
            int result = styleName != null ? styleName.hashCode() : 0;
            result = 31 * result + (bold != null ? bold.hashCode() : 0);
            result = 31 * result + (italic != null ? italic.hashCode() : 0);
            result = 31 * result + (underline != null ? underline.hashCode() : 0);
            result = 31 * result + (fontSize != null ? fontSize.hashCode() : 0);
            result = 31 * result + (fontName != null ? fontName.hashCode() : 0);
            result = 31 * result + (fontColor != null ? fontColor.hashCode() : 0);
            result = 31 * result + (isHyperlink ? 1 : 0);
            return result;
        }
    }

    /**
     * 样式分组结果
     */
    public static class StyleGroup {
        private final StyleKey styleKey;
        private final List<XWPFRun> runs;

        public StyleGroup(StyleKey styleKey, List<XWPFRun> runs) {
            this.styleKey = styleKey;
            this.runs = runs;
        }

        public StyleKey getStyleKey() {
            return styleKey;
        }

        public List<XWPFRun> getRuns() {
            return runs;
        }

        /**
         * 获取分组中的合并文本
         */
        public String getMergedText() {
            StringBuilder sb = new StringBuilder();
            for (XWPFRun run : runs) {
                String text = run.getText(0);
                if (text != null) {
                    sb.append(text);
                }
            }
            return sb.toString();
        }
    }

    /**
     * 按样式对段落中的 run 进行分组
     * <p>
     * 相邻的具有相同样式的 run 会被分到同一组，超链接 run 会单独分组。
     *
     * @param paragraph 段落对象
     * @return 样式分组列表
     */
    public static List<StyleGroup> groupRunsByStyle(XWPFParagraph paragraph) {
        List<StyleGroup> groups = new ArrayList<>();

        if (paragraph == null) {
            return groups;
        }

        StyleKey currentKey = null;
        List<XWPFRun> currentGroup = new ArrayList<>();

        for (XWPFRun run : paragraph.getRuns()) {
            StyleKey key = extractStyleKey(run);

            // 超链接单独分组
            if (key.isHyperlink) {
                if (!currentGroup.isEmpty()) {
                    groups.add(new StyleGroup(currentKey, new ArrayList<>(currentGroup)));
                    currentGroup.clear();
                    currentKey = null;
                }
                groups.add(new StyleGroup(key, List.of(run)));
                continue;
            }

            // 相同样式合并，或者当前组为空
            if (currentKey == null || (currentKey.equals(key) && !currentKey.isHyperlink)) {
                currentGroup.add(run);
                currentKey = key;
            } else {
                // 样式不同，保存当前组，开始新组
                groups.add(new StyleGroup(currentKey, new ArrayList<>(currentGroup)));
                currentGroup.clear();
                currentGroup.add(run);
                currentKey = key;
            }
        }

        // 保存最后一组
        if (!currentGroup.isEmpty()) {
            groups.add(new StyleGroup(currentKey, currentGroup));
        }

        return groups;
    }

    /**
     * 提取 run 的样式键
     */
    private static StyleKey extractStyleKey(XWPFRun run) {
        String styleName = run.getStyle();
        Boolean bold = run.isBold();
        Boolean italic = run.isItalic();
        Boolean underline = run.getUnderline() != UnderlinePatterns.NONE;

        Integer fontSize = null;
        if (run.getFontSizeAsDouble() != null) {
            fontSize = run.getFontSizeAsDouble().intValue();
        }

        String fontName = run.getFontName();
        String fontColor = null;
        if (run.getColor() != null) {
            fontColor = run.getColor();
        }

        boolean isHyperlink = isHyperlinkRun(run);

        return new StyleKey(styleName, bold, italic, underline, fontSize, fontName, fontColor, isHyperlink);
    }

    /**
     * 判断 run 是否为超链接
     * <p>
     * 注意：原实现使用 selectPath() 需要 Saxon 依赖，现改为 DOM 方式遍历。
     */
    private static boolean isHyperlinkRun(XWPFRun run) {
        try {
            CTR ctr = run.getCTR();
            if (ctr == null) {
                return false;
            }

            // 使用 DOM 方式检查父元素链中是否有 hyperlink 元素
            org.w3c.dom.Node node = ctr.getDomNode();
            while (node != null) {
                if (node instanceof org.w3c.dom.Element) {
                    org.w3c.dom.Element elem = (org.w3c.dom.Element) node;
                    // 检查是否为 hyperlink 元素 (w:hyperlink)
                    if ("hyperlink".equals(elem.getLocalName()) ||
                        "w:hyperlink".equals(elem.getTagName())) {
                        return true;
                    }
                }
                node = node.getParentNode();
            }
        } catch (Exception e) {
            // 忽略异常
        }

        return false;
    }

    // ===================== 文档后处理 =====================

    /**
     * 统一设置文档字体
     * <p>
     * 注意：Apache POI 的 setFontFamily 可能抛出 ArrayIndexOutOfBoundsException，
     * 这是 POI 的已知 Bug，需要 try-catch 保护。
     *
     * @param doc      Word文档
     * @param fontName 字体名称
     */
    public static void setFontAll(XWPFDocument doc, String fontName) {
        // 正文段落
        for (XWPFParagraph p : doc.getParagraphs()) {
            for (XWPFRun run : p.getRuns()) {
                safeSetFontFamily(run, fontName);
                setEastAsiaFont(run, fontName);
            }
        }

        // 表格
        for (XWPFTable table : doc.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph p : cell.getParagraphs()) {
                        for (XWPFRun run : p.getRuns()) {
                            safeSetFontFamily(run, fontName);
                            setEastAsiaFont(run, fontName);
                        }
                    }
                }
            }
        }

        // 页眉页脚
        for (XWPFHeader header : doc.getHeaderList()) {
            for (XWPFParagraph p : header.getParagraphs()) {
                for (XWPFRun run : p.getRuns()) {
                    safeSetFontFamily(run, fontName);
                    setEastAsiaFont(run, fontName);
                }
            }
        }
        for (XWPFFooter footer : doc.getFooterList()) {
            for (XWPFParagraph p : footer.getParagraphs()) {
                for (XWPFRun run : p.getRuns()) {
                    safeSetFontFamily(run, fontName);
                    setEastAsiaFont(run, fontName);
                }
            }
        }
    }

    /**
     * 安全设置字体（捕获 POI 的 ArrayIndexOutOfBoundsException）
     */
    private static void safeSetFontFamily(XWPFRun run, String fontName) {
        try {
            run.setFontFamily(fontName);
        } catch (ArrayIndexOutOfBoundsException e) {
            // Apache POI 已知 Bug，忽略此异常
        } catch (Exception e) {
            // 其他异常也忽略
        }
    }

    /**
     * 设置东亚字体（解决中文字体显示问题）
     * <p>
     * 注意：必须先移除已有的 rFonts 元素，再添加新的，
     * 否则会产生重复的 rFonts 元素导致文档损坏（尤其是 PDF 转换的文档）。
     */
    private static void setEastAsiaFont(XWPFRun run, String fontName) {
        try {
            CTR ctr = run.getCTR();
            CTRPr rPr = ctr.isSetRPr() ? ctr.getRPr() : ctr.addNewRPr();
            // 移除已有的 rFonts 元素，避免重复
            for (int i = rPr.sizeOfRFontsArray() - 1; i >= 0; i--) {
                rPr.removeRFonts(i);
            }
            CTFonts fonts = rPr.addNewRFonts();
            fonts.setEastAsia(fontName);
        } catch (Exception e) {
            // 忽略异常
        }
    }

    /**
     * 启用表格自动调整
     * <p>
     * 让表格自动根据内容调整宽高，避免翻译后内容溢出。
     *
     * @param doc Word文档
     */
    public static void enableTableAutofit(XWPFDocument doc) {
        for (XWPFTable table : doc.getTables()) {
            try {
                // 设置表格布局为 autofit
                CTTblPr tblPr = table.getCTTbl().getTblPr();
                if (tblPr == null) {
                    tblPr = table.getCTTbl().addNewTblPr();
                }

                // 移除现有的 tblLayout
                if (tblPr.isSetTblLayout()) {
                    tblPr.unsetTblLayout();
                }

                // 添加 autofit 布局
                CTTblLayoutType layout = tblPr.addNewTblLayout();
                layout.setType(STTblLayoutType.AUTOFIT);

                // 设置行高为自动
                for (XWPFTableRow row : table.getRows()) {
                    CTTrPr trPr = row.getCtRow().isSetTrPr()
                            ? row.getCtRow().getTrPr()
                            : row.getCtRow().addNewTrPr();

                    // 移除固定行高
                    for (int i = trPr.sizeOfTrHeightArray() - 1; i >= 0; i--) {
                        trPr.removeTrHeight(i);
                    }

                    // 添加自动行高
                    CTHeight height = trPr.addNewTrHeight();
                    height.setHRule(STHeightRule.AUTO);
                }

                // 允许单元格换行
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        CTTcPr tcPr = cell.getCTTc().isSetTcPr()
                                ? cell.getCTTc().getTcPr()
                                : cell.getCTTc().addNewTcPr();

                        // 移除 noWrap 设置
                        if (tcPr.isSetNoWrap()) {
                            tcPr.unsetNoWrap();
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略单个表格的异常
            }
        }
    }

    // ===================== ZIP 直接写入（不用 POI write） =====================

    private static final String W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

    /**
     * 通过直接操作 ZIP 内 XML 的方式写入翻译后的文档
     * <p>
     * 完全不用 POI 的 write()，而是以原始 docx 为模板，
     * 通过文本匹配找到对应段落并替换文本。
     * 这样原始文档的所有 XML 结构、命名空间、格式完全不变，
     * 彻底避免 POI 5.x 序列化引入的 Word 2007 不兼容问题。
     *
     * @param inputPath       原始文档路径（作为 ZIP 模板）
     * @param outputPath      输出路径
     * @param sourceToTarget  原文→译文的映射（支持重复文本）
     * @param fontName        要设置的字体（null 表示不修改字体）
     * @param translationFirst  译文前置（仅双语对照模式有效，true=译文在原文前，false=译文在原文后）
     * @param convertNumbering  是否将中文自动编号（如“第一章、第一条、（一）”）转换为目标语言编号
     */
    public static void writeTranslatedDocxViaZip(
            String inputPath,
            String outputPath,
            List<Map.Entry<String, String>> sourceToTarget,
            String fontName,
            boolean translationFirst,
            boolean convertNumbering) throws Exception {

        // 先复制原始文件到输出路径
        Path outputDir = Paths.get(outputPath).getParent();
        if (outputDir != null) {
            Files.createDirectories(outputDir);
        }
        Files.copy(Paths.get(inputPath), Paths.get(outputPath),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // 使用 POI 原文作为 textMap 的 key。
        // processContentXml 中的 extractTextFromChildren 必须与 POI getText() 行为一致：
        // 处理 <w:r>、<w:hyperlink>、<w:ins>、<w:smartTag> 内的文本。
        Map<String, LinkedList<String>> textMap = buildTextMapFromXml(inputPath, sourceToTarget);
        log.info("[writeTranslatedDocxViaZip] textMap 构建完成: sourceToTarget={}, textMap keys={}",
                sourceToTarget.size(), textMap.size());

        // 双语对照模式（fontName==null）下目标语言非中文时，解析编号定义：
        // 译文段落不再引用自动编号，而是按编号定义计算序号文本、翻译后拼接到译文正文前面
        NumberingModel numberingModel = null;
        boolean contrastMode = fontName == null;
        if (contrastMode && convertNumbering) {
            try (ZipFile preZf = new ZipFile(outputPath)) {
                ZipEntry numEntry = preZf.getEntry("word/numbering.xml");
                if (numEntry != null) {
                    try (InputStream nis = preZf.getInputStream(numEntry)) {
                        numberingModel = buildNumberingModel(nis.readAllBytes());
                        log.info("[writeTranslatedDocxViaZip] 双语对照模式: 编号定义解析完成, numId数={}",
                                numberingModel.levelsByNumId.size());
                    }
                }
            } catch (Exception e) {
                log.warn("[writeTranslatedDocxViaZip] 解析编号定义失败，译文段落序号将不拼接: {}", e.getMessage());
            }
        }
        final NumberingModel finalNumberingModel = numberingModel;

        File tempFile = new File(outputPath + ".write.tmp");
        try {
            try (ZipFile zf = new ZipFile(outputPath);
                 ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempFile))) {

                var entries = zf.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    zos.putNextEntry(new ZipEntry(entry.getName()));

                    try (InputStream is = zf.getInputStream(entry)) {
                        byte[] bytes = is.readAllBytes();
                        String name = entry.getName();

                        if ("word/document.xml".equals(name)
                                || (name.startsWith("word/header") && name.endsWith(".xml"))
                                || (name.startsWith("word/footer") && name.endsWith(".xml"))) {
                            bytes = processContentXml(bytes, textMap, fontName, translationFirst, finalNumberingModel);
                        } else if ("word/styles.xml".equals(name) && fontName != null) {
                            bytes = setDefaultFontInStyles(bytes, fontName);
                        } else if ("word/numbering.xml".equals(name) && convertNumbering) {
                            if (!contrastMode) {
                                // 纯译文：直接全局转换为英文编号；
                                // 双语对照：保留原中文编号定义不变（译文序号以文本形式拼接）
                                bytes = convertChineseNumberingXml(bytes);
                            }
                        }

                        zos.write(bytes);
                    }
                    zos.closeEntry();
                }
            }

            Files.move(tempFile.toPath(), Paths.get(outputPath),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        } catch (Exception e) {
            if (tempFile.exists()) {
                tempFile.delete();
            }
            throw e;
        }
    }

    // ===================== 中文自动编号转换 =====================

    /**
     * 中文数字编号格式（numFmt）集合
     * <p>
     * 这些格式会渲染出“一、二、三”等中文数字，翻译为非中文目标语言时
     * 需转换为阿拉伯数字（decimal）。
     */
    private static final Pattern CHINESE_NUM_FMT_PATTERN = Pattern.compile(
            "<w:numFmt w:val=\"(?:chineseCounting|chineseCountingThousand|" +
            "chineseLegalSimplified|chineseLegalTraditional|ideographDigital|" +
            "ideographTraditional|ideographEnclosedCircle|ideographZodiac)\"/>");

    /** lvlText 编号模板匹配 */
    private static final Pattern LVL_TEXT_PATTERN = Pattern.compile(
            "<w:lvlText w:val=\"([^\"]*)\"/>");

    /**
     * 将 numbering.xml 中的中文编号定义转换为英文编号
     * <p>
     * 文档中“第一章、第一条、（一）”等标题序号是 Word 基于 numbering.xml
     * 自动渲染的编号，不存在于段落文本中，段落翻译时无法触达。
     * 这里将中文数字编号格式（如 chineseCountingThousand）转为 decimal，
     * 并将中文编号模板（如“第%1章”）转为英文模板（如“Chapter %1”），
     * 使译文文档中的标题序号以目标语言形式呈现。
     */
    public static byte[] convertChineseNumberingXml(byte[] xmlBytes) {
        String content = new String(xmlBytes, java.nio.charset.StandardCharsets.UTF_8);

        // 1. 中文数字编号格式 → decimal
        String converted = CHINESE_NUM_FMT_PATTERN.matcher(content)
                .replaceAll("<w:numFmt w:val=\"decimal\"/>");

        // 2. 转换中文编号模板 lvlText
        Matcher m = LVL_TEXT_PATTERN.matcher(converted);
        StringBuilder sb = new StringBuilder();
        int changedCount = 0;
        while (m.find()) {
            String original = m.group(1);
            String newLvl = convertLvlText(original);
            if (!newLvl.equals(original) && !newLvl.isEmpty()) {
                changedCount++;
                m.appendReplacement(sb, Matcher.quoteReplacement("<w:lvlText w:val=\"" + newLvl + "\"/>"));
            } else {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group()));
            }
        }
        m.appendTail(sb);

        log.info("[convertChineseNumberingXml] 中文编号转换完成, lvlText 修改数={}", changedCount);
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 将单个中文编号模板转为英文模板，保留 %n 占位符
     * <p>
     * 如：第%1章 → Chapter %1；第%2条 → Article %2；(%1)（中文数字）→ (%1)（decimal）
     */
    private static String convertLvlText(String val) {
        String result = val;
        // 常见中文编号模板 → 英文模板（长词在前避免误匹配）
        result = result.replaceAll("第(%\\d+)部分", "Part $1");
        result = result.replaceAll("第(%\\d+)章", "Chapter $1");
        result = result.replaceAll("第(%\\d+)编", "Book $1");
        result = result.replaceAll("第(%\\d+)篇", "Part $1");
        result = result.replaceAll("第(%\\d+)节", "Section $1");
        result = result.replaceAll("第(%\\d+)条", "Article $1");
        result = result.replaceAll("第(%\\d+)款", "Clause $1");
        result = result.replaceAll("第(%\\d+)项", "Item $1");
        result = result.replaceAll("附录(%\\d+)", "Appendix $1");
        result = result.replaceAll("附件(%\\d+)", "Appendix $1");
        result = result.replaceAll("表(%\\d+)", "Table $1");
        result = result.replaceAll("图(%\\d+)", "Figure $1");
        // 去除剩余中文字符
        result = result.replaceAll("[\\u4e00-\\u9fff]+", "");
        // 中文标点转西文标点（如“%1、”→“%1.”）
        result = result.replace('、', '.')
                .replace('（', '(').replace('）', ')')
                .replace('；', ';').replace('：', ':');
        return result;
    }

    /** 中文字符检测正则 */
    private static final Pattern CJK_CHAR_PATTERN = Pattern.compile("[\\u4e00-\\u9fff]");

    /** 中文数字字符集（用于序号文本回译） */
    private static final String CN_NUM_CHARS = "〇零一二三四五六七八九十百千万两";

    /** 数字串匹配：中文数字串或阿拉伯数字串 */
    private static final Pattern NUMERAL_PATTERN = Pattern.compile("[" + CN_NUM_CHARS + "]+|\\d+");

    /**
     * 编号定义模型：numId → 各层级定义（起始值/数字格式/编号模板）
     * <p>
     * 双语对照模式下用于按文档顺序计算各编号段落的渲染序号，
     * 翻译后以纯文本拼接到译文正文前面。
     */
    private static class NumberingModel {
        final Map<String, LevelDef[]> levelsByNumId = new HashMap<>();
    }

    /** 单个编号层级定义 */
    private static class LevelDef {
        int start = 1;
        String numFmt = "decimal";
        String lvlText = "%1.";
    }

    /**
     * 解析 numbering.xml 构建编号定义模型：numId → 各层级的 start/numFmt/lvlText
     * （含 w:num 内 lvlOverride 的覆盖）
     */
    private static NumberingModel buildNumberingModel(byte[] xmlBytes) throws Exception {
        NumberingModel model = new NumberingModel();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xmlBytes));
        Element root = doc.getDocumentElement();

        // abstractNumId → 各层级定义
        Map<String, LevelDef[]> abstractLevels = new HashMap<>();
        NodeList absList = root.getElementsByTagNameNS(W_NS, "abstractNum");
        for (int i = 0; i < absList.getLength(); i++) {
            Element abs = (Element) absList.item(i);
            String absId = abs.getAttribute("w:abstractNumId");
            LevelDef[] levels = new LevelDef[9];
            NodeList children = abs.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if (!(child instanceof Element)) continue;
                Element elem = (Element) child;
                if (!"lvl".equals(elem.getLocalName()) || !W_NS.equals(elem.getNamespaceURI())) continue;
                int ilvl = (int) parseLongSafe(elem.getAttribute("w:ilvl"), -1);
                if (ilvl < 0 || ilvl >= 9) continue;
                LevelDef def = new LevelDef();
                NodeList startL = elem.getElementsByTagNameNS(W_NS, "start");
                if (startL.getLength() > 0) def.start = (int) parseLongSafe(((Element) startL.item(0)).getAttribute("w:val"), 1);
                NodeList fmtL = elem.getElementsByTagNameNS(W_NS, "numFmt");
                if (fmtL.getLength() > 0) def.numFmt = ((Element) fmtL.item(0)).getAttribute("w:val");
                NodeList textL = elem.getElementsByTagNameNS(W_NS, "lvlText");
                if (textL.getLength() > 0) def.lvlText = ((Element) textL.item(0)).getAttribute("w:val");
                levels[ilvl] = def;
            }
            abstractLevels.put(absId, levels);
        }

        // numId → abstractNum 层级 + lvlOverride 覆盖
        NodeList numList = root.getElementsByTagNameNS(W_NS, "num");
        for (int i = 0; i < numList.getLength(); i++) {
            Element num = (Element) numList.item(i);
            String numId = num.getAttribute("w:numId");
            NodeList absRefL = num.getElementsByTagNameNS(W_NS, "abstractNumId");
            if (absRefL.getLength() == 0) continue;
            LevelDef[] base = abstractLevels.get(((Element) absRefL.item(0)).getAttribute("w:val"));
            if (base == null) continue;
            LevelDef[] levels = new LevelDef[9];
            for (int l = 0; l < 9; l++) {
                if (base[l] == null) continue;
                levels[l] = new LevelDef();
                levels[l].start = base[l].start;
                levels[l].numFmt = base[l].numFmt;
                levels[l].lvlText = base[l].lvlText;
            }
            NodeList children = num.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if (!(child instanceof Element)) continue;
                Element elem = (Element) child;
                if (!"lvlOverride".equals(elem.getLocalName()) || !W_NS.equals(elem.getNamespaceURI())) continue;
                int ilvl = (int) parseLongSafe(elem.getAttribute("w:ilvl"), -1);
                if (ilvl < 0 || ilvl >= 9 || levels[ilvl] == null) continue;
                NodeList soL = elem.getElementsByTagNameNS(W_NS, "startOverride");
                if (soL.getLength() > 0) levels[ilvl].start = (int) parseLongSafe(((Element) soL.item(0)).getAttribute("w:val"), levels[ilvl].start);
                NodeList fmtL = elem.getElementsByTagNameNS(W_NS, "numFmt");
                if (fmtL.getLength() > 0) levels[ilvl].numFmt = ((Element) fmtL.item(0)).getAttribute("w:val");
                NodeList textL = elem.getElementsByTagNameNS(W_NS, "lvlText");
                if (textL.getLength() > 0) levels[ilvl].lvlText = ((Element) textL.item(0)).getAttribute("w:val");
            }
            model.levelsByNumId.put(numId, levels);
        }
        return model;
    }

    /**
     * 按文档顺序推进编号计数器，并返回该段落序号的译文文本（如“Chapter 1”）
     * <p>
     * 模拟 Word 编号引擎：同一 numId 的计数器按文档顺序递增，
     * 浅层级出现时重置更深层级。渲染出的序号（如“第一章”）再经
     * convertRenderedNumberText 转为英文。段落无编号或定义缺失时返回 null。
     *
     * @param counters 编号计数器状态：numId → 各层级当前值（调用方维护，跨段落复用）
     */
    private static String advanceAndRenderNumber(Element p, NumberingModel model, Map<String, int[]> counters) {
        NodeList pPrL = p.getElementsByTagNameNS(W_NS, "pPr");
        if (pPrL.getLength() == 0) return null;
        Element pPr = (Element) pPrL.item(0);

        String numId = null;
        int ilvl = 0;
        NodeList children = pPr.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (!(child instanceof Element)) continue;
            Element elem = (Element) child;
            if (!"numPr".equals(elem.getLocalName()) || !W_NS.equals(elem.getNamespaceURI())) continue;
            NodeList ilvlL = elem.getElementsByTagNameNS(W_NS, "ilvl");
            if (ilvlL.getLength() > 0) ilvl = (int) parseLongSafe(((Element) ilvlL.item(0)).getAttribute("w:val"), 0);
            NodeList numIdL = elem.getElementsByTagNameNS(W_NS, "numId");
            if (numIdL.getLength() > 0) numId = ((Element) numIdL.item(0)).getAttribute("w:val");
        }
        if (numId == null) return null;

        LevelDef[] levels = model.levelsByNumId.get(numId);
        if (levels == null || ilvl < 0 || ilvl >= 9 || levels[ilvl] == null) return null;
        LevelDef def = levels[ilvl];

        // 推进当前层级计数，重置更深层级（Word 编号引擎规则）
        int[] c = counters.computeIfAbsent(numId, k -> new int[9]);
        c[ilvl] = (c[ilvl] == 0) ? def.start : c[ilvl] + 1;
        for (int l = ilvl + 1; l < 9; l++) c[l] = 0;

        // 按 lvlText 模板渲染：%n 占位符替换为对应层级的计数值
        Matcher m = Pattern.compile("%(\\d)").matcher(def.lvlText);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            int idx = (int) parseLongSafe(m.group(1), 1) - 1;
            int value = 1;
            String fmt = def.numFmt;
            if (idx >= 0 && idx < 9) {
                if (c[idx] > 0) {
                    value = c[idx];
                } else if (levels[idx] != null) {
                    value = levels[idx].start;
                }
                if (levels[idx] != null) fmt = levels[idx].numFmt;
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(formatNumber(value, fmt)));
        }
        m.appendTail(sb);
        return convertRenderedNumberText(sb.toString());
    }

    /**
     * 将渲染出的序号文本转为目标语言
     * <p>
     * 如：第一章 → Chapter 1；第一条 → Article 1；（一） → (1)；一、 → 1.
     */
    private static String convertRenderedNumberText(String text) {
        String r = text;
        r = replaceLabelNumber(r, "第", "部分", "Part");
        r = replaceLabelNumber(r, "第", "章", "Chapter");
        r = replaceLabelNumber(r, "第", "编", "Book");
        r = replaceLabelNumber(r, "第", "篇", "Part");
        r = replaceLabelNumber(r, "第", "节", "Section");
        r = replaceLabelNumber(r, "第", "条", "Article");
        r = replaceLabelNumber(r, "第", "款", "Clause");
        r = replaceLabelNumber(r, "第", "项", "Item");
        r = replaceLabelNumber(r, "附录", "", "Appendix");
        r = replaceLabelNumber(r, "附件", "", "Appendix");
        r = replaceLabelNumber(r, "表", "", "Table");
        r = replaceLabelNumber(r, "图", "", "Figure");
        // 剩余中文数字串 → 阿拉伯数字（如“（一）”→“（1）”）
        r = NUMERAL_PATTERN.matcher(r).replaceAll(mr -> numeralToDecimalString(mr.group()));
        // 去除剩余中文字符
        r = CJK_CHAR_PATTERN.matcher(r).replaceAll("");
        // 中文标点转西文标点
        r = r.replace('、', '.')
                .replace('（', '(').replace('）', ')')
                .replace('；', ';').replace('：', ':');
        return r.trim();
    }

    /** 将“前缀+数字+后缀”形式的中文序号替换为英文（如 第X章 → Chapter d） */
    private static String replaceLabelNumber(String text, String prefix, String suffix, String english) {
        String num = "([" + CN_NUM_CHARS + "]+|\\d+)";
        Pattern p = Pattern.compile(Pattern.quote(prefix) + num + Pattern.quote(suffix));
        return p.matcher(text).replaceAll(mr -> english + " " + numeralToDecimalString(mr.group(1)));
    }

    /** 数字串转阿拉伯数字字符串（已是阿拉伯数字则原样返回） */
    private static String numeralToDecimalString(String s) {
        if (s.matches("\\d+")) return s;
        return String.valueOf(chineseToDecimal(s));
    }

    /** 中文数字转十进制（支持 零〇一二...九 十百千万，如 二十一 → 21，一百零五 → 105） */
    private static long chineseToDecimal(String s) {
        long total = 0, section = 0, number = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            int digit = chineseDigit(ch);
            if (digit >= 0) {
                number = digit;
                continue;
            }
            long unit = chineseUnit(ch);
            if (unit <= 0) continue;
            if (number == 0 && unit == 10) number = 1; // “十一”→11，“十”→10
            section += number * unit;
            if (unit == 10000) {
                total += section;
                section = 0;
            }
            number = 0;
        }
        return total + section + number;
    }

    private static int chineseDigit(char ch) {
        switch (ch) {
            case '零': case '〇': return 0;
            case '一': return 1;
            case '二': case '两': return 2;
            case '三': return 3;
            case '四': return 4;
            case '五': return 5;
            case '六': return 6;
            case '七': return 7;
            case '八': return 8;
            case '九': return 9;
            default: return -1;
        }
    }

    private static long chineseUnit(char ch) {
        switch (ch) {
            case '十': return 10;
            case '百': return 100;
            case '千': return 1000;
            case '万': return 10000;
            default: return 0;
        }
    }

    /**
     * 按 numFmt 渲染计数值（用于 lvlText 模板替换）
     * <p>
     * 中文格式先渲染为中文数字，随后由 convertRenderedNumberText 统一翻译。
     */
    private static String formatNumber(int value, String numFmt) {
        switch (numFmt) {
            case "lowerLetter": return toLetter(value, 'a');
            case "upperLetter": return toLetter(value, 'A');
            case "lowerRoman": return toRoman(value).toLowerCase();
            case "upperRoman": return toRoman(value);
            case "chineseCounting":
            case "chineseCountingThousand":
            case "chineseLegalSimplified":
            case "chineseLegalTraditional":
            case "ideographDigital":
            case "ideographTraditional":
                return toChineseNumeral(value);
            default: return String.valueOf(value);
        }
    }

    /** 数字转字母（1→a，27→aa） */
    private static String toLetter(int n, char base) {
        StringBuilder sb = new StringBuilder();
        while (n > 0) {
            n--;
            sb.insert(0, (char) (base + n % 26));
            n /= 26;
        }
        return sb.length() == 0 ? "1" : sb.toString();
    }

    /** 数字转罗马数字 */
    private static String toRoman(int n) {
        int[] vals = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] syms = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vals.length && n > 0; i++) {
            while (n >= vals[i]) {
                sb.append(syms[i]);
                n -= vals[i];
            }
        }
        return sb.length() == 0 ? String.valueOf(n) : sb.toString();
    }

    /** 数字转中文数字（1→一，11→十一，105→一百零五，支持到 9999） */
    private static String toChineseNumeral(int n) {
        if (n <= 0) return String.valueOf(n);
        String[] digits = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
        if (n < 10) return digits[n];
        if (n == 10) return "十";
        if (n < 20) return "十" + digits[n % 10];
        if (n < 100) {
            int rem = n % 10;
            return digits[n / 10] + "十" + (rem == 0 ? "" : digits[rem]);
        }
        if (n < 1000) {
            int rem = n % 100;
            String mid = rem == 0 ? "" : (rem < 10 ? "零" + digits[rem] : toChineseNumeral(rem));
            return digits[n / 100] + "百" + mid;
        }
        if (n < 10000) {
            int rem = n % 1000;
            String mid = rem == 0 ? "" : (rem < 100 ? "零" + toChineseNumeral(rem) : toChineseNumeral(rem));
            return digits[n / 1000] + "千" + mid;
        }
        return String.valueOf(n);
    }

    /** 安全的 long 解析 */
    private static long parseLongSafe(String s, long def) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * 用 POI 原文作为 textMap 的 key。
     * processContentXml 通过 extractTextFromChildren 提取 XML 段落文本进行匹配，
     * 因此 extractTextFromChildren 必须与 POI getText() 行为完全一致。
     */
    private static Map<String, LinkedList<String>> buildTextMapFromXml(
            String inputPath,
            List<Map.Entry<String, String>> sourceToTarget) throws Exception {

        if (sourceToTarget.isEmpty()) {
            return new LinkedHashMap<>();
        }

        // 直接用 POI 原文作为 textMap 的 key
        Map<String, LinkedList<String>> textMap = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : sourceToTarget) {
            String poiText = normalizeText(entry.getKey());
            if (poiText.isEmpty()) continue;
            textMap.computeIfAbsent(poiText, k -> new LinkedList<>()).add(entry.getValue());
        }

        log.info("[buildTextMapFromXml] sourceToTarget={}, textMap keys={}",
                sourceToTarget.size(), textMap.size());

        // 诊断：dump 所有 textMap key
        int keyIdx = 0;
        for (String key : textMap.keySet()) {
            String snippet = key.length() > 100 ? key.substring(0, 100) + "..." : key;
            log.debug("[buildTextMapFromXml] textMap key#{}: [{}]", keyIdx++, snippet);
        }

        return textMap;
    }

    /**
     * 收集 docx 中需要处理的 XML 文件路径，按 getAllParagraphs() 的顺序排列：
     * document.xml → header1.xml, header2.xml, ... → footer1.xml, footer2.xml, ...
     */
    private static String[] collectXmlPaths(ZipFile zf) {
        List<String> paths = new ArrayList<>();

        // 1. 正文
        if (zf.getEntry("word/document.xml") != null) {
            paths.add("word/document.xml");
        }

        // 2. 页眉（按编号顺序）
        for (int i = 1; i <= 20; i++) {
            String headerPath = "word/header" + i + ".xml";
            if (zf.getEntry(headerPath) != null) {
                paths.add(headerPath);
            } else {
                break;
            }
        }

        // 3. 页脚（按编号顺序）
        for (int i = 1; i <= 20; i++) {
            String footerPath = "word/footer" + i + ".xml";
            if (zf.getEntry(footerPath) != null) {
                paths.add(footerPath);
            } else {
                break;
            }
        }

        return paths.toArray(new String[0]);
    }

    /**
     * 使用 POI 段落列表构建 textMap，确保 key 的顺序与 sourceToTarget 完全一致。
     * <p>
     * 这是解决正文内容错乱的关键：sourceToTarget 是按 POI getAllParagraphs() 的
     * 顺序构建的，如果从 XML 提取段落顺序与之不一致，译文就会被错误分配。
     * 直接用 POI 段落列表提取文本，保证顺序 100% 匹配。
     */
    private static Map<String, LinkedList<String>> buildTextMapFromPoi(
            List<XWPFParagraph> paragraphs,
            List<Map.Entry<String, String>> sourceToTarget) {

        Map<String, LinkedList<String>> textMap = new LinkedHashMap<>();
        int targetIdx = 0;

        for (XWPFParagraph p : paragraphs) {
            if (targetIdx >= sourceToTarget.size()) break;

            String text = p.getText();
            if (text == null || text.isBlank()) continue;

            String normalizedKey = normalizeText(text);
            if (normalizedKey.isEmpty()) continue;

            String translation = sourceToTarget.get(targetIdx).getValue();
            textMap.computeIfAbsent(normalizedKey, k -> new LinkedList<>()).add(translation);
            targetIdx++;
        }

        return textMap;
    }

    /**
     * 处理 content XML（document/header/footer）：文本匹配替换 + 设置字体 + 表格自适应
     */
    private static byte[] processContentXml(byte[] xmlBytes,
                                            Map<String, LinkedList<String>> textMap,
                                            String fontName,
                                            boolean translationFirst,
                                            NumberingModel numberingModel) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}

        Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xmlBytes));

        // 【修复】将活 NodeList 转为静态列表，避免 insertTranslationParagraph 插入新段落时
        // 导致 NodeList 动态变化、迭代顺序混乱
        NodeList liveList = doc.getElementsByTagNameNS(W_NS, "p");
        List<Element> paragraphs = new ArrayList<>(liveList.getLength());
        for (int i = 0; i < liveList.getLength(); i++) {
            paragraphs.add((Element) liveList.item(i));
        }

        // 遍历所有段落，通过文本匹配替换
        int matchedCount = 0;
        int unmatchedCount = 0;
        // 诊断：dump 所有非空段落文本
        int paraIdx = 0;
        for (int i = 0; i < paragraphs.size(); i++) {
            String pt = normalizeText(extractParagraphText(paragraphs.get(i)));
            if (pt.isEmpty()) continue;
            String snippet = pt.length() > 100 ? pt.substring(0, 100) + "..." : pt;
            boolean inMap = textMap.containsKey(pt);
            log.debug("[processContentXml] XML段落#{}: inMap={} [{}]", paraIdx++, inMap, snippet);
        }
        // 双语对照模式下按文档顺序维护编号计数器，用于计算译文序号文本
        Map<String, int[]> numberingCounters = new HashMap<>();
        for (int i = 0; i < paragraphs.size(); i++) {
            Element p = paragraphs.get(i);
            String paraText = normalizeText(extractParagraphText(p));
            if (paraText.isEmpty()) continue;

            // 无论段落是否匹配译文，都推进编号计数并计算序号译文（保证计数连续）
            String numberText = null;
            if (fontName == null && numberingModel != null) {
                numberText = advanceAndRenderNumber(p, numberingModel, numberingCounters);
            }

            LinkedList<String> queue = textMap.get(paraText);
            if (queue != null && !queue.isEmpty()) {
                matchedCount++;
                String translatedText = queue.poll();

                if (fontName == null) {
                    // 双语对照模式：原文保持不变，插入独立的译文段落
                    insertTranslationParagraph(p, translatedText, doc, translationFirst, numberText);
                } else {
                    // 纯翻译模式：替换段落文本
                    replaceParagraphText(p, translatedText);
                    setFontOnParagraph(p, fontName);
                }
            } else {
                unmatchedCount++;
                if (unmatchedCount <= 5) {
                    // 找到最相似的 textMap key，用于对比 POI 与 XML 文本差异
                    String bestKey = null;
                    int bestPrefixLen = 0;
                    for (String key : textMap.keySet()) {
                        int prefixLen = 0;
                        int minLen = Math.min(key.length(), paraText.length());
                        while (prefixLen < minLen && key.charAt(prefixLen) == paraText.charAt(prefixLen)) {
                            prefixLen++;
                        }
                        if (prefixLen > bestPrefixLen) {
                            bestPrefixLen = prefixLen;
                            bestKey = key;
                        }
                    }
                    // 增强诊断：显示差异点附近的字符（含 charCode）
                    log.debug("[processContentXml] 未匹配#{}: 共同前缀={}字符, XML长度={}, POI长度={}",
                            unmatchedCount, bestPrefixLen, paraText.length(), bestKey != null ? bestKey.length() : -1);
                    if (bestKey != null && bestPrefixLen > 0) {
                        // 显示分歧点前后各20个字符
                        int divPoint = bestPrefixLen;
                        int showStart = Math.max(0, divPoint - 20);
                        int xmlShowEnd = Math.min(paraText.length(), divPoint + 30);
                        int poiShowEnd = Math.min(bestKey.length(), divPoint + 30);
                        String xmlAround = paraText.substring(showStart, xmlShowEnd);
                        String poiAround = bestKey.substring(showStart, poiShowEnd);
                        log.debug("  分歧点附近 XML[{}-{}]: [{}]", showStart, xmlShowEnd, xmlAround);
                        log.debug("  分歧点附近 POI[{}-{}]: [{}]", showStart, poiShowEnd, poiAround);
                        // 显示分歧点字符的 charCode
                        if (divPoint < paraText.length() && divPoint < bestKey.length()) {
                            char xmlChar = paraText.charAt(divPoint);
                            char poiChar = bestKey.charAt(divPoint);
                            log.debug("  XML char@{}: '{}' (U+{})", divPoint, xmlChar, String.format("%04X", (int) xmlChar));
                            log.debug("  POI char@{}: '{}' (U+{})", divPoint, poiChar, String.format("%04X", (int) poiChar));
                        }
                    } else if (bestKey != null) {
                        // 共同前缀很短或为0，显示两者开头
                        String xmlHead = paraText.length() > 60 ? paraText.substring(0, 60) : paraText;
                        String poiHead = bestKey.length() > 60 ? bestKey.substring(0, 60) : bestKey;
                        log.debug("  XML开头: [{}]", xmlHead);
                        log.debug("  POI开头: [{}]", poiHead);
                    } else {
                        String xmlSnippet = paraText.length() > 80 ? paraText.substring(0, 80) + "..." : paraText;
                        log.debug("  XML: [{}]", xmlSnippet);
                    }
                    // 输出段落的子元素结构，帮助定位缺失文本在哪个容器内
                    dumpParagraphStructure(p, bestKey);
                }
            }
        }
        log.info("[processContentXml] 段落匹配统计: 总段落={}, 匹配={}, 未匹配={}",
                paragraphs.size(), matchedCount, unmatchedCount);

        // 表格自适应
        enableTableAutofitXml(doc.getDocumentElement());

        // 序列化
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TransformerFactory.newInstance().newTransformer()
                .transform(new DOMSource(doc), new StreamResult(baos));
        return baos.toByteArray();
    }

    /**
     * 在原文段落后插入独立的译文段落（双语对照模式）
     * <p>
     * 译文段落继承原文的格式属性（加粗、字号等），移除原文的编号属性（numPr）
     * 和悬挂缩进（hanging），确保译文顶格显示且不影响原文位置。
     * 当 numberText 非空时（原文段落带自动编号），将翻译后的序号文本
     * （如“Chapter 1”）拼接到译文正文前面，序号沿用译文段落的文本格式。
     *
     * @param translationFirst 译文前置（true=译文插入到原文前面，false=译文插入到原文后面）
     * @param numberText       翻译后的序号文本（null 表示原文段落无编号或不处理）
     */
    private static void insertTranslationParagraph(Element originalP, String translatedText, Document doc,
                                                   boolean translationFirst, String numberText) {
        Node parent = originalP.getParentNode();
        if (parent == null) return;

        // 序号译文拼接到正文前面（保留文本格式，随译文 run 一起写入）
        if (numberText != null && !numberText.isEmpty()) {
            translatedText = numberText + " " + translatedText;
        }
        // 创建新的译文段落
        Element newP = doc.createElementNS(W_NS, "w:p");

        // 复制原文的段落属性，但移除编号和悬挂缩进
        NodeList origPPrList = originalP.getElementsByTagNameNS(W_NS, "pPr");
        if (origPPrList.getLength() > 0) {
            Element origPPr = (Element) origPPrList.item(0);
            Element newPPr = doc.createElementNS(W_NS, "w:pPr");

            // 复制所有子元素，跳过 numPr 和 ind 中的 hanging
            NodeList children = origPPr.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (!(child instanceof Element)) continue;

                Element elem = (Element) child;
                String localName = elem.getLocalName();

                // 跳过编号属性
                if ("numPr".equals(localName)) continue;

                // 对于 ind 元素，克隆但不包含 hanging 属性
                if ("ind".equals(localName)) {
                    Element newInd = doc.createElementNS(W_NS, "w:ind");
                    // 复制除 hanging 外的所有属性
                    for (int j = 0; j < elem.getAttributes().getLength(); j++) {
                        org.w3c.dom.Attr attr = (org.w3c.dom.Attr) elem.getAttributes().item(j);
                        if (!"hanging".equals(attr.getLocalName())) {
                            newInd.setAttributeNodeNS((org.w3c.dom.Attr) attr.cloneNode(true));
                        }
                    }
                    // 如果没有其他属性，设置 left="0" 防止编号默认缩进（译文序号为纯文本）
                    if (newInd.getAttributes().getLength() == 0) {
                        newInd.setAttribute("w:left", "0");
                    }
                    newPPr.appendChild(newInd);
                } else {
                    // 其他属性直接克隆
                    newPPr.appendChild(elem.cloneNode(true));
                }
            }

            // 显式移除编号（numId=0）：原文段落的编号可能由 pStyle 样式定义，
            // 仅跳过直接 numPr 不够，译文段落会从样式继承中文自动编号，
            // 而译文序号已以纯文本拼接，必须彻底取消自动编号
            Element cancelNumPr = doc.createElementNS(W_NS, "w:numPr");
            Element cancelNumId = doc.createElementNS(W_NS, "w:numId");
            cancelNumId.setAttribute("w:val", "0");
            cancelNumPr.appendChild(cancelNumId);
            // 插入到 schema 规定的位置：pStyle 之后，否则放在最前
            Node first = newPPr.getFirstChild();
            if (first instanceof Element && "pStyle".equals(((Element) first).getLocalName())) {
                newPPr.insertBefore(cancelNumPr, first.getNextSibling());
            } else {
                newPPr.insertBefore(cancelNumPr, first);
            }

            newP.appendChild(newPPr);
        }

        // 复制原文中包含 <w:sym>（符号/复选框）的 run，确保译文段落也显示复选框
        NodeList origChildren = originalP.getChildNodes();
        for (int i = 0; i < origChildren.getLength(); i++) {
            Node child = origChildren.item(i);
            if (!(child instanceof Element)) continue;
            Element elem = (Element) child;
            if (!W_NS.equals(elem.getNamespaceURI()) || !"r".equals(elem.getLocalName())) continue;

            NodeList symList = elem.getElementsByTagNameNS(W_NS, "sym");
            if (symList.getLength() > 0) {
                // 这是一个包含符号/复选框的 run，完整克隆到译文段落
                newP.appendChild(elem.cloneNode(true));
            }
        }

        // 创建文本 run 并添加译文
        Element newRun = doc.createElementNS(W_NS, "w:r");

        // 复制原文第一个包含 <w:t> 的 run 的格式属性
        for (int i = 0; i < origChildren.getLength(); i++) {
            Node child = origChildren.item(i);
            if (!(child instanceof Element)) continue;
            Element elem = (Element) child;
            if (!W_NS.equals(elem.getNamespaceURI()) || !"r".equals(elem.getLocalName())) continue;
            NodeList tList = elem.getElementsByTagNameNS(W_NS, "t");
            if (tList.getLength() > 0) {
                NodeList rPrList = elem.getElementsByTagNameNS(W_NS, "rPr");
                if (rPrList.getLength() > 0) {
                    newRun.appendChild(rPrList.item(0).cloneNode(true));
                }
                break;
            }
        }

        // 添加译文文本
        Element t = doc.createElementNS(W_NS, "w:t");
        t.setTextContent(translatedText);
        t.setAttribute("xml:space", "preserve");
        newRun.appendChild(t);

        newP.appendChild(newRun);

        // 根据 translationFirst 决定译文段落插入位置
        if (translationFirst) {
            // 译文前置：插入到原文段落之前
            parent.insertBefore(newP, originalP);
        } else {
            // 译文后置（默认）：插入到原文段落之后
            Node nextSibling = originalP.getNextSibling();
            if (nextSibling != null) {
                parent.insertBefore(newP, nextSibling);
            } else {
                parent.appendChild(newP);
            }
        }
    }

    /**
     * 从段落中提取文本（模拟 POI 的 getText()）
     * <p>
     * 按文档顺序遍历段落内的子元素：
     * - <w:t> 提取文本内容
     * - <w:br/> 插入换行符 \n（与 POI getText() 行为一致）
     * <p>
     * 注意：不处理 <w:tab/>，因为 POI 的 getText() 不包含 tab 字符。
     * 保持与 POI 行为一致是 textMap 匹配的关键。
     */
    private static String extractParagraphText(Element p) {
        StringBuilder sb = new StringBuilder();
        extractTextFromChildren(p, sb);
        return sb.toString();
    }

    /**
     * 诊断方法：输出段落的子元素结构，帮助定位缺失文本在哪个容器内
     */
    private static void dumpParagraphStructure(Element p, String poiKey) {
        NodeList children = p.getChildNodes();
        StringBuilder structure = new StringBuilder();
        structure.append("段落结构: ");
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (!(child instanceof Element)) continue;
            Element elem = (Element) child;
            String name = elem.getLocalName();
            String text = elem.getTextContent();
            String snippet = text.length() > 30 ? text.substring(0, 30) + "..." : text;
            snippet = snippet.replace("\n", "\\n").replace("\r", "\\r");
            structure.append(String.format("<w:%s>[\"%s\"] ", name, snippet));
        }
        log.debug("  {}", structure.toString().trim());
        if (poiKey != null) {
            String poiSnippet = poiKey.length() > 100 ? poiKey.substring(0, 100) + "..." : poiKey;
            log.debug("  期望POI文本: [{}]", poiSnippet);
        }
    }

    /**
     * 提取段落内的所有文本，直接拼接（不加空格）。
     * 经诊断确认：POI 的 getText() 也是直接拼接各 run 的文本，不在 runs 之间添加空格。
     * <p>
     * 处理范围：直接子 <w:r>、<w:hyperlink> 内的 <w:r>。
     * 不递归 <w:ins>/<w:smartTag>/<w:del>（POI 的 getRuns() 不进入这些容器）。
     */
    private static void extractTextFromChildren(Element parent, StringBuilder sb) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (!(child instanceof Element)) continue;
            Element elem = (Element) child;
            String localName = elem.getLocalName();

            if ("t".equals(localName) && W_NS.equals(elem.getNamespaceURI())) {
                sb.append(elem.getTextContent());
            } else if ("br".equals(localName) && W_NS.equals(elem.getNamespaceURI())) {
                sb.append('\n');
            }
            // POI 的 getText() 将 <w:tab/> 转为空格
            // 诊断证实：编号后的分隔（如 "2)\t"）和 "Article 1\tDefinitions" 中的 tab
            else if ("tab".equals(localName) && W_NS.equals(elem.getNamespaceURI())) {
                sb.append(' ');
            }
            // 递归处理 <w:r> 子元素
            else if ("r".equals(localName) && W_NS.equals(elem.getNamespaceURI())) {
                extractTextFromChildren(elem, sb);
            }
            // POI 的 getRuns() 处理 <w:hyperlink> 内的 <w:r>（XWPFHyperlinkRun）
            else if ("hyperlink".equals(localName) && W_NS.equals(elem.getNamespaceURI())) {
                extractTextFromChildren(elem, sb);
            }
            // POI 的 getRuns() 也处理 <w:smartTag> 内的 <w:r>
            // 诊断证实：编号后的空格（如 "2) "）常被包裹在 <w:smartTag> 中
            else if ("smartTag".equals(localName) && W_NS.equals(elem.getNamespaceURI())) {
                extractTextFromChildren(elem, sb);
            }
            // POI 的 getRuns() 也处理 <w:ins> 内的 <w:r>（修订插入内容）
            // 诊断证实："23 " 等文本和编号后的空格可能在 <w:ins> 容器内
            else if ("ins".equals(localName) && W_NS.equals(elem.getNamespaceURI())) {
                extractTextFromChildren(elem, sb);
            }
            // 【关键】不递归 <w:del>（删除内容），POI 的 getText() 不包含已删除文本
        }
    }

    /**
     * 文本规范化：去除首尾空白、压缩连续空白
     */
    private static String normalizeText(String text) {
        if (text == null) return "";
        return text.strip().replaceAll("\\s+", " ");
    }

    /**
     * 替换段落中的文本内容
     * <p>
     * 清除所有 <w:r> 中的 <w:t> 文本，
     * 然后在包含文本的 run 中设置译文。
     * 包含 <w:sym>（符号/复选框）的 run 会被保留不动，
     * 确保 Wingdings 等字体绘制的复选框、符号不会丢失。
     * 如果文本包含换行符 \n，则用 <w:br/> 元素实现换行。
     */
    private static void replaceParagraphText(Element p, String newText) {
        if (newText == null) return;

        Document doc = p.getOwnerDocument();

        // 收集所有直接子 <w:r> 元素
        List<Element> runs = new ArrayList<>();
        NodeList children = p.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element && W_NS.equals(child.getNamespaceURI())
                    && "r".equals(child.getLocalName())) {
                runs.add((Element) child);
            }
        }

        // 也收集 <w:hyperlink> 内的 <w:r>
        NodeList hlList = p.getElementsByTagNameNS(W_NS, "hyperlink");
        for (int i = 0; i < hlList.getLength(); i++) {
            Element hl = (Element) hlList.item(i);
            NodeList hlChildren = hl.getChildNodes();
            for (int j = 0; j < hlChildren.getLength(); j++) {
                Node child = hlChildren.item(j);
                if (child instanceof Element && W_NS.equals(child.getNamespaceURI())
                        && "r".equals(child.getLocalName())) {
                    runs.add((Element) child);
                }
            }
        }

        if (runs.isEmpty()) {
            // 没有 run，创建一个
            Element newRun = doc.createElementNS(W_NS, "w:r");
            appendTextWithBreaks(doc, newRun, newText);
            p.appendChild(newRun);
            return;
        }

        // 清除所有 run 中的 <w:t> 文本（不影响 <w:sym> 等符号元素）
        for (Element run : runs) {
            NodeList tList = run.getElementsByTagNameNS(W_NS, "t");
            for (int i = 0; i < tList.getLength(); i++) {
                tList.item(i).setTextContent("");
            }
        }

        // 找到第一个包含 <w:t> 元素的 run 作为译文写入位置，
        // 跳过仅包含 <w:sym>（符号/复选框）的 run，以保留复选框等视觉元素
        Element targetRun = null;
        for (Element run : runs) {
            NodeList tList = run.getElementsByTagNameNS(W_NS, "t");
            if (tList.getLength() > 0) {
                targetRun = run;
                break;
            }
        }

        if (targetRun == null) {
            // 所有 run 都不含 <w:t>（例如全是符号），在末尾创建新 run
            targetRun = doc.createElementNS(W_NS, "w:r");
            // 复制第一个 run 的 rPr 以保持格式一致
            NodeList rPrList = runs.get(0).getElementsByTagNameNS(W_NS, "rPr");
            if (rPrList.getLength() > 0) {
                targetRun.appendChild(rPrList.item(0).cloneNode(true));
            }
            p.appendChild(targetRun);
        } else {
            // 清理目标 run 中除 <w:rPr> 和 <w:sym> 外的子元素
            List<Node> toRemove = new ArrayList<>();
            NodeList targetChildren = targetRun.getChildNodes();
            for (int i = 0; i < targetChildren.getLength(); i++) {
                Node child = targetChildren.item(i);
                if (child instanceof Element) {
                    Element elem = (Element) child;
                    String localName = elem.getLocalName();
                    // 保留 rPr 和 sym（符号/复选框）
                    if ("rPr".equals(localName) || "sym".equals(localName)) continue;
                    toRemove.add(child);
                } else if (child.getNodeType() == Node.TEXT_NODE) {
                    toRemove.add(child);
                }
            }
            for (Node node : toRemove) {
                targetRun.removeChild(node);
            }
        }

        // 在目标 run 中追加译文（含换行处理）
        appendTextWithBreaks(doc, targetRun, newText);
    }

    /**
     * 向 run 中追加文本，将 \n 转换为 <w:br/> + <w:t>
     */
    private static void appendTextWithBreaks(Document doc, Element run, String text) {
        String[] lines = text.split("\\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                // 添加 <w:br/> 换行元素
                Element br = doc.createElementNS(W_NS, "w:br");
                run.appendChild(br);
            }
            // 添加 <w:t> 文本元素
            Element t = doc.createElementNS(W_NS, "w:t");
            t.setTextContent(lines[i]);
            t.setAttribute("xml:space", "preserve");
            run.appendChild(t);
        }
    }

    /**
     * 设置段落中所有 run 的字体
     */
    private static void setFontOnParagraph(Element p, String fontName) {
        // 处理直接子 <w:r>
        List<Element> runs = new ArrayList<>();
        NodeList children = p.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element && W_NS.equals(child.getNamespaceURI())
                    && "r".equals(child.getLocalName())) {
                runs.add((Element) child);
            }
        }
        // 处理 <w:hyperlink> 内的 <w:r>
        NodeList hlList = p.getElementsByTagNameNS(W_NS, "hyperlink");
        for (int i = 0; i < hlList.getLength(); i++) {
            Element hl = (Element) hlList.item(i);
            NodeList hlChildren = hl.getChildNodes();
            for (int j = 0; j < hlChildren.getLength(); j++) {
                Node child = hlChildren.item(j);
                if (child instanceof Element && W_NS.equals(child.getNamespaceURI())
                        && "r".equals(child.getLocalName())) {
                    runs.add((Element) child);
                }
            }
        }

        for (Element run : runs) {
            // 跳过包含 <w:sym>（符号/复选框）的 run，
            // 因为符号字体（如 Wingdings 2）不能被覆盖为目标字体
            NodeList symList = run.getElementsByTagNameNS(W_NS, "sym");
            if (symList.getLength() > 0) continue;

            setFontOnRun(run, fontName);
        }
    }

    /**
     * 设置单个 run 的字体（修改 <w:rPr><w:rFonts>）
     */
    private static void setFontOnRun(Element run, String fontName) {
        Document doc = run.getOwnerDocument();

        // 查找或创建 <w:rPr>
        Element rPr = null;
        NodeList children = run.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element && W_NS.equals(child.getNamespaceURI())
                    && "rPr".equals(child.getLocalName())) {
                rPr = (Element) child;
                break;
            }
        }
        if (rPr == null) {
            rPr = doc.createElementNS(W_NS, "w:rPr");
            run.insertBefore(rPr, run.getFirstChild());
        }

        // 移除已有的 <w:rFonts>，添加新的
        // 【修复】用 getChildNodes 遍历直接子元素，避免 getElementsByTagNameNS
        // 返回 live NodeList 导致 removeChild 时 NOT_FOUND_ERR
        List<Node> toRemove = new ArrayList<>();
        NodeList rPrChildren = rPr.getChildNodes();
        for (int i = 0; i < rPrChildren.getLength(); i++) {
            Node child = rPrChildren.item(i);
            if (child instanceof Element && W_NS.equals(child.getNamespaceURI())
                    && "rFonts".equals(child.getLocalName())) {
                toRemove.add(child);
            }
        }
        for (Node node : toRemove) {
            rPr.removeChild(node);
        }
        Element fonts = doc.createElementNS(W_NS, "w:rFonts");
        fonts.setAttribute("w:ascii", fontName);
        fonts.setAttribute("w:hAnsi", fontName);
        fonts.setAttribute("w:eastAsia", fontName);
        rPr.insertBefore(fonts, rPr.getFirstChild());
    }

    /**
     * 在 styles.xml 中设置默认字体
     */
    private static byte[] setDefaultFontInStyles(byte[] xmlBytes, String fontName) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xmlBytes));

        // 查找 <w:docDefaults> 中的 <w:rPrDefault> / <w:rPr>
        NodeList docDefaultsList = doc.getElementsByTagNameNS(W_NS, "docDefaults");
        if (docDefaultsList.getLength() > 0) {
            Element docDefaults = (Element) docDefaultsList.item(0);
            NodeList rPrDefaultList = docDefaults.getElementsByTagNameNS(W_NS, "rPrDefault");
            if (rPrDefaultList.getLength() > 0) {
                Element rPrDefault = (Element) rPrDefaultList.item(0);
                NodeList rPrList = rPrDefault.getElementsByTagNameNS(W_NS, "rPr");
                if (rPrList.getLength() > 0) {
                    setFontOnRun((Element) rPrList.item(0), fontName);
                }
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TransformerFactory.newInstance().newTransformer()
                .transform(new DOMSource(doc), new StreamResult(baos));
        return baos.toByteArray();
    }

    /**
     * XML 方式实现表格自适应
     */
    private static void enableTableAutofitXml(Element root) {
        NodeList tables = root.getElementsByTagNameNS(W_NS, "tbl");
        for (int t = 0; t < tables.getLength(); t++) {
            Element tbl = (Element) tables.item(t);
            try {
                // 查找或创建 <w:tblPr>
                Element tblPr = null;
                NodeList children = tbl.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child instanceof Element && W_NS.equals(child.getNamespaceURI())
                            && "tblPr".equals(child.getLocalName())) {
                        tblPr = (Element) child;
                        break;
                    }
                }
                if (tblPr == null) {
                    tblPr = tbl.getOwnerDocument().createElementNS(W_NS, "w:tblPr");
                    tbl.insertBefore(tblPr, tbl.getFirstChild());
                }

                // 移除现有的 <w:tblLayout>
                // 【修复】用 getChildNodes 遍历直接子元素，避免 NOT_FOUND_ERR
                List<Node> layoutsToRemove = new ArrayList<>();
                NodeList tblPrChildren = tblPr.getChildNodes();
                for (int i = 0; i < tblPrChildren.getLength(); i++) {
                    Node child = tblPrChildren.item(i);
                    if (child instanceof Element && W_NS.equals(child.getNamespaceURI())
                            && "tblLayout".equals(child.getLocalName())) {
                        layoutsToRemove.add(child);
                    }
                }
                for (Node node : layoutsToRemove) {
                    tblPr.removeChild(node);
                }

                // 添加 autofit 布局
                Element layout = tbl.getOwnerDocument().createElementNS(W_NS, "w:tblLayout");
                layout.setAttribute("w:type", "autofit");
                tblPr.appendChild(layout);
            } catch (Exception ignored) {}
        }
    }
}