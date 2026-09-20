package cn.iocoder.sva.module.ai.controller.admin.translation.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 术语库管理 Response VO")
@Data
@ExcelIgnoreUnannotated
public class TranGlossaryRespVO {

    @Schema(description = "主键id", requiredMode = Schema.RequiredMode.REQUIRED, example = "10083")
    @ExcelProperty("主键id")
    private Long id;

    @Schema(description = "术语库名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @ExcelProperty("术语库名称")
    private String glossaryName;

    @Schema(description = "源语言", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("源语言")
    private String sourceLanguage;

    @Schema(description = "目标语言", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("目标语言")
    private String targetLanguage;

    @Schema(description = "语言方向", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("语言方向")
    private String languageDirection;

    @Schema(description = "条目数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "23091")
    @ExcelProperty("条目数量")
    private Integer itemCount;

    @Schema(description = "所属角色", example = "2273")
    @ExcelProperty("所属角色")
    private Long roleId;

    @Schema(description = "所属人员", example = "")
    @ExcelProperty("所属人员")
    private String username;

    @Schema(description = "所属部门", example = "13831")
    @ExcelProperty("所属部门")
    private Long deptId;

    @Schema(description = "是否启用 1=启用 0=禁用", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("是否启用 1=启用 0=禁用")
    private Integer isEnabled;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "可见角色", example = "8600")
    @ExcelProperty("可见角色")
    private Integer roleShow;

}