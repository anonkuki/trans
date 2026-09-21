package cn.iocoder.sva.module.system.controller.admin.extlink.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.sva.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.sva.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 系统外链分页 Request VO")
@Data
public class ExternalLinkPageReqVO extends PageParam {

    @Schema(description = "外链名称（卡片标题）", example = "李四")
    private String name;

    @Schema(description = "外链地址（跳转目标网站）", example = "https://www.iocoder.cn")
    private String url;

    @Schema(description = "图标（卡片图标，如 ep:link）")
    private String icon;

    @Schema(description = "外链描述（卡片副标题）", example = "你猜")
    private String description;

    @Schema(description = "分类（首页卡片分组，如 办公系统）")
    private String category;

    @Schema(description = "显示顺序（首页卡片排序）")
    private Integer sort;

    @Schema(description = "状态（0开启 1关闭，CommonStatusEnum）", example = "1")
    private Integer status;

    @Schema(description = "打开方式（0当前窗口 1新窗口）")
    private Integer openTarget;

    @Schema(description = "点击次数", example = "11747")
    private Long clickCount;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}