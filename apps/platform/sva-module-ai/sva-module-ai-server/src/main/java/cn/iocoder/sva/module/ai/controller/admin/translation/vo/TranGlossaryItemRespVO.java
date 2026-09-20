package cn.iocoder.sva.module.ai.controller.admin.translation.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 术语管理 Response VO")
@Data
@ExcelIgnoreUnannotated
public class TranGlossaryItemRespVO {

    @Schema(description = "主键id", requiredMode = Schema.RequiredMode.REQUIRED, example = "30036")
    @ExcelProperty("主键id")
    private Long id;

    @Schema(description = "源语言", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("源语言")
    private String sourceLanguage;

    @Schema(description = "目标语言", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("目标语言")
    private String targetLanguage;

    @Schema(description = "所属术语库id", requiredMode = Schema.RequiredMode.REQUIRED, example = "31003")
    @ExcelProperty("所属术语库id")
    private Long glossaryId;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}