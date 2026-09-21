package cn.iocoder.sva.module.system.controller.admin.extlink.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 外链和角色关联 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ExternalLinkRoleRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "6589")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "外链编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13077")
    @ExcelProperty("外链编号")
    private Long linkId;

    @Schema(description = "角色编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "18559")
    @ExcelProperty("角色编号")
    private Long roleId;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}