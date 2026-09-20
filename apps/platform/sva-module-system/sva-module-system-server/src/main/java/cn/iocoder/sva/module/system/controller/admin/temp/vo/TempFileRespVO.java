package cn.iocoder.sva.module.system.controller.admin.temp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 模板管理 Response VO")
@Data
@ExcelIgnoreUnannotated
public class TempFileRespVO {

    @Schema(description = "文件编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "16179")
    @ExcelProperty("文件编号")
    private Long id;

    @Schema(description = "模板标识", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("模板标识")
    private String tempName;

    @Schema(description = "文件名")
    @ExcelProperty("文件名")
    private String name;

    @Schema(description = "文件 URL", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("文件 URL")
    private String url;

    @Schema(description = "文件类型", example = "2")
    @ExcelProperty("文件类型")
    private String type;

    @Schema(description = "文件大小")
    @ExcelProperty("文件大小")
    private Integer size;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}