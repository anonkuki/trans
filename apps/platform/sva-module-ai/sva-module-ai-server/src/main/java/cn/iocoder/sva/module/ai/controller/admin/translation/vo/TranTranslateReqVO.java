package cn.iocoder.sva.module.ai.controller.admin.translation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Schema(description = "管理后台 - AI翻译 Request VO")
@Data
public class TranTranslateReqVO {

    @Schema(description = "翻译文件", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "翻译文件不能为空")
    private MultipartFile file;

    @Schema(description = "目标语言", requiredMode = Schema.RequiredMode.REQUIRED, example = "en")
    @NotBlank(message = "目标语言不能为空")
    private String targetLang;

    @Schema(description = "术语库ID", example = "1")
    private Long glossaryId;

    @Schema(description = "是否使用术语替换", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private boolean useGlossaryReplace = false;

    @Schema(description = "是否严格格式", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private boolean strictFormat = false;

    @Schema(description = "是否启用对照", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private boolean enableComparison = false;

    @Schema(description = "是否启用质检", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private boolean enableQc = false;

    @Schema(description = "AI模型ID", example = "1")
    private Long modelId;

    @Schema(description = "是否禁用缓存（1=禁用，0=启用）", example = "0")
    private Integer disableCache = 0;

    @Schema(description = "聊天角色ID（用于自定义翻译提示词）", example = "1")
    private Long roleId;

}