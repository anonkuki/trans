package cn.iocoder.sva.module.ai.service.translation.tran.common;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文档质量检查工具
 * <p>
 * 对应 Python 项目 deterministic_qc.py，提供数字、缩写、长度等检查功能。
 */
public class DocQualityChecker {

    /** 数字匹配正则：匹配整数、小数、带分隔符的数字 */
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]+(?:[.,/:][0-9]+)*");

    /** 缩写匹配正则：匹配连续大写字母，使用 ASCII 字母环视代替 \b 以兼容中英文混合文本 */
    private static final Pattern ABBR_PATTERN = Pattern.compile("(?<![A-Za-z])[A-Z]{2,}(?![A-Za-z])");

    private DocQualityChecker() {
        // 工具类，禁止实例化
    }

    /**
     * 质量检查结果
     */
    public static class QcResult {
        private final boolean ok;
        private final String status;
        private final String errorType;

        public QcResult(boolean ok, String status, String errorType) {
            this.ok = ok;
            this.status = status;
            this.errorType = errorType;
        }

        public static QcResult ok() {
            return new QcResult(true, "OK", null);
        }

        public static QcResult error(String status) {
            String errorType = status.startsWith("[ERROR]") ? "error" : "warn";
            // WARN 级别不阻断翻译流程（如缩写被翻译为目标语言）
            boolean ok = "warn".equals(errorType);
            return new QcResult(ok, status, errorType);
        }

        public boolean isOk() {
            return ok;
        }

        public String getStatus() {
            return status;
        }

        public String getErrorType() {
            return errorType;
        }
    }

    /**
     * 执行质量检查
     * <p>
     * 检查项目：
     * <ul>
     *   <li>数字完整性：原文中的数字是否在译文中保留</li>
     *   <li>缩写完整性：原文中的缩写是否在译文中保留</li>
     *   <li>长度比例：译文长度与原文的比例是否合理</li>
     * </ul>
     *
     * @param source 原文
     * @param target 译文
     * @return 检查结果
     */
    public static QcResult check(String source, String target) {
        if (source == null) source = "";
        if (target == null) target = "";

        // 检查数字
        QcResult numberCheck = checkNumbers(source, target);
        if (!numberCheck.isOk()) {
            return numberCheck;
        }

        // 检查缩写
        QcResult abbrCheck = checkAbbreviations(source, target);
        if (!abbrCheck.isOk()) {
            return abbrCheck;
        }

        // 检查长度比例
        QcResult lengthCheck = checkLengthRatio(source, target);
        if (!lengthCheck.isOk()) {
            return lengthCheck;
        }

        return QcResult.ok();
    }

    /**
     * 检查数字完整性
     * <p>
     * 比较纯数字值（去除分隔符），避免因语言间数字格式差异（如西班牙语 1.700 = 中文 1700）导致误报。
     */
    public static QcResult checkNumbers(String source, String target) {
        Set<String> srcNums = extractMatches(NUMBER_PATTERN, source);
        Set<String> tgtNums = extractMatches(NUMBER_PATTERN, target);

        for (String num : srcNums) {
            if (tgtNums.contains(num)) {
                continue;
            }
            // 格式不同但纯数字相同也视为匹配（如 1.700 vs 1700, 8/10 vs 8 10）
            String normalizedSrc = num.replaceAll("[.,/:]", "");
            boolean found = false;
            for (String tgtNum : tgtNums) {
                String normalizedTgt = tgtNum.replaceAll("[.,/:]", "");
                if (normalizedSrc.equals(normalizedTgt)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return QcResult.error("[ERROR] 数字丢失: " + num);
            }
        }

        return QcResult.ok();
    }

    /**
     * 检查缩写完整性
     * <p>
     * 缩写在翻译中可能被展开（如 USA → 美国，CQV 保留），
     * 因此缩写丢失只作为警告而非错误，不阻断翻译流程。
     */
    public static QcResult checkAbbreviations(String source, String target) {
        Set<String> srcAbbrs = extractMatches(ABBR_PATTERN, source);
        Set<String> tgtAbbrs = extractMatches(ABBR_PATTERN, target);

        for (String abbr : srcAbbrs) {
            if (!tgtAbbrs.contains(abbr)) {
                // 缩写可能被翻译为目标语言（如 USA → 美国），降级为 WARN
                return QcResult.error("[WARN] 缩写丢失: " + abbr);
            }
        }

        return QcResult.ok();
    }

    /**
     * 检查长度比例
     */
    public static QcResult checkLengthRatio(String source, String target) {
        int srcLen = source.length();
        int tgtLen = target.length();

        // 原文太短时跳过长度检查
        if (srcLen < 15) {
            return QcResult.ok();
        }

        double ratio = (double) tgtLen / Math.max(1, srcLen);

        if (ratio > 10) {
            return QcResult.error("[WARN] 译文过长 ratio=" + String.format("%.2f", ratio));
        }

        if (ratio < 0.1) {
            return QcResult.error("[WARN] 译文过短 ratio=" + String.format("%.2f", ratio));
        }

        return QcResult.ok();
    }

    /**
     * 提取正则匹配结果
     */
    private static Set<String> extractMatches(Pattern pattern, String text) {
        Set<String> matches = new HashSet<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            matches.add(matcher.group());
        }
        return matches;
    }
}
