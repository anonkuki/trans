package cn.iocoder.sva.module.system.controller.admin.extlink.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.sva.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.sva.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 外链和角色关联分页 Request VO")
@Data
public class ExternalLinkRolePageReqVO extends PageParam {

    @Schema(description = "外链编号", example = "13077")
    private Long linkId;

    @Schema(description = "角色编号", example = "18559")
    private Long roleId;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}