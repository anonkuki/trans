package cn.iocoder.sva.module.ai.dal.dataobject.translation;

import cn.iocoder.sva.framework.mybatis.core.dataobject.BaseCommonDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * AI词句翻译历史 DO
 * <p>
 * 记录“词句翻译”页面每一次翻译的原文、译文及所选配置（模型、目标语言、术语、场景），
 * 每个用户仅保留最近 1000 条，支持按原文/译文模糊搜索。
 *
 * @author like
 */
@TableName("ai_tran_text_history")
@KeySequence("ai_tran_text_history_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranTextHistoryDO extends BaseCommonDO {

    /**
     * 主键ID，自增
     */
    @TableId
    private Long id;

    /**
     * 原文
     */
    private String sourceText;

    /**
     * 译文
     */
    private String targetText;

    /**
     * 目标语言（English/Chinese）
     */
    private String targetLang;

    /**
     * 模型ID
     */
    private Long modelId;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 场景（聊天角色）ID
     */
    private Long roleId;

    /**
     * 场景名称
     */
    private String roleName;

    /**
     * 术语库ID列表（逗号分隔）
     */
    private String glossaryIds;

    /**
     * 用户名（工号）
     */
    private String username;

}
