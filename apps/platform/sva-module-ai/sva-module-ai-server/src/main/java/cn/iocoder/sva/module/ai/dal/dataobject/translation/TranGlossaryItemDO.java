package cn.iocoder.sva.module.ai.dal.dataobject.translation;

import cn.iocoder.sva.framework.mybatis.core.dataobject.BaseCommonDO;
import cn.iocoder.sva.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 术语管理 DO
 *
 * @author like
 */
@TableName("ai_tran_glossary_item")
@KeySequence("ai_tran_glossary_item_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranGlossaryItemDO extends BaseCommonDO {

    /**
     * 主键id
     */
    @TableId
    private Long id;
    /**
     * 源语言
     */
    private String sourceLanguage;
    /**
     * 目标语言
     */
    private String targetLanguage;
    /**
     * 所属术语库id
     */
    private Long glossaryId;


}