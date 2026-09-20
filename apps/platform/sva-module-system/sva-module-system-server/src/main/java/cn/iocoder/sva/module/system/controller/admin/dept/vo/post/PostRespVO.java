package cn.iocoder.sva.module.system.controller.admin.dept.vo.post;

import cn.iocoder.sva.framework.excel.core.annotations.DictFormat;
import cn.iocoder.sva.framework.excel.core.convert.DictConvert;
import cn.iocoder.sva.module.system.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 岗位信息 Response VO")
@Data
@ExcelIgnoreUnannotated
public class PostRespVO {

    @Schema(description = "岗位序号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("岗位序号")
    private Long id;

    @Schema(description = "岗位名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "小土豆")
    @ExcelProperty("岗位名称")
    private String name;

    @Schema(description = "岗位编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "sva")
    @ExcelProperty("岗位编码")
    private String code;

    @Schema(description = "显示顺序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("岗位排序")
    private Integer sort;

    @Schema(description = "状态，参见 CommonStatusEnum 枚举类", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.COMMON_STATUS)
    private Integer status;

    @Schema(description = "备注", example = "快乐的备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    // ========== 新增字段 ==========

    @Schema(description = "唯一ID", example = "550e8400-e29b-41d4-a716-446655440000")
    @ExcelProperty("唯一ID")
    private String guid;

    @Schema(description = "岗位简称", example = "研发组长")
    @ExcelProperty("岗位简称")
    private String descrshort;

    @Schema(description = "职级", example = "P7")
    @ExcelProperty("职级")
    private String dcJobLevel;

    @Schema(description = "职级描述", example = "高级工程师")
    @ExcelProperty("职级描述")
    private String dcJobLevelDescr;

    @Schema(description = "职等", example = "3")
    @ExcelProperty("职等")
    private String dcJobGrade;

    @Schema(description = "职等描述", example = "中级")
    @ExcelProperty("职等描述")
    private String dcJobGradeDescr;

    @Schema(description = "职层", example = "M")
    @ExcelProperty("职层")
    private String dcJobStage;

    @Schema(description = "职层描述", example = "管理层")
    @ExcelProperty("职层描述")
    private String dcJobStageDescr;

    @Schema(description = "直接汇报岗位ID", example = "10001")
    @ExcelProperty("直接汇报岗位ID")
    private String reportsTo;

    @Schema(description = "直接汇报岗位", example = "技术总监")
    @ExcelProperty("直接汇报岗位")
    private String reportsToDescr;

    @Schema(description = "岗位英文名称", example = "Software Engineer")
    @ExcelProperty("岗位英文名称")
    private String dcPositionDescra;

    @Schema(description = "部门ID", example = "100")
    @ExcelProperty("部门ID")
    private Long deptid;

    @Schema(description = "部门名称", example = "技术研发部")
    @ExcelProperty("部门名称")
    private String dcDeptDescr50;

    @Schema(description = "业务单位ID", example = "BU001")
    @ExcelProperty("业务单位ID")
    private String businessUnit;

    @Schema(description = "业务单位", example = "中国区业务部")
    @ExcelProperty("业务单位")
    private String businessDescr;
}