package cn.iocoder.sva.module.ai.controller.admin.translation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - AI词句翻译 Request VO")
@Data
public class TranTextTranslateReqVO {

    @Schema(description = "待翻译原文", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "翻译内容不能为空")
    @Size(max = 20000, message = "翻译内容过长，请控制在 20000 字以内")
    private String sourceText;

    @Schema(description = "目标语言", requiredMode = Schema.RequiredMode.REQUIRED, example = "English")
    @NotBlank(message = "目标语言不能为空")
    private String targetLang;

    @Schema(description = "AI模型ID", example = "1")
    private Long modelId;

    @Schema(description = "聊天角色ID（场景）", example = "1")
    private Long roleId;

    @Schema(description = "术语库ID列表（支持多选）", example = "[1, 2, 3]")
    private List<Long> glossaryIds;

}
