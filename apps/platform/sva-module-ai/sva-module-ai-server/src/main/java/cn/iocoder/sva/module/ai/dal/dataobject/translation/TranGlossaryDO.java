package cn.iocoder.sva.module.ai.dal.dataobject.translation;

import cn.iocoder.sva.framework.mybatis.core.dataobject.BaseCommonDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 术语库管理 DO
 *
 * @author like
 */
@TableName("ai_tran_glossary")
@KeySequence("ai_tran_glossary_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranGlossaryDO extends BaseCommonDO {

    /**
     * 主键id
     */
    @TableId
    private Long id;
    /**
     * 术语库名称
     */
    private String glossaryName;
    /**
     * 源语言
     */
    private String sourceLanguage;
    /**
     * 目标语言
     */
    private String targetLanguage;
    /**
     * 语言方向
     */
    private String languageDirection;
    /**
     * 条目数量
     */
    private Integer itemCount;
    /**
     * 所属角色
     */
    private Long roleId;
    /**
     * 所属人员
     */
    private String username;
    /**
     * 所属部门
     */
    private Long deptId;
    /**
     * 是否启用 1=启用 0=禁用
     */
    private Integer isEnabled;


    private Long roleShow;


}