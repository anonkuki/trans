package cn.iocoder.sva.module.system.dal.dataobject.sync;

import lombok.*;

import java.time.LocalDate;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.sva.framework.mybatis.core.dataobject.BaseDO;

/**
 * 岗位信息同步 DO
 *
 * @author 李可
 */
@TableName("system_sync_position")
@KeySequence("system_sync_position_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncPositionDO {

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
     * 岗位ID
     */
    private String positionNbr;
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
     * 岗位名称
     */
    private String descr;
    /**
     * 岗位简称
     */
    private String descrshort;
    /**
     * 业务单位ID
     */
    private String businessUnit;
    /**
     * 业务单位
     */
    private String businessDescr;
    /**
     * 管理区域ID
     */
    private String regRegion;
    /**
     * 管理区域
     */
    private String regRegionDescr;
    /**
     * 地点ID
     */
    private String location;
    /**
     * 地点
     */
    private String dcLocationDescr;
    /**
     * 部门ID
     */
    private String deptid;
    /**
     * 部门名称
     */
    private String dcDeptDescr50;
    /**
     * 部门简称
     */
    private String dcDeptDescrshort;
    /**
     * 标准岗位ID
     */
    private String jobcode;
    /**
     * 标准岗位名称
     */
    private String dcJobcodeDescr;
    /**
     * 标准岗位简称
     */
    private String dcJobcodeDescrs;
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


}