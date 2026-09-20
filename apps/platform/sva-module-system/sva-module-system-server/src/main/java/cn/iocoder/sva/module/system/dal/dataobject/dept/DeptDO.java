package cn.iocoder.sva.module.system.dal.dataobject.dept;

import cn.iocoder.sva.framework.common.enums.CommonStatusEnum;
import cn.iocoder.sva.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.sva.module.system.dal.dataobject.user.AdminUserDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 部门表
 *
 * @author like
 */
@TableName("system_dept")
@KeySequence("system_dept_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
public class DeptDO extends TenantBaseDO {

    public static final Long PARENT_ID_ROOT = 0L;

    /**
     * 部门ID
     */
    @TableId
    private Long id;
    /**
     * 部门名称
     */
    private String name;
    /**
     * 父部门ID
     *
     * 关联 {@link #id}
     */
    private Long parentId;
    /**
     * 显示顺序
     */
    private Integer sort;
    /**
     * 负责人
     *
     * 关联 {@link AdminUserDO#getId()}
     */
    private Long leaderUserId;
    /**
     * 联系电话
     */
    private String phone;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 部门状态
     *
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;

    // ========== 新增字段 ==========

    /**
     * 唯一ID
     */
    private String guid;
    /**
     * 生效日期
     */
    private LocalDate effdt;
    /**
     * 生效状态
     */
    private String effStatus;
    /**
     * 部门简称
     */
    private String descrshort;
    /**
     * 所属公司ID
     */
    private String company;
    /**
     * 所属公司名称
     */
    private String dcCompanyDescr;
    /**
     * 组织类型
     */
    private String dcOrgType;
    /**
     * 组织类型名称
     */
    private String dcOrgTypeDescr;
    /**
     * 组织类别
     */
    private String dcOrgLevel;
    /**
     * 组织类别名称
     */
    private String dcOrgLevelDescr;
    /**
     * 上级部门名称
     */
    private String dcParDeptDescr;
    /**
     * 地点ID
     */
    private String location;
    /**
     * 地点名称
     */
    private String dcLocationDescr;
    /**
     * 部门职责
     */
    private String dcDeptRespon;
    /**
     * 部门全路径
     */
    private String dcDeptFullCode;
    /**
     * 部门全路径名称
     */
    private String dcDeptFullDescr;

}