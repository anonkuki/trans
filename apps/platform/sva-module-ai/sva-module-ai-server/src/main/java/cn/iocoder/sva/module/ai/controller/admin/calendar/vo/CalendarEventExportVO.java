package cn.iocoder.sva.module.ai.controller.admin.calendar.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 日程事件导出 Response VO")
@Data
@ExcelIgnoreUnannotated
public class CalendarEventExportVO {

    @Schema(description = "序号")
    @ExcelProperty("序号")
    private Integer index;

    @Schema(description = "日程标题")
    @ExcelProperty("日程标题")
    private String summary;

    @Schema(description = "描述")
    @ExcelProperty("描述")
    private String description;

    @Schema(description = "开始时间")
    @ExcelProperty("开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    @ExcelProperty("结束时间")
    private String endTime;

    @Schema(description = "会议类型")
    @ExcelProperty("会议类型")
    private String vcType;

    @Schema(description = "会议链接")
    @ExcelProperty("会议链接")
    private String meetingUrl;

    @Schema(description = "地点名称")
    @ExcelProperty("地点名称")
    private String locationName;

    @Schema(description = "地点地址")
    @ExcelProperty("地点地址")
    private String locationAddress;
}
