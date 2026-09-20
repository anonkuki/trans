package cn.iocoder.sva.module.system.dal.dataobject.sync;

import lombok.*;

import java.time.LocalDate;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.sva.framework.mybatis.core.dataobject.BaseDO;

/**
 * 人员信息同步 DO
 *
 * @author 李可
 */
@TableName("system_sync_person")
@KeySequence("system_sync_person_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncPersonDO {

    /**
     * 自增主键
     */
    @TableId
    private Long id;
    /**
     * 唯一ID
     */
    private String guid;
    /**
     * 数据状态：A-新增，U-更新，D-删除
     */
    private String dcInfDtStatus;
    /**
     * 数据状态描述
     */
    private String dcInfDtStatusDescr;
    /**
     * 员工ID
     */
    private String emplid;
    /**
     * 员工记录号
     */
    private String emplRcd;
    /**
     * 生效日期
     */
    private LocalDate effdt;
    /**
     * 生效序号
     */
    private String effseq;
    /**
     * 员工类型ID
     */
    private String emplClass;
    /**
     * 员工类型
     */
    private String dcEmplClsDescr;
    /**
     * 人事操作ID
     */
    private String action;
    /**
     * 人事操作
     */
    private String actionDescr;
    /**
     * 操作原因ID
     */
    private String actionReason;
    /**
     * 操作原因
     */
    private String actionReasnDescr;
    /**
     * HR状态：A-在职，I-离职
     */
    private String hrStatus;
    /**
     * HR状态描述
     */
    private String hrStatusDescr;
    /**
     * 正式/临时：R-正式，T-临时
     */
    private String regTemp;
    /**
     * 正式/临时描述
     */
    private String regTempDescr;
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
     * 管理区域ID
     */
    private String regRegion;
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
    /**
     * 部门ID
     */
    private String deptid;
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
     * 部门副总经理岗位ID
     */
    private String dcManagerPosn;
    /**
     * 成本中心ID
     */
    private String dcCostCenter;
    /**
     * 转正日期
     */
    private LocalDate probationDt;
    /**
     * 工作城市ID
     */
    private String location;
    /**
     * 工作城市
     */
    private String dcLocationDescr;
    /**
     * 是否博士后：Y-是，N-否
     */
    private String dcPdhFellowYn;
    /**
     * 是否博士后描述
     */
    private String dcPdhFellowYnDescr;
    /**
     * 是否残疾人挂靠：Y-是，N-否
     */
    private String dcDisabledYn;
    /**
     * 是否残疾人挂靠描述
     */
    private String dcDisabledYnDescr;
    /**
     * 入职时间
     */
    private LocalDate lastHireDt;
    /**
     * 实习生入职时间
     */
    private LocalDate dcInternHireDt;
    /**
     * 标准岗位ID
     */
    private String jobcode;
    /**
     * 标准岗位名称
     */
    private String dcJobcodeDescr;
    /**
     * 岗位开始日期
     */
    private LocalDate positionEntryDt;
    /**
     * 序列ID
     */
    private String dcJobGroup;
    /**
     * 序列
     */
    private String dcJobGroupDescr;
    /**
     * 通道ID
     */
    private String dcJobSequence;
    /**
     * 通道
     */
    private String dcJobSeqDescr;
    /**
     * 子序列ID
     */
    private String dcFirstJobCate;
    /**
     * 子序列
     */
    private String dcJobcateDescr;
    /**
     * 职层
     */
    private String dcJobStage;
    /**
     * 职层描述
     */
    private String dcJobStageDescr;
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
     * sap公司编码
     */
    private String dcSapCompanyid;
    /**
     * 姓名
     */
    private String nameDisplay;
    /**
     * 公司邮箱
     */
    private String emailAddr;


}