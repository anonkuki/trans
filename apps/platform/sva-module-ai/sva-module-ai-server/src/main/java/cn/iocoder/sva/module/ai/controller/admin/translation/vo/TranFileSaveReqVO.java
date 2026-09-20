package cn.iocoder.sva.module.ai.controller.admin.translation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AI翻译文件信息新增/修改 Request VO")
@Data
public class TranFileSaveReqVO {

    @Schema(description = "主键ID，自增", requiredMode = Schema.RequiredMode.REQUIRED, example = "15800")
    private Long id;

    @Schema(description = "文件名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "文件名不能为空")
    private String fileName;

    @Schema(description = "文件路径", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "文件路径不能为空")
    private String fileUrl;

    @Schema(description = "文件状态：0-未翻译 1-翻译完成 2-异常文件", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "文件状态：0-未翻译 1-翻译完成 2-异常文件不能为空")
    private Integer fileStatus;

    @Schema(description = "文件类型", example = "1")
    private String fileType;

    @Schema(description = "文件说明")
    private String context;

}