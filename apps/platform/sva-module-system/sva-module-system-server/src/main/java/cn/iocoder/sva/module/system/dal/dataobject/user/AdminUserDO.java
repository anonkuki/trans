package cn.iocoder.sva.module.system.dal.dataobject.user;

import cn.iocoder.sva.framework.common.enums.CommonStatusEnum;
import cn.iocoder.sva.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.sva.module.system.enums.common.SexEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Set;

/**
 * 管理后台的用户 DO
 *
 * @author 科兴源码
 */
@TableName(value = "system_users", autoResultMap = true) // 由于 SQL Server 的 system_user 是关键字，所以使用 system_users
@KeySequence("system_users_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDO extends TenantBaseDO {

    /**
     * 用户ID
     */
    @TableId
    private Long id;
    /**
     * 用户账号
     */
    private String username;
    /**
     * 加密后的密码
     *
     * 因为目前使用 {@link BCryptPasswordEncoder} 加密器，所以无需自己处理 salt 盐
     */
    private String password;
    /**
     * 用户昵称
     */
    private String nickname;
    /**
     * 备注
     */
    private String remark;
    /**
     * 部门 ID
     */
    private Long deptId;
    /**
     * 岗位编号数组
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Set<Long> postIds;
    /**
     * 用户邮箱
     */
    private String email;
    /**
     * 手机号码
     */
    private String mobile;
    /**
     * 用户性别
     *
     * 枚举类 {@link SexEnum}
     */
    private Integer sex;
    /**
     * 用户头像
     */
    private String avatar;
    /**
     * 帐号状态
     *
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;
    /**
     * 最后登录IP
     */
    private String loginIp;
    /**
     * 最后登录时间
     */
    private LocalDateTime loginDate;

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
     * 员工类型ID
     */
    private String emplClass;

    /**
     * 员工类型
     */
    private String dcEmplClsDescr;

    /**
     * HR状态
     */
    private String hrStatus;

    /**
     * 正式/临时
     */
    private String regTemp;

    /**
     * 直接上级岗位ID
     */
    private String reportsTo;

    /**
     * 主/兼岗
     */
    private String jobIndicator;

    /**
     * 主/兼岗描述
     */
    private String jobIndicatorDescr;

    /**
     * 岗位ID
     */
    private String positionNbr;

    /**
     * 岗位
     */
    private String dcPositionDescr;

    /**
     * 部门
     */
    private String dcDeptDescr50;

    /**
     * 部门负责人岗位ID
     */
    private String managerPosn;

    /**
     * 部门总监岗位ID
     */
    private String dcDirectorPosn;

    /**
     * 转正日期
     */
    private LocalDate probationDt;

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
     * 入职时间
     */
    private LocalDate lastHireDt;

    /**
     * 公司ID
     */
    private String company;

    /**
     * 公司
     */
    private String dcCompanyDescr;

    /**
     * 业务单位ID
     */
    private String businessUnit;

    /**
     * 业务单位
     */
    private String businessDescr;
}