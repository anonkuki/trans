package cn.iocoder.sva.module.system.dal.dataobject.dept;

import cn.iocoder.sva.framework.common.enums.CommonStatusEnum;
import cn.iocoder.sva.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 岗位表
 *
 * @author ruoyi
 */
@TableName("system_post")
@KeySequence("system_post_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
public class PostDO extends BaseDO {

    /**
     * 岗位序号
     */
    @TableId
    private Long id;
    /**
     * 岗位名称
     */
    private String name;
    /**
     * 岗位编码
     */
    private String code;
    /**
     * 岗位排序
     */
    private Integer sort;
    /**
     * 状态
     *
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

    // ========== 新增字段 ==========

    /**
     * 唯一ID
     */
    private String guid;

    /**
     * 岗位简称
     */
    private String descrshort;

    /**
     * 职级
     */
    private String dcJobLevel;

    /**
     * 职级描述
     */
    private String dcJobLevelDescr;

    /**
     * 职等
     */
    private String dcJobGrade;

    /**
     * 职等描述
     */
    private String dcJobGradeDescr;

    /**
     * 职层
     */
    private String dcJobStage;

    /**
     * 职层描述
     */
    private String dcJobStageDescr;

    /**
     * 直接汇报岗位ID
     */
    private String reportsTo;

    /**
     * 直接汇报岗位
     */
    private String reportsToDescr;

    /**
     * 岗位英文名称
     */
    private String dcPositionDescra;

    /**
     * 部门ID
     */
    private Long deptid;

    /**
     * 部门名称
     */
    private String dcDeptDescr50;

    /**
     * 业务单位ID
     */
    private String businessUnit;

    /**
     * 业务单位
     */
    private String businessDescr;
}