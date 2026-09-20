package cn.iocoder.sva.module.system.controller.admin.sync.vo;

import cn.iocoder.sva.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static cn.iocoder.sva.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 岗位信息同步分页 Request VO")
@Data
public class SyncPositionPageReqVO extends PageParam {

    @Schema(description = "唯一ID", example = "22898")
    private String guid;

    @Schema(description = "数据状态：A-新增，U-更新，D-删除", example = "1")
    private String dcInfDtStatus;

    @Schema(description = "数据状态描述")
    private String dcInfDtStatusDescr;

    @Schema(description = "岗位ID")
    private String positionNbr;

    @Schema(description = "生效日期")
    private LocalDate effdt;

    @Schema(description = "生效状态：A-有效，I-无效", example = "1")
    private String effStatus;

    @Schema(description = "生效状态描述")
    private String effStatusDescr;

    @Schema(description = "岗位名称")
    private String descr;

    @Schema(description = "岗位简称")
    private String descrshort;

    @Schema(description = "业务单位ID")
    private String businessUnit;

    @Schema(description = "业务单位")
    private String businessDescr;

    @Schema(description = "管理区域ID")
    private String regRegion;

    @Schema(description = "管理区域")
    private String regRegionDescr;

    @Schema(description = "地点ID")
    private String location;

    @Schema(description = "地点")
    private String dcLocationDescr;

    @Schema(description = "部门ID", example = "4753")
    private String deptid;

    @Schema(description = "部门名称")
    private String dcDeptDescr50;

    @Schema(description = "部门简称")
    private String dcDeptDescrshort;

    @Schema(description = "标准岗位ID")
    private String jobcode;

    @Schema(description = "标准岗位名称")
    private String dcJobcodeDescr;

    @Schema(description = "标准岗位简称")
    private String dcJobcodeDescrs;

    @Schema(description = "序列ID")
    private String dcJobGroup;

    @Schema(description = "序列")
    private String dcJobGroupDescr;

    @Schema(description = "通道ID")
    private String dcJobSequence;

    @Schema(description = "通道")
    private String dcJobSeqDescr;

    @Schema(description = "子序列ID")
    private String dcFirstJobCate;

    @Schema(description = "子序列")
    private String dcJobcateDescr;

    @Schema(description = "职级")
    private String dcJobLevel;

    @Schema(description = "职级描述")
    private String dcJobLevelDescr;

    @Schema(description = "职等")
    private String dcJobGrade;

    @Schema(description = "职等描述")
    private String dcJobGradeDescr;

    @Schema(description = "职层")
    private String dcJobStage;

    @Schema(description = "职层描述")
    private String dcJobStageDescr;

    @Schema(description = "直接汇报岗位ID")
    private String reportsTo;

    @Schema(description = "直接汇报岗位")
    private String reportsToDescr;

    @Schema(description = "岗位英文名称")
    private String dcPositionDescra;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}