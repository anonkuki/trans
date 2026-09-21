package cn.iocoder.sva.module.ai.controller.admin.calendar.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 日程事件导出 Request VO")
@Data
public class CalendarEventExportReqVO {

    @Schema(description = "日历ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "feishu.cn_xxx@group.calendar.feishu.cn")
    private String calendarId;

    @Schema(description = "开始时间戳", example = "1781229600")
    private Long startTime;

    @Schema(description = "结束时间戳", example = "1781238600")
    private Long endTime;

    @Schema(description = "选中的事件ID列表（为空则导出全部）")
    private List<String> eventIds;

    @Schema(description = "状态筛选（为空则导出全部）", example = "confirmed")
    private String status;

    @Schema(description = "排序状态：asc=升序, desc=降序", example = "asc")
    private String sortOrder;

    @Schema(description = "分页大小", example = "500")
    private Integer pageSize;
}
