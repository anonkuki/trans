package cn.iocoder.sva.module.system.controller.admin.role.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 部门和角色关联 Response VO")
@Data
@ExcelIgnoreUnannotated
public class DeptRoleRespVO {

    @Schema(description = "自增编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17452")
    @ExcelProperty("自增编号")
    private Long id;

    @Schema(description = "部门ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "20893")
    @ExcelProperty("部门ID")
    private Long deptId;

    @Schema(description = "角色ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "32602")
    @ExcelProperty("角色ID")
    private Long roleId;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}