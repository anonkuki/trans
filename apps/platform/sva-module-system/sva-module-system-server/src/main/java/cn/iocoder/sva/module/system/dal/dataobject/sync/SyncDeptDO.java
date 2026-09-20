package cn.iocoder.sva.module.system.dal.dataobject.sync;

import cn.iocoder.sva.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

/**
 * 部门信息同步 DO
 *
 * @author 李可
 */
@TableName("system_sync_dept")
@KeySequence("system_sync_dept_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncDeptDO {

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
     * 集合ID
     */
    private String setid;
    /**
     * 部门ID
     */
    private String deptid;
    /**
     * 生效日期
     */
    private LocalDate effdt;
    /**
     * 生效状态：A-有效，I-无效
     */
    private String effStatus;
    /**
     * 生效状态描述
     */
    private String effStatusDescr;
    /**
     * 部门名称
     */
    private String descr;
    /**
     * 部门简称
     */
    private String descrshort;
    /**
     * 地点集合ID
     */
    private String setidLocation;
    /**
     * 地点ID
     */
    private String location;
    /**
     * 地点
     */
    private String dcLocationDescr;
    /**
     * 所属公司ID
     */
    private String company;
    /**
     * 所属公司名称
     */
    private String dcCompanyDescr;
    /**
     * 组织类型：10-公司,20-体系，30-部门，40-项目，50-班组
     */
    private String dcOrgType;
    /**
     * 组织类型描述
     */
    private String dcOrgTypeDescr;
    /**
     * 组织类别：10-一级组织，20-二级组织，30-三级组织
     */
    private String dcOrgLevel;
    /**
     * 组织类别描述
     */
    private String dcOrgLevelDescr;
    /**
     * 上级部门ID
     */
    private String partDeptidChn;
    /**
     * 上级部门
     */
    private String dcParDeptDescr;
    /**
     * 组织负责人岗位ID
     */
    private String managerPosn;
    /**
     * 部门分管总监岗位ID
     */
    private String dcDirectorPosn;
    /**
     * 部门分管副总经理岗位ID
     */
    private String dcManagerPosn;
    /**
     * 成立日期
     */
    private LocalDate dcSetupDate;
    /**
     * 成立文号
     */
    private String dcSetupNum;
    /**
     * 成本中心ID
     */
    private String dcCostCenter;
    /**
     * 体系ID
     */
    private String dcOrgBranch;
    /**
     * 体系
     */
    private String dcOrgBranchDescr;
    /**
     * 成立原因
     */
    private String dcSetupReason;
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
    /**
     * 红海转换ID
     */
    private String dcHonghaiDeptid;
    /**
     * SAP公司代码
     */
    private String dcSapCompanyid;
    /**
     * 一级部门
     */
    private String dcDeptidLv01;
    /**
     * 二级部门
     */
    private String dcDeptidLv02;
    /**
     * 三级部门
     */
    private String dcDeptidLv03;


}