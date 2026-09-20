package cn.iocoder.sva.module.ai.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 对话文件记录新增/修改 Request VO")
@Data
public class ChatbotFileSaveReqVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "6087")
    private Long id;

    @Schema(description = "文件名称")
    private String fileName;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "用户问题")
    private String userQuestion;

    @Schema(description = "文件内容")
    private String fileContent;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "文件类型（如：image, document, video等）", example = "2")
    private String fileType;

    @Schema(description = "文件后缀（如：.jpg, .pdf, .docx）")
    private String fileExtension;

    @Schema(description = "上传时间")
    private LocalDateTime uploadTime;

    @Schema(description = "用户工号")
    private String userJobNumber;

    @Schema(description = "所属会话")
    private String sessionId;

}