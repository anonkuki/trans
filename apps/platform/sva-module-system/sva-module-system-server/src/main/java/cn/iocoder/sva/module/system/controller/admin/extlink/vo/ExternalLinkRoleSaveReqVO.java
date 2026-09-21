package cn.iocoder.sva.module.system.controller.admin.extlink.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 外链和角色关联新增/修改 Request VO")
@Data
public class ExternalLinkRoleSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "6589")
    private Long id;

    @Schema(description = "外链编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13077")
    @NotNull(message = "外链编号不能为空")
    private Long linkId;

    @Schema(description = "角色编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "18559")
    @NotNull(message = "角色编号不能为空")
    private Long roleId;

}