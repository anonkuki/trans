package cn.iocoder.sva.module.system.controller.admin.temp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 模板管理新增/修改 Request VO")
@Data
public class TempFileSaveReqVO {

    @Schema(description = "文件编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "16179")
    private Long id;

    @Schema(description = "模板标识", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "模板标识不能为空")
    private String tempName;

    @Schema(description = "文件名")
    private String name;

    @Schema(description = "文件 URL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "文件 URL不能为空")
    private String url;

    @Schema(description = "文件类型", example = "2")
    private String type;

    @Schema(description = "文件大小")
    private Integer size;

}