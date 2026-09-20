package cn.iocoder.sva.module.ai.service.translation.tran.common;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 翻译相关常量定义
 * <p>
 * 集中管理翻译系统使用的各类常量，包括：
 * <ul>
 *   <li>定价常量 — LLM 调用的 token 单价</li>
 *   <li>翻译状态常量 — 翻译对的状态标记</li>
 *   <li>列表符号常量 — 用于识别文档中的列表项</li>
 *   <li>数字正则模式 — 用于数字相关术语的匹配和质检</li>
 *   <li>单位别名映射 — 统一不同表示法的度量单位</li>
 * </ul>
 */
public final class TranslationConstants {

    /** 构造私有化，常量类不允许实例化 */
    private TranslationConstants() {
    }

    // ===================== 翻译状态常量 =====================

    /** 翻译正常 — 已成功翻译 */
    public static final String STATUS_OK = "ok";

    /** 翻译缺失 — 未能完成翻译（译文为空或异常） */
    public static final String STATUS_MISS = "miss";

    /** 无需翻译 — 原文不含需要翻译的内容（如纯数字、纯公式） */
    public static final String STATUS_NO_NEED = "no_need";

    /** 术语完全命中 — 原文完全匹配术语库中的某条术语 */
    public static final String STATUS_FULL_HIT = "full_hit";

    /** 术语部分命中 — 原文部分匹配术语库中的某条术语 */
    public static final String STATUS_PARTIAL_HIT = "partial_hit";

    // ===================== 列表符号常量 =====================

    /**
     * 常见列表符号集合
     * <p>
     * 用于识别文档中的列表项标记（如 "1."、"2."、"a)"、"•" 等），
     * 翻译时需保留这些标记的格式。
     * <p>
     * 包含：
     * <ul>
     *   <li>常见编号符号：•, ·, ◦, ▪, ▫, ►, ‣, ⁃</li>
     *   <li>常见项目符号：-, *, +</li>
     *   <li>中文序号：〇, 一, 二, ... , 十</li>
     * </ul>
     */
    public static final Set<String> BULLET_TOKENS;

    static {
        Set<String> bullets = new HashSet<>();
        // 编号符号
        Collections.addAll(bullets, "•", "·", "◦", "▪", "▫", "►", "‣", "⁃");
        // 项目符号
        Collections.addAll(bullets, "-", "*", "+");
        // 中文数字序号
        Collections.addAll(bullets, "〇", "一", "二", "三", "四", "五",
                "六", "七", "八", "九", "十");
        BULLET_TOKENS = Collections.unmodifiableSet(bullets);
    }

    // ===================== 数字正则模式 =====================

    /**
     * 数字匹配正则模式列表
     * <p>
     * 用于识别文本中的数字和数字相关术语，支持以下格式：
     * <ul>
     *   <li>纯整数：123, -45</li>
     *   <li>小数：3.14, -0.5</li>
     *   <li>科学计数法：1.5e10, 2E-3</li>
     *   <li>带千分位分隔符：1,000,000</li>
     *   <li>中文数字：一二三（仅基础匹配）</li>
     *   <li>百分号：50%, 12.5%</li>
     *   <li>带单位数字：5kg, 3.2mm, 100°C</li>
     * </ul>
     * <p>
     * 这些模式在质检（QC）中用于检查数字是否被正确翻译保留。
     */
    public static final List<Pattern> NUM_PATTERNS;

    static {
        List<Pattern> patterns = new ArrayList<>();
        // 整数和小数（含负数）
        patterns.add(Pattern.compile("-?\\d+(?:\\.\\d+)?"));
        // 科学计数法
        patterns.add(Pattern.compile("-?\\d+(?:\\.\\d+)?[eE][+-]?\\d+"));
        // 百分数
        patterns.add(Pattern.compile("-?\\d+(?:\\.\\d+)?%"));
        // 带千分位的数字
        patterns.add(Pattern.compile("\\d{1,3}(?:,\\d{3})+(?:\\.\\d+)?"));
        // 数字+单位（如 5kg, 3.2mm, 100°C）
        patterns.add(Pattern.compile("-?\\d+(?:\\.\\d+)?\\s*[a-zA-Z°℃%]+"));
        NUM_PATTERNS = Collections.unmodifiableList(patterns);
    }

    // ===================== 单位别名映射 =====================

    /**
     * 单位别名映射表
     * <p>
     * 将同一物理量的不同表示法统一为标准形式，
     * 用于术语匹配和质检中的单位一致性检查。
     * <p>
     * key 为别名形式，value 为标准形式。
     * <p>
     * 示例：
     * <ul>
     *   <li>"mm" → "毫米"（英制→公制中文名）</li>
     *   <li>"kg" → "千克"</li>
     *   <li>"℃" → "°C"（全角符号→标准符号）</li>
     *   <li>"m²" → "平方米"</li>
     * </ul>
     */
    public static final Map<String, String> UNIT_ALIASES;

    static {
        Map<String, String> aliases = new HashMap<>();
        // 长度单位
        aliases.put("mm", "毫米");
        aliases.put("cm", "厘米");
        aliases.put("m", "米");
        aliases.put("km", "千米");
        aliases.put("in", "英寸");
        aliases.put("ft", "英尺");
        // 重量单位
        aliases.put("mg", "毫克");
        aliases.put("g", "克");
        aliases.put("kg", "千克");
        aliases.put("lb", "磅");
        // 面积单位
        aliases.put("m²", "平方米");
        aliases.put("m2", "平方米");
        aliases.put("cm²", "平方厘米");
        aliases.put("cm2", "平方厘米");
        // 体积单位
        aliases.put("ml", "毫升");
        aliases.put("mL", "毫升");
        aliases.put("l", "升");
        aliases.put("L", "升");
        // 温度单位
        aliases.put("℃", "°C");
        aliases.put("°C", "°C");
        aliases.put("°F", "°F");
        // 时间单位
        aliases.put("ms", "毫秒");
        aliases.put("s", "秒");
        aliases.put("min", "分钟");
        aliases.put("h", "小时");
        // 其他
        aliases.put("rpm", "转/分");
        aliases.put("RPM", "转/分");
        aliases.put("Hz", "赫兹");
        aliases.put("kHz", "千赫兹");
        aliases.put("MHz", "兆赫兹");
        aliases.put("GHz", "吉赫兹");
        aliases.put("W", "瓦");
        aliases.put("kW", "千瓦");
        aliases.put("MW", "兆瓦");
        aliases.put("V", "伏");
        aliases.put("kV", "千伏");
        aliases.put("A", "安");
        aliases.put("mA", "毫安");
        aliases.put("Pa", "帕");
        aliases.put("kPa", "千帕");
        aliases.put("MPa", "兆帕");

        UNIT_ALIASES = Collections.unmodifiableMap(aliases);
    }
}
