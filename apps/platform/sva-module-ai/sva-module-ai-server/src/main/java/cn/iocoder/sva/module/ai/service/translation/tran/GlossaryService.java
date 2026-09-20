package cn.iocoder.sva.module.ai.service.translation.tran;

import cn.iocoder.sva.module.ai.service.translation.tran.model.GlossaryResponse;

import java.util.Map;

/**
 * 术语库管理服务接口
 * <p>
 * 提供术语库的加载、保存、合并、搜索、以及核心的弱边界术语替换功能。
 * <p>
 * 对应 Python 项目的 glossary.py 模块，接口设计保持一致。
 */
public interface GlossaryService {

    /**
     * 加载术语库
     * <p>
     * 从配置的 glossary-path 读取 JSON 文件，key 用 TextNormalizer.normalizeKey() 规范化，
     * value 用 normalizeValue() 规范化。
     *
     * @return 术语库映射（key=原文术语, value=译文术语），文件不存在时返回空 Map
     */
    Map<String, String> loadGlossary();

    /**
     * 保存术语库
     * <p>
     * 将术语库写入 JSON 文件，ensure_ascii=false（保留中文），pretty print（缩进2空格）。
     *
     * @param glossary 要保存的术语库映射
     */
    void saveGlossary(Map<String, String> glossary);

    /**
     * 合并新术语到现有术语库
     * <p>
     * 加载现有术语库 → 合并新术语（忽略空值和超长key > 60字符） → 保存。
     * 已存在的 key 会被新值覆盖。
     *
     * @param newTerms 新增的术语映射
     * @return 本次新增 + 更新的数量
     */
    int mergeGlossary(Map<String, String> newTerms);

    /**
     * 搜索术语库
     * <p>
     * 支持按 query 模糊搜索（大小写不敏感），结果按 key 排序。
     *
     * @param query 搜索关键词，为空时返回全部
     * @return 搜索结果（包含术语条目列表和总数）
     */
    GlossaryResponse searchGlossary(String query);

    /**
     * 从 Excel 文件导入术语
     * <p>
     * 读取 Excel 文件中的 source/target 列，合并到现有术语库。
     *
     * @param excelPath Excel 文件路径
     * @return 导入的术语数量
     */
    int importFromExcel(String excelPath);

    /**
     * 弱边界术语替换（核心算法）
     * <p>
     * 在文本中查找并替换术语库中的术语，采用"最长优先 + 非重叠贪心"策略。
     * <p>
     * 对中文术语使用"去空白视图"匹配（忽略空白差异），
     * 对英文术语使用简单子串匹配。
     * <p>
     * 返回的 ReplaceResult 包含：替换后的文本、覆盖长度、总有效长度。
     *
     * @param text     待替换的文本
     * @param glossary 术语库映射
     * @return 替换结果（text=替换后文本, covered=覆盖长度, totalClean=总有效长度）
     */
    ReplaceResult replaceTermsMaxCover(String text, Map<String, String> glossary);

    /**
     * 术语替换结果
     * <p>
     * 包含替换后的文本和统计信息，用于翻译质检（QC）覆盖率计算。
     */
    class ReplaceResult {
        /** 替换后的文本 */
        private final String text;
        /** 被术语覆盖的字符长度（原文中的字符数） */
        private final int covered;
        /** 文本的总有效长度（去空白后的字符数），用于计算覆盖率 */
        private final int totalClean;

        public ReplaceResult(String text, int covered, int totalClean) {
            this.text = text;
            this.covered = covered;
            this.totalClean = totalClean;
        }

        public String getText() {
            return text;
        }

        public int getCovered() {
            return covered;
        }

        public int getTotalClean() {
            return totalClean;
        }
    }
}
