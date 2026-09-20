package cn.iocoder.sva.module.ai.service.translation.tran.common;

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Word文档操作工具类
 * <p>
 * 对应 Python 项目 docx_utils.py，提供段落遍历、翻译应用、样式分组等功能。
 * 使用 Apache POI 库操作 Word 文档。
 */
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
                    if (instrText != null && instrText.toUpperCase().contains("TOC")) {
                        return true;
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
     */
    private static void setEastAsiaFont(XWPFRun run, String fontName) {
        try {
            CTR ctr = run.getCTR();
            CTRPr rPr = ctr.isSetRPr() ? ctr.getRPr() : ctr.addNewRPr();
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
}
