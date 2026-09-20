package cn.iocoder.sva.module.system.controller.admin.role.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 部门和角色关联新增/修改 Request VO")
@Data
public class DeptRoleSaveReqVO {

    @Schema(description = "自增编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17452")
    private Long id;

    @Schema(description = "部门ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "20893")
    @NotNull(message = "部门ID不能为空")
    private Long deptId;

    @Schema(description = "角色ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "32602")
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

}