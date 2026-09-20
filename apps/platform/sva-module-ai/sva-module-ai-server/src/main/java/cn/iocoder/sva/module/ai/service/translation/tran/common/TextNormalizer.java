package cn.iocoder.sva.module.ai.service.translation.tran.common;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本规范化工具类
 * <p>
 * 对应 Python 项目 glossary.py 中的规范化函数。
 * 这是术语匹配的核心基础工具类，提供全角半角转换、空白处理、
 * 去空白映射等关键功能。
 * <p>
 * <b>核心概念说明：</b>
 * <ul>
 *   <li><b>全角→半角</b>：中文输入法下的英文字母、数字、标点为全角字符（Unicode FF00-FFEF区块），
 *       术语匹配前需统一转为半角，否则 "ABC"（全角）和 "ABC"（半角）无法匹配</li>
 *   <li><b>去空白映射</b>：去除文本中的空白字符后，需要建立原文位置→规范化文本位置的映射，
 *       以便在原文中精确定位术语匹配的起止位置，实现术语高亮和替换</li>
 * </ul>
 */
public class TextNormalizer {

    /**
     * 全角字符到半角字符的偏移量
     * <p>
     * 全角空格 (U+3000) 特殊处理，其他全角字符 (U+FF01 ~ U+FF5E) 
     * 减去此偏移量即可得到对应半角字符。
     * 例如：全角 'A' (U+FF21) - 0xFEE0 = 半角 'A' (U+0041)
     */
    private static final int FULLWIDTH_OFFSET = 0xFEE0;

    /** 构造私有化，工具类不允许实例化 */
    private TextNormalizer() {
    }

    // ===================== 全角半角转换 =====================

    /**
     * 将字符串中的全角字符转换为半角字符
     * <p>
     * 转换规则：
     * <ul>
     *   <li>全角空格 (U+3000) → 半角空格 (U+0020)</li>
     *   <li>其他全角字符 (U+FF01 ~ U+FF5E) → 对应半角字符 (U+0021 ~ U+007E)</li>
     *   <li>非全角字符保持不变</li>
     * </ul>
     * <p>
     * 示例：
     * <pre>
     * toHalfwidth("ＡＢＣ１２３") → "ABC123"
     * toHalfwidth("　hello") → " hello"
     * </pre>
     *
     * @param s 待转换的字符串，可以为 null
     * @return 转换后的半角字符串，null 输入返回 null
     */
    public static String toHalfwidth(String s) {
        if (s == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\u3000') {
                // 全角空格 → 半角空格
                sb.append(' ');
            } else if (c >= '\uFF01' && c <= '\uFF5E') {
                // 全角字符 → 半角字符：减去偏移量 0xFEE0
                // 例如：全角 '！' (FF01) - FEE0 = 半角 '!' (0021)
                sb.append((char) (c - FULLWIDTH_OFFSET));
            } else {
                // 非全角字符保持不变
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ===================== 术语键值规范化 =====================

    /**
     * 术语键规范化
     * <p>
     * 用于术语匹配时的键规范化处理，确保不同输入格式的术语能够正确匹配。
     * <p>
     * 处理步骤：
     * <ol>
     *   <li>全角转半角 — 统一英文和数字的宽度形式</li>
     *   <li>去除首尾空白</li>
     *   <li>如果包含中文字符，则去除所有空白（中文语境下空白不影响语义）</li>
     * </ol>
     * <p>
     * 示例：
     * <pre>
     * normalizeKey(" ＡＢＣ ") → "ABC"              （纯英文：去首尾空白+全角转半角）
     * normalizeKey(" 数据 库 ") → "数据库"            （含中文：去所有空白+全角转半角）
     * normalizeKey(" Ｏracle ") → "Oracle"           （含中文'Ｏ'为全角：去所有空白）
     * </pre>
     *
     * @param s 待规范化的字符串
     * @return 规范化后的字符串
     */
    public static String normalizeKey(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        // 1. 全角转半角
        String result = toHalfwidth(s);
        // 2. 去除首尾空白
        result = result.trim();
        // 3. 如果包含中文字符，去除所有空白
        if (containsChinese(result)) {
            result = result.replaceAll("\\s+", "");
        }
        return result;
    }

    /**
     * 术语值规范化
     * <p>
     * 用于术语匹配时的值规范化处理，规则与 normalizeKey 一致。
     * 术语的 source（原文）和 target（译文）都需要规范化后才能匹配。
     *
     * @param s 待规范化的字符串
     * @return 规范化后的字符串
     */
    public static String normalizeValue(String s) {
        return normalizeKey(s);
    }

    // ===================== 去空白映射 =====================

    /**
     * 去空白映射结果
     * <p>
     * 包含去除空白后的规范化文本和位置映射数组。
     * <p>
     * <b>mapping 数组的含义：</b>
     * <ul>
     *   <li>mapping[i] = 原始文本中第 i 个字符在规范化文本中对应的位置</li>
     *   <li>如果原始字符是空白，则 mapping[i] 指向下一个非空白字符的位置</li>
     * </ul>
     * <p>
     * 用途：在原文中定位术语匹配位置时，先在规范化文本中找到匹配位置，
     * 然后通过 mapping 反查回原文位置，实现精确定位。
     */
    public static class StripResult {
        /** 去除空白后的规范化文本 */
        private final String normText;
        /**
         * 位置映射数组
         * <p>
         * mapping[i] 表示原文第 i 个字符对应规范化文本中的位置。
         * 长度与原始文本长度相同。
         */
        private final int[] mapping;

        public StripResult(String normText, int[] mapping) {
            this.normText = normText;
            this.mapping = mapping;
        }

        public String getNormText() {
            return normText;
        }

        public int[] getMapping() {
            return mapping;
        }
    }

    /**
     * 去除文本中的空白字符，并建立位置映射
     * <p>
     * 本方法是术语精确替换的核心。流程如下：
     * <ol>
     *   <li>遍历原始文本的每个字符</li>
     *   <li>跳过空白字符（空格、制表符、换行等）</li>
     *   <li>非空白字符追加到规范化文本</li>
     *   <li>对于每个原始位置 i，记录 mapping[i] = 该位置在规范化文本中的对应位置</li>
     * </ol>
     * <p>
     * <b>映射逻辑详解：</b>
     * <pre>
     * 原始文本:  "A B C"
     * 位置:       0 1 2 3 4
     * 规范文本:  "ABC"
     * 映射:      mapping[0]=0, mapping[1]=1（空白指向下一个非空位置）,
     *            mapping[2]=1, mapping[3]=2, mapping[4]=2
     * </pre>
     * <p>
     * 通过此映射，可以将在规范化文本中找到的匹配区间 [normStart, normEnd)
     * 反查为原始文本中的区间，从而在原文中进行高亮或替换操作。
     *
     * @param text 原始文本
     * @return StripResult 包含规范化文本和位置映射
     */
    public static StripResult stripWhitespaceWithMapping(String text) {
        if (text == null || text.isEmpty()) {
            return new StripResult("", new int[0]);
        }

        int len = text.length();
        int[] mapping = new int[len];
        StringBuilder normBuilder = new StringBuilder(len);

        // normPos 追踪规范化文本的当前位置
        int normPos = 0;

        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                // 空白字符：映射到下一个非空白字符的位置
                // 如果后面没有非空白字符，则映射到规范化文本末尾
                mapping[i] = normPos;
            } else {
                normBuilder.append(c);
                mapping[i] = normPos;
                normPos++;
            }
        }

        return new StripResult(normBuilder.toString(), mapping);
    }

    // ===================== CJK字符判断 =====================

    /**
     * 判断字符是否为中文字符（CJK统一表意文字）
     * <p>
     * 判断范围基于 Unicode CJK 统一表意文字区块：
     * <ul>
     *   <li>U+4E00 ~ U+9FFF：CJK统一表意文字（基本区，最常用）</li>
     *   <li>U+3400 ~ U+4DBF：CJK统一表意文字扩展A</li>
     *   <li>U+20000 ~ U+2A6DF：CJK统一表意文字扩展B</li>
     *   <li>U+2A700 ~ U+2B73F：CJK统一表意文字扩展C</li>
     *   <li>U+2B740 ~ U+2B81F：CJK统一表意文字扩展D</li>
     *   <li>U+F900 ~ U+FAFF：CJK兼容表意文字</li>
     * </ul>
     * <p>
     * 同时包含 CJK 标点符号范围：
     * <ul>
     *   <li>U+3000 ~ U+303F：CJK符号和标点</li>
     * </ul>
     *
     * @param c 待判断的字符
     * @return true 如果是中文字符或CJK标点
     */
    public static boolean isChinese(char c) {
        // CJK基本区：常用汉字
        if (c >= '\u4E00' && c <= '\u9FFF') return true;
        // CJK扩展A区
        if (c >= '\u3400' && c <= '\u4DBF') return true;
        // CJK兼容表意文字
        if (c >= '\uF900' && c <= '\uFAFF') return true;
        // CJK符号和标点（如：、。「」等）
        if (c >= '\u3000' && c <= '\u303F') return true;
        // CJK扩展B区及以后（代理对范围）
        if (c >= '\uD840' && c <= '\uD868') return true;
        // CJK扩展区补充
        if (c == '\uD869') return true;
        return false;
    }

    /**
     * 判断字符串中是否包含中文字符
     *
     * @param s 待判断的字符串
     * @return true 如果包含至少一个中文字符
     */
    public static boolean containsChinese(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (isChinese(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    // ===================== 映射反查辅助 =====================

    /**
     * 根据去空白映射，将规范化文本中的区间反查为原始文本中的区间
     * <p>
     * 给定规范化文本中的匹配位置 [normStart, normEnd)，
     * 通过 mapping 数组找到原始文本中对应的 [origStart, origEnd) 区间。
     * <p>
     * 反查逻辑：
     * <ul>
     *   <li>origStart: 找到 mapping[i] == normStart 的最小 i</li>
     *   <li>origEnd: 找到 mapping[i] == normEnd 的最小 i（如果存在），
     *       或原文长度（如果 normEnd 是规范化文本末尾）</li>
     * </ul>
     *
     * @param mapping   位置映射数组
     * @param normStart 规范化文本中的起始位置（含）
     * @param normEnd   规范化文本中的结束位置（不含）
     * @return int[2] 数组，[0]=原始起始位置，[1]=原始结束位置
     */
    public static int[] mapBackToOriginal(int[] mapping, int normStart, int normEnd) {
        int origStart = -1;
        int origEnd = -1;

        for (int i = 0; i < mapping.length; i++) {
            if (mapping[i] == normStart && origStart == -1) {
                origStart = i;
            }
            if (mapping[i] == normEnd && origEnd == -1) {
                origEnd = i;
                break;
            }
        }

        // 如果 normEnd 超出映射范围（即匹配到规范化文本末尾），
        // 则 origEnd 取原始文本长度
        if (origEnd == -1) {
            origEnd = mapping.length;
        }

        return new int[]{origStart, origEnd};
    }
}
