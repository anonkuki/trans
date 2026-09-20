package cn.iocoder.sva.module.system.dal.dataobject.sync;

import cn.iocoder.sva.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

/**
 * 全部人员信息同步 DO
 *
 * @author like
 */
@TableName("system_sync_staff")
@KeySequence("system_sync_staff_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncStaffDO {

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
     * 姓名
     */
    private String nameDisplay;
    /**
     * 性别：F-女，M-男
     */
    private String sex;
    /**
     * 性别描述
     */
    private String sexDescr;
    /**
     * 公司邮箱
     */
    private String emailAddr;
    /**
     * 手机
     */
    private String phone;
    /**
     * 员工类型ID：40-专家顾问,60-劳务外包,50-劳务派遣,20-实习生,10-正式员工,30-退休返聘
     */
    private String emplClass;
    /**
     * 员工类型
     */
    private String dcEmplClsDescr;
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
     * 员工层级
     */
    private String dcEmpLevel;
    /**
     * 员工职层：A-高层，B-中层，C-员工
     */
    private String dcEmplStage;
    /**
     * 员工职层描述
     */
    private String dcEmplStageDescr;
    /**
     * 证件类型：NID-身份证, HKMID-港澳居民来往内地通行证, TWMID-台胞证, PASS-护照
     */
    private String nationalIdType;
    /**
     * 证件号
     */
    private String nationalId;
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
     * 名
     */
    private String firstName;
    /**
     * 姓
     */
    private String lastName;


}