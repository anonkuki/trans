package cn.iocoder.sva.module.ai.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 对话文件记录 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ChatbotFileRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "6087")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "文件名称")
    @ExcelProperty("文件名称")
    private String fileName;

    @Schema(description = "文件路径")
    @ExcelProperty("文件路径")
    private String filePath;

    @Schema(description = "用户问题")
    @ExcelProperty("用户问题")
    private String userQuestion;

    @Schema(description = "文件内容")
    @ExcelProperty("文件内容")
    private String fileContent;

    @Schema(description = "文件类型（如：image, document, video等）", example = "2")
    @ExcelProperty("文件类型（如：image, document, video等）")
    private String fileType;

    @Schema(description = "用户工号")
    @ExcelProperty("用户工号")
    private String userJobNumber;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}