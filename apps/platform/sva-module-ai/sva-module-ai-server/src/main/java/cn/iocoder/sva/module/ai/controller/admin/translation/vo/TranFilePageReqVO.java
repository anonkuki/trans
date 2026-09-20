package cn.iocoder.sva.module.ai.controller.admin.translation.vo;

import cn.iocoder.sva.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.sva.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - AI翻译文件信息分页 Request VO")
@Data
public class TranFilePageReqVO extends PageParam {

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "用户名称", example = "张三")
    private String username;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}