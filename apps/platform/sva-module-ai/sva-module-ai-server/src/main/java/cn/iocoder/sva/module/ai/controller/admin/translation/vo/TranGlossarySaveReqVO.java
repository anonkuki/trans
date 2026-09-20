package cn.iocoder.sva.module.ai.controller.admin.translation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 术语库管理新增/修改 Request VO")
@Data
public class TranGlossarySaveReqVO {

    @Schema(description = "主键id", requiredMode = Schema.RequiredMode.REQUIRED, example = "10083")
    private Long id;

    @Schema(description = "术语库名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotEmpty(message = "术语库名称不能为空")
    private String glossaryName;

    @Schema(description = "源语言", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "源语言不能为空")
    private String sourceLanguage;

    @Schema(description = "目标语言", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "目标语言不能为空")
    private String targetLanguage;

    @Schema(description = "语言方向", requiredMode = Schema.RequiredMode.REQUIRED)
//    @NotEmpty(message = "语言方向不能为空")
    private String languageDirection;

    @Schema(description = "条目数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "23091")
//    @NotNull(message = "条目数量不能为空")
    private Integer itemCount;

    @Schema(description = "所属角色", example = "2273")
    private Long roleId;

    @Schema(description = "所属人员", example = "8600")
    private String username;

    @Schema(description = "所属部门", example = "13831")
    private Long deptId;

    @Schema(description = "是否启用 1=启用 0=禁用", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "是否启用 1=启用 0=禁用不能为空")
    private Integer isEnabled;

    @Schema(description = "可见角色", example = "2273")
    private Long roleShow;

}