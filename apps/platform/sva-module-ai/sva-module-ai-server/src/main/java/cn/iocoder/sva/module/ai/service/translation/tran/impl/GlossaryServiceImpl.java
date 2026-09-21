package cn.iocoder.sva.module.ai.service.translation.tran.impl;

import cn.iocoder.sva.module.ai.service.translation.tran.GlossaryService;
import cn.iocoder.sva.module.ai.service.translation.tran.common.TextNormalizer;
import cn.iocoder.sva.module.ai.service.translation.tran.config.TransDocProperties;
import cn.iocoder.sva.module.ai.service.translation.tran.model.GlossaryItem;
import cn.iocoder.sva.module.ai.service.translation.tran.model.GlossaryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 术语库管理服务实现
 * <p>
 * 核心特性：
 * <ul>
 *   <li>支持 JSON 格式术语库加载/保存</li>
 *   <li>首字符索引 + LRU 缓存优化（支持 10~30 万术语规模）</li>
 *   <li>中文术语：去空白视图匹配（忽略空白差异）</li>
 *   <li>英文术语：简单子串匹配</li>
 *   <li>最长优先 + 非重叠贪心策略</li>
 *   <li>从后往前替换避免下标偏移</li>
 * </ul>
 */
@Slf4j
@Service
public class GlossaryServiceImpl implements GlossaryService {

    private final TransDocProperties properties;
    private final ObjectMapper objectMapper;

    /** CJK字符正则 */
    private static final Pattern CJK_PATTERN = Pattern.compile("[\\u4e00-\\u9fff]");
    /** CJK或字母数字正则（用于首字符索引分桶） */
    private static final Pattern CJK_ALNUM_PATTERN = Pattern.compile("[A-Za-z0-9\\u4e00-\\u9fff]");
    /** 连续英文单词正则 */
    private static final Pattern WORD_PATTERN = Pattern.compile("[A-Za-z]+");

    public GlossaryServiceImpl(TransDocProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
    }

    // ==================== 基础 I/O ====================

    @Override
    public Map<String, String> loadGlossary() {
        String path = properties.getGlossaryPath();
        if (path == null || path.isEmpty()) {
            path = "glossary.json";
        }

        String content = null;
        Path filePath = Paths.get(path);

        // 首先尝试从文件系统加载
        if (Files.exists(filePath)) {
            try {
                content = Files.readString(filePath, StandardCharsets.UTF_8);
                log.debug("从文件系统加载术语库: {}", path);
            } catch (Exception e) {
                log.warn("读取文件系统术语库失败: {}", e.getMessage());
            }
        }

        // 如果文件系统不存在，尝试从 classpath 加载
        if (content == null) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
                if (is != null) {
                    content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    log.debug("从 classpath 加载术语库: {}", path);
                }
            } catch (Exception e) {
                log.warn("从 classpath 加载术语库失败: {}", e.getMessage());
            }
        }

        if (content == null) {
            log.warn("未找到术语库文件 {}，返回空词典", path);
            return new ConcurrentHashMap<>();
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = objectMapper.readValue(content, Map.class);

            Map<String, String> glossary = new ConcurrentHashMap<>();
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                String key = TextNormalizer.normalizeKey(entry.getKey());
                String value = entry.getValue() != null
                        ? TextNormalizer.normalizeValue(entry.getValue().toString())
                        : "";
                if (key != null && !key.isEmpty() && !value.isEmpty()) {
                    glossary.put(key, value);
                }
            }

            log.info("已加载术语库 {} 条", glossary.size());
            return glossary;
        } catch (Exception e) {
            log.error("加载术语库失败: {}", e.getMessage(), e);
            return new ConcurrentHashMap<>();
        }
    }

    @Override
    public void saveGlossary(Map<String, String> glossary) {
        String path = properties.getGlossaryPath();
        if (path == null || path.isEmpty()) {
            path = "glossary.json";
        }

        try {
            Path filePath = Paths.get(path);
            Files.createDirectories(filePath.getParent());

            String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(glossary);
            Files.writeString(filePath, json, StandardCharsets.UTF_8);

            log.info("术语库已写入 {}（{} 条）", path, glossary.size());
        } catch (Exception e) {
            log.error("写入术语库失败: {}", e.getMessage(), e);
            throw new RuntimeException("写入术语库失败", e);
        }
    }

    @Override
    public int mergeGlossary(Map<String, String> newTerms) {
        if (newTerms == null || newTerms.isEmpty()) {
            return 0;
        }

        Map<String, String> current = loadGlossary();
        int added = 0;
        int maxKeyLen = 60;

        for (Map.Entry<String, String> entry : newTerms.entrySet()) {
            String key = TextNormalizer.normalizeKey(entry.getKey());
            String value = entry.getValue() != null
                    ? TextNormalizer.normalizeValue(entry.getValue())
                    : "";

            if (key == null || key.isEmpty() || value.isEmpty()) {
                continue;
            }
            if (key.length() > maxKeyLen) {
                log.debug("忽略超长术语键（>{}）：{}", maxKeyLen, key);
                continue;
            }

            if (!current.containsKey(key)) {
                current.put(key, value);
                added++;
            } else if (!current.get(key).equals(value)) {
                current.put(key, value);
                added++;
            }
        }

        saveGlossary(current);
        return added;
    }

    @Override
    public GlossaryResponse searchGlossary(String query) {
        Map<String, String> glossary = loadGlossary();

        List<GlossaryItem> items;
        if (query == null || query.trim().isEmpty()) {
            items = glossary.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> new GlossaryItem(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        } else {
            String lowerQuery = query.toLowerCase();
            items = glossary.entrySet().stream()
                    .filter(e -> e.getKey().toLowerCase().contains(lowerQuery))
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> new GlossaryItem(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        }

        return new GlossaryResponse(items, items.size());
    }

    @Override
    public int importFromExcel(String excelPath) {
        try {
            // 使用 Apache POI 读取 Excel
            org.apache.poi.ss.usermodel.Workbook wb = org.apache.poi.ss.usermodel.WorkbookFactory.create(
                    new FileInputStream(excelPath));

            Map<String, String> terms = new HashMap<>();

            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                org.apache.poi.ss.usermodel.Sheet sheet = wb.getSheetAt(i);
                for (org.apache.poi.ss.usermodel.Row row : sheet) {
                    org.apache.poi.ss.usermodel.Cell sourceCell = row.getCell(0);
                    org.apache.poi.ss.usermodel.Cell targetCell = row.getCell(1);

                    if (sourceCell != null && targetCell != null) {
                        String source = getCellValueAsString(sourceCell);
                        String target = getCellValueAsString(targetCell);

                        if (source != null && !source.trim().isEmpty()
                                && target != null && !target.trim().isEmpty()) {
                            terms.put(source.trim(), target.trim());
                        }
                    }
                }
            }
            wb.close();

            if (!terms.isEmpty()) {
                int count = mergeGlossary(terms);
                log.info("从 Excel 导入 {} 条术语", count);
                return count;
            }
            return 0;

        } catch (Exception e) {
            log.error("导入 Excel 术语失败: {}", e.getMessage(), e);
            throw new RuntimeException("导入 Excel 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取单元格的字符串值
     */
    private String getCellValueAsString(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    // ==================== 弱边界替换核心算法 ====================

    @Override
    public ReplaceResult replaceTermsMaxCover(String text, Map<String, String> glossary) {
        if (text == null || text.isEmpty()) {
            return new ReplaceResult(text, 0, 0);
        }
        if (glossary == null || glossary.isEmpty()) {
            int totalClean = text.replaceAll("\\s+", "").length();
            return new ReplaceResult(text, 0, totalClean);
        }

        String textHw = TextNormalizer.toHalfwidth(text);
        int totalClean = text.replaceAll("\\s+", "").length();

        // 核心优化：大表 → 候选子集
        List<Map.Entry<String, String>> candidates = getCandidatePairs(glossary, text);

        if (candidates.isEmpty()) {
            return new ReplaceResult(text, 0, totalClean);
        }

        // 构建匹配列表
        List<MatchInfo> matches = new ArrayList<>();
        for (Map.Entry<String, String> entry : candidates) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (CJK_PATTERN.matcher(key).find()) {
                // 含中文术语：按“去空白视图”匹配
                for (int[] range : findCjkWsInsensitiveMatches(text, key)) {
                    matches.add(new MatchInfo(range[0], range[1], range[1] - range[0], key, value));
                }
            } else {
                // 纯英数字术语：简单子串匹配
                for (int[] range : findWeakMatches(textHw, key)) {
                    matches.add(new MatchInfo(range[0], range[1], range[1] - range[0], key, value));
                }
            }
        }

        if (matches.isEmpty()) {
            return new ReplaceResult(text, 0, totalClean);
        }

        // 最长优先 + 起始位置排序
        matches.sort((a, b) -> {
            int cmp = Integer.compare(b.length, a.length);
            if (cmp != 0) return cmp;
            return Integer.compare(a.start, b.start);
        });

        // 非重叠贪心
        List<MatchInfo> chosen = new ArrayList<>();
        boolean[] occupied = new boolean[text.length()];
        for (MatchInfo m : matches) {
            boolean overlap = false;
            for (int i = m.start; i < m.end && i < occupied.length; i++) {
                if (occupied[i]) {
                    overlap = true;
                    break;
                }
            }
            if (!overlap) {
                chosen.add(m);
                for (int i = m.start; i < m.end && i < occupied.length; i++) {
                    occupied[i] = true;
                }
            }
        }

        // 从后往前替换（避免下标偏移）
        StringBuilder replaced = new StringBuilder(text);
        int covered = 0;
        chosen.sort((a, b) -> Integer.compare(b.start, a.start)); // 从后往前

        for (MatchInfo m : chosen) {
            replaced.replace(m.start, m.end, m.target);
            // 计算去空格后的覆盖长度
            String matchedText = text.substring(m.start, m.end);
            covered += matchedText.replaceAll("\\s+", "").length();
        }

        return new ReplaceResult(replaced.toString(), covered, totalClean);
    }

    // ==================== 匹配辅助方法 ====================

    /**
     * 弱边界匹配：简单子串查找
     */
    private List<int[]> findWeakMatches(String text, String key) {
        List<int[]> result = new ArrayList<>();
        if (key == null || key.isEmpty()) {
            return result;
        }

        int start = 0;
        while (true) {
            int pos = text.indexOf(key, start);
            if (pos == -1) break;
            result.add(new int[]{pos, pos + key.length()});
            start = pos + 1;
        }
        return result;
    }

    /**
     * 中文术语去空白视图匹配
     * <p>
     * 对 text 做全角→半角 + 去空白视图，
     * 在去空白视图中用简单子串查找 key，
     * 再通过 mapping 映射回原始 text 的 start/end 下标
     */
    private List<int[]> findCjkWsInsensitiveMatches(String text, String key) {
        List<int[]> result = new ArrayList<>();
        if (key == null || key.isEmpty()) {
            return result;
        }

        // key 再保险去一遍空白
        String keyNorm = TextNormalizer.toHalfwidth(key).replaceAll("\\s+", "");
        if (keyNorm.isEmpty()) {
            return result;
        }

        // 构造去空白视图
        String textHw = TextNormalizer.toHalfwidth(text);
        TextNormalizer.StripResult stripResult = stripWhitespaceWithMapping(textHw);
        String normText = stripResult.getNormText();
        int[] mapping = stripResult.getMapping();

        if (normText.isEmpty() || mapping.length == 0) {
            return result;
        }

        int start = 0;
        int nKey = keyNorm.length();
        while (true) {
            int pos = normText.indexOf(keyNorm, start);
            if (pos == -1) break;

            // 映射回原始 text 的区间
            int origStart = mapping[pos];
            int endPos = pos + nKey - 1;
            int origEnd = (endPos < mapping.length) ? mapping[endPos] + 1 : text.length();
            result.add(new int[]{origStart, origEnd});

            start = pos + 1;
        }
        return result;
    }

    /**
     * 去除文本中的空白字符，并建立位置映射
     */
    private TextNormalizer.StripResult stripWhitespaceWithMapping(String text) {
        if (text == null || text.isEmpty()) {
            return new TextNormalizer.StripResult("", new int[0]);
        }

        int len = text.length();
        List<Integer> mappingList = new ArrayList<>();
        StringBuilder normBuilder = new StringBuilder(len);

        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (!Character.isWhitespace(c)) {
                normBuilder.append(c);
                mappingList.add(i);
            }
        }

        // 构建反向映射：规范化位置 -> 原文位置
        int[] mapping = new int[mappingList.size()];
        for (int i = 0; i < mappingList.size(); i++) {
            mapping[i] = mappingList.get(i);
        }

        return new TextNormalizer.StripResult(normBuilder.toString(), mapping);
    }

    // ==================== 首字符索引优化 ====================

    /**
     * 生成词典签名（用于缓存索引）
     */
    private String glossarySignature(Map<String, String> glossary) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(String.valueOf(glossary.size()).getBytes(StandardCharsets.UTF_8));

            // 取部分键做采样
            int i = 0;
            for (String key : glossary.keySet().stream().sorted().limit(500).collect(Collectors.toList())) {
                if (i % 7 == 0) {
                    md.update(key.getBytes(StandardCharsets.UTF_8));
                }
                i++;
            }
            return bytesToHex(md.digest());
        } catch (Exception e) {
            return String.valueOf(glossary.hashCode());
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 构建首字符索引
     */
    private Map<String, List<Map.Entry<String, String>>> buildLeadingIndex(Map<String, String> glossary) {
        Map<String, List<Map.Entry<String, String>>> index = new HashMap<>();

        for (Map.Entry<String, String> entry : glossary.entrySet()) {
            String key = entry.getKey();
            String bucket = getBucket(key);

            index.computeIfAbsent(bucket, k -> new ArrayList<>())
                    .add(entry);
        }

        // 各桶内按 key 长度降序（最长优先）
        for (List<Map.Entry<String, String>> list : index.values()) {
            list.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
        }

        log.debug("[buildLeadingIndex] 术语库索引构建完成，共 {} 个字符桶", index.size());

        return index;
    }

    /**
     * 获取术语的首字符桶
     * <p>
     * 对于英文术语，统一使用小写首字母作为桶 key，确保大小写不敏感匹配
     */
    private String getBucket(String key) {
        Matcher m = CJK_ALNUM_PATTERN.matcher(key);
        if (m.find()) {
            char firstChar = m.group().charAt(0);
            // 英文字母统一转为小写，中文和数字保持不变
            if (Character.isLetter(firstChar) && firstChar <= 127) {
                return String.valueOf(Character.toLowerCase(firstChar));
            }
            return String.valueOf(firstChar);
        }
        return "#";
    }

    /**
     * 从大词典中按文本涉及到的字符桶筛选候选
     */
    private List<Map.Entry<String, String>> getCandidatePairs(Map<String, String> glossary, String text) {
        if (glossary == null || glossary.isEmpty() || text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        // 每次调用都重新构建索引，确保使用最新的术语库数据
        Map<String, List<Map.Entry<String, String>>> index = buildLeadingIndex(glossary);

        // 提取文本中的相关字符集合（优化：英文提取完整单词的首字母）
        Set<String> chars = new HashSet<>();
        int maxChars = 80;

        // 先检查文本是否包含中文
        boolean hasChinese = CJK_PATTERN.matcher(text).find();

        if (hasChinese) {
            // 中文文本：提取单个中文字符和数字
            Matcher m = CJK_ALNUM_PATTERN.matcher(text);
            while (m.find() && chars.size() < maxChars) {
                chars.add(m.group());
            }
        } else {
            // 英文文本：提取单词首字母（更高效）
            Matcher wordMatcher = WORD_PATTERN.matcher(text);
            while (wordMatcher.find() && chars.size() < maxChars) {
                String word = wordMatcher.group();
                if (!word.isEmpty()) {
                    char firstChar = word.charAt(0);
                    // 添加小写形式
                    chars.add(String.valueOf(Character.toLowerCase(firstChar)));
                    // 如果有空间，也添加大写形式
                    if (chars.size() < maxChars) {
                        chars.add(String.valueOf(Character.toUpperCase(firstChar)));
                    }
                }
            }
        }

        if (chars.isEmpty()) {
            chars.add("#");
        }

        int maxPerBucket = 1200;
        List<Map.Entry<String, String>> candidates = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (String ch : chars) {
            List<Map.Entry<String, String>> list = index.get(ch);
            if (list == null) continue;

            int count = 0;
            for (Map.Entry<String, String> entry : list) {
                if (!seen.contains(entry.getKey())) {
                    candidates.add(entry);
                    seen.add(entry.getKey());
                    count++;
                    if (count >= maxPerBucket) break;
                }
            }
            if (candidates.size() >= maxPerBucket * 5) break;
        }

        return candidates;
    }

    // ==================== 内部类 ====================

    /**
     * 匹配信息
     */
    private static class MatchInfo {
        final int start;
        final int end;
        final int length;
        final String source;
        final String target;

        MatchInfo(int start, int end, int length, String source, String target) {
            this.start = start;
            this.end = end;
            this.length = length;
            this.source = source;
            this.target = target;
        }
    }
}
