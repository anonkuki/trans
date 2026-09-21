package cn.iocoder.sva.module.ai.controller.admin.translation.vo;

import cn.iocoder.sva.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.sva.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - AI词句翻译历史分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class TranTextHistoryPageReqVO extends PageParam {

    @Schema(description = "模糊搜索关键字（匹配原文或译文）", example = "疫苗")
    private String keyword;

    @Schema(description = "用户名称（工号）", example = "SVA09969")
    private String username;

    @Schema(description = "翻译时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
