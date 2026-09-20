package cn.iocoder.sva.module.system.controller.admin.sync.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 岗位信息同步 Response VO")
@Data
@ExcelIgnoreUnannotated
public class SyncPositionRespVO {

    @Schema(description = "自增主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "24828")
    @ExcelProperty("自增主键")
    private Long id;

    @Schema(description = "唯一ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "22898")
    @ExcelProperty("唯一ID")
    private String guid;

    @Schema(description = "数据状态：A-新增，U-更新，D-删除", example = "1")
    @ExcelProperty("数据状态：A-新增，U-更新，D-删除")
    private String dcInfDtStatus;

    @Schema(description = "数据状态描述")
    @ExcelProperty("数据状态描述")
    private String dcInfDtStatusDescr;

    @Schema(description = "岗位ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("岗位ID")
    private String positionNbr;

    @Schema(description = "生效日期")
    @ExcelProperty("生效日期")
    private LocalDate effdt;

    @Schema(description = "生效状态：A-有效，I-无效", example = "1")
    @ExcelProperty("生效状态：A-有效，I-无效")
    private String effStatus;

    @Schema(description = "生效状态描述")
    @ExcelProperty("生效状态描述")
    private String effStatusDescr;

    @Schema(description = "岗位名称")
    @ExcelProperty("岗位名称")
    private String descr;

    @Schema(description = "岗位简称")
    @ExcelProperty("岗位简称")
    private String descrshort;

    @Schema(description = "业务单位ID")
    @ExcelProperty("业务单位ID")
    private String businessUnit;

    @Schema(description = "业务单位")
    @ExcelProperty("业务单位")
    private String businessDescr;

    @Schema(description = "管理区域ID")
    @ExcelProperty("管理区域ID")
    private String regRegion;

    @Schema(description = "管理区域")
    @ExcelProperty("管理区域")
    private String regRegionDescr;

    @Schema(description = "地点ID")
    @ExcelProperty("地点ID")
    private String location;

    @Schema(description = "地点")
    @ExcelProperty("地点")
    private String dcLocationDescr;

    @Schema(description = "部门ID", example = "4753")
    @ExcelProperty("部门ID")
    private String deptid;

    @Schema(description = "部门名称")
    @ExcelProperty("部门名称")
    private String dcDeptDescr50;

    @Schema(description = "部门简称")
    @ExcelProperty("部门简称")
    private String dcDeptDescrshort;

    @Schema(description = "标准岗位ID")
    @ExcelProperty("标准岗位ID")
    private String jobcode;

    @Schema(description = "标准岗位名称")
    @ExcelProperty("标准岗位名称")
    private String dcJobcodeDescr;

    @Schema(description = "标准岗位简称")
    @ExcelProperty("标准岗位简称")
    private String dcJobcodeDescrs;

    @Schema(description = "序列ID")
    @ExcelProperty("序列ID")
    private String dcJobGroup;

    @Schema(description = "序列")
    @ExcelProperty("序列")
    private String dcJobGroupDescr;

    @Schema(description = "通道ID")
    @ExcelProperty("通道ID")
    private String dcJobSequence;

    @Schema(description = "通道")
    @ExcelProperty("通道")
    private String dcJobSeqDescr;

    @Schema(description = "子序列ID")
    @ExcelProperty("子序列ID")
    private String dcFirstJobCate;

    @Schema(description = "子序列")
    @ExcelProperty("子序列")
    private String dcJobcateDescr;

    @Schema(description = "职级")
    @ExcelProperty("职级")
    private String dcJobLevel;

    @Schema(description = "职级描述")
    @ExcelProperty("职级描述")
    private String dcJobLevelDescr;

    @Schema(description = "职等")
    @ExcelProperty("职等")
    private String dcJobGrade;

    @Schema(description = "职等描述")
    @ExcelProperty("职等描述")
    private String dcJobGradeDescr;

    @Schema(description = "职层")
    @ExcelProperty("职层")
    private String dcJobStage;

    @Schema(description = "职层描述")
    @ExcelProperty("职层描述")
    private String dcJobStageDescr;

    @Schema(description = "直接汇报岗位ID")
    @ExcelProperty("直接汇报岗位ID")
    private String reportsTo;

    @Schema(description = "直接汇报岗位")
    @ExcelProperty("直接汇报岗位")
    private String reportsToDescr;

    @Schema(description = "岗位英文名称")
    @ExcelProperty("岗位英文名称")
    private String dcPositionDescra;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}