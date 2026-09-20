package cn.iocoder.sva.module.ai.controller.admin.translation.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - AI翻译文件信息 Response VO")
@Data
@ExcelIgnoreUnannotated
public class TranFileRespVO {

    @Schema(description = "主键ID，自增", requiredMode = Schema.RequiredMode.REQUIRED, example = "15800")
    @ExcelProperty("主键ID，自增")
    private Long id;

    @Schema(description = "文件名", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("文件名")
    private String fileName;

    @Schema(description = "文件路径", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("文件路径")
    private String fileUrl;

    @Schema(description = "文件状态：0-未翻译 1-翻译完成 2-异常文件", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("文件状态：0-未翻译 1-翻译完成 2-异常文件")
    private Integer fileStatus;

    @Schema(description = "文件类型", example = "1")
    @ExcelProperty("文件类型")
    private String fileType;

    @Schema(description = "文件说明")
    @ExcelProperty("文件说明")
    private String context;

    @Schema(description = "用户名称", example = "SVA09969")
    @ExcelProperty("用户名称")
    private String username;

    @Schema(description = "原始文件路径")
    @ExcelProperty("原始文件路径")
    private String sourceFileUrl;

    @Schema(description = "对比文件路径")
    @ExcelProperty("对比文件路径")
    private String compareFileUrl;

    @Schema(description = "QC文件路径")
    @ExcelProperty("QC文件路径")
    private String qcFileUrl;

    @Schema(description = "双语文件路径")
    @ExcelProperty("双语文件路径")
    private String contrastFileUrl;

    @Schema(description = "文件后缀", example = ".docx")
    @ExcelProperty("文件后缀")
    private String fileExt;

    @Schema(description = "文件大小", example = "1024000")
    @ExcelProperty("文件大小")
    private Long fileSize;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}