package cn.iocoder.sva.module.ai.controller.admin.file.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.sva.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.sva.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 对话文件记录分页 Request VO")
@Data
public class ChatbotFilePageReqVO extends PageParam {

    @Schema(description = "文件名称")
    private String fileName;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "用户问题")
    private String userQuestion;

    @Schema(description = "文件内容")
    private String fileContent;

    @Schema(description = "文件类型（如：image, document, video等）", example = "2")
    private String fileType;

    @Schema(description = "用户工号")
    private String userJobNumber;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}