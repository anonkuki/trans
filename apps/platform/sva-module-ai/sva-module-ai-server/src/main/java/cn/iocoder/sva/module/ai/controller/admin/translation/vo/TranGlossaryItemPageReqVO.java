package cn.iocoder.sva.module.ai.controller.admin.translation.vo;

import cn.iocoder.sva.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.sva.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 术语管理分页 Request VO")
@Data
public class TranGlossaryItemPageReqVO extends PageParam {

    @Schema(description = "源语言")
    private String sourceLanguage;

    @Schema(description = "目标语言")
    private String targetLanguage;

    @Schema(description = "所属术语库id", example = "31003")
    private Long glossaryId;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}