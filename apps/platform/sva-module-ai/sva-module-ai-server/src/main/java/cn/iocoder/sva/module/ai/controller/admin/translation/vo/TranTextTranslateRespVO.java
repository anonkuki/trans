package cn.iocoder.sva.module.ai.controller.admin.translation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - AI词句翻译 Response VO")
@Data
public class TranTextTranslateRespVO {

    @Schema(description = "历史记录ID", example = "1024")
    private Long id;

    @Schema(description = "原文")
    private String sourceText;

    @Schema(description = "译文")
    private String targetText;

    @Schema(description = "目标语言", example = "English")
    private String targetLang;

    @Schema(description = "模型名称", example = "DeepSeek")
    private String modelName;

    @Schema(description = "场景名称", example = "翻译助手")
    private String roleName;

    @Schema(description = "翻译时间")
    private LocalDateTime createTime;

}
