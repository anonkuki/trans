package cn.iocoder.sva.module.ai.controller.admin.translation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 术语管理新增/修改 Request VO")
@Data
public class TranGlossaryItemSaveReqVO {

    @Schema(description = "主键id", requiredMode = Schema.RequiredMode.REQUIRED, example = "30036")
    private Long id;

    @Schema(description = "源语言", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "源语言不能为空")
    private String sourceLanguage;

    @Schema(description = "目标语言", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "目标语言不能为空")
    private String targetLanguage;

    @Schema(description = "所属术语库id", requiredMode = Schema.RequiredMode.REQUIRED, example = "31003")
    @NotNull(message = "所属术语库id不能为空")
    private Long glossaryId;

}