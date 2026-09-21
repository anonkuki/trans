package cn.iocoder.sva.module.ai.controller.admin.calendar.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.sva.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.sva.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 日程导出分页 Request VO")
@Data
public class CalendarPageReqVO extends PageParam {

    @Schema(description = "所属人员", example = "张三")
    private String username;

    @Schema(description = "日历ID", example = "21396")
    private String calendarId;

    @Schema(description = "日程ID", example = "18401")
    private String eventId;

    @Schema(description = "日程组织者的日历ID", example = "4922")
    private String organizerCalendarId;

    @Schema(description = "日程标题")
    private String summary;

    @Schema(description = "日程描述")
    private String description;

    @Schema(description = "开始时间（格式：yyyy-MM-dd HH:mm:ss）", example = "2024-01-01 00:00:00")
    private String startTime;

    @Schema(description = "结束时间（格式：yyyy-MM-dd HH:mm:ss）", example = "2024-12-31 23:59:59")
    private String endTime;

    @Schema(description = "视频会议类型	可选值有：	vc：飞书视频会议。取该类型时，vchat 内的其他字段无效。	third_party：第三方链接视频会议。取该类型时，vchat 内仅生效 icon_type、description、meeting_url 字段。	no_meeting：无视频会议。取该类型时，vchat 内的其他字段无效。	lark_live：飞书直播，只读参数。	unknown：未知类型，用于兼容的只读参数。", example = "2")
    private String vcType;

    @Schema(description = "第三方视频会议 icon 类型	可选值有：	vc：飞书视频会议 icon	live：直播视频会议 icon	default：默认 icon", example = "1")
    private String iconType;

    @Schema(description = "第三方视频会议文案")
    private String vcDescription;

    @Schema(description = "视频会议 URL")
    private String meetingUrl;

    @Schema(description = "日程公开范围:	default：默认权限，跟随日历权限，即默认仅向他人显示是否忙碌	public：公开，显示日程详情	private：私密，仅自己可见详情")
    private String visibility;

    @Schema(description = "参与人权限。	可选值有：	none：无法编辑日程、无法邀请其它参与人、无法查看参与人列表	can_see_others：无法编辑日程、无法邀请其它参与人、可以查看参与人列表	can_invite_others：无法编辑日程、可以邀请其它参与人、可以查看参与人列表	can_modify_event：可以编辑日程、可以邀请其它参与人、可以查看参与人列表")
    private String attendeeAbility;

    @Schema(description = "日程占用的忙闲状态。仅新建日程时对所有参与人生效，之后修改该属性仅对当前身份生效。	可选值有：	busy：忙碌	free：空闲", example = "2")
    private String freeBusyStatus;

    @Schema(description = "地点名称")
    private String locationName;

    @Schema(description = "地点地址")
    private String locationAddress;

    @Schema(description = "地点坐标纬度信息")
    private Double locationLatitude;

    @Schema(description = "地点坐标经度信息")
    private Double locationLongitude;

    @Schema(description = "日程颜色，由颜色 RGB 值的 int32 表示")
    private Integer color;

    @Schema(description = "日程提醒时间的偏移量。该参数仅对当前身份生效。	正数时表示在日程开始前 X 分钟提醒。	负数时表示在日程开始后 X 分钟提醒。")
    private String remindersMinutes;

    @Schema(description = "重复日程的重复性规则")
    private String recurrence;

    @Schema(description = "日程状态。	可选值有：	tentative：未回应	confirmed：已确认	cancelled：日程已取消", example = "1")
    private String status;

    @Schema(description = "日程是否是一个重复日程的例外日程")
    private Integer isException;

    @Schema(description = "例外日程对应的原重复日程的 event_id", example = "25569")
    private String recurringEventId;

    @Schema(description = "日程的创建时间（秒级时间戳）")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private String[] calendarCreateTime;

    @Schema(description = "日程组织者 user ID", example = "21423")
    private String userId;

    @Schema(description = "日程组织者姓名", example = "赵六")
    private String displayName;

    @Schema(description = "日程的 app_link，跳转到具体的某个日程")
    private String appLink;

    @Schema(description = "附件信息")
    private String fileInfo;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

    @Schema(description = "选中的事件ID列表（用于导出勾选的数据）")
    private List<String> eventIds;

}