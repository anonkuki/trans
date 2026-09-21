package cn.iocoder.sva.module.ai.dal.dataobject.calendar;

import cn.iocoder.sva.framework.mybatis.core.dataobject.BaseCommonDO;
import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.sva.framework.mybatis.core.dataobject.BaseDO;

/**
 * 日程导出 DO
 *
 * @author like
 */
@TableName("ai_calendar")
@KeySequence("ai_calendar_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarDO extends BaseCommonDO {

    /**
     * 主键id
     */
    @TableId
    private Long id;
    /**
     * 所属人员
     */
    private String username;
    /**
     * 日历ID
     */
    private String calendarId;
    /**
     * 日程ID
     */
    private String eventId;
    /**
     * 日程组织者的日历ID
     */
    private String organizerCalendarId;
    /**
     * 日程标题
     */
    private String summary;
    /**
     * 日程描述
     */
    private String description;
    /**
     * 日程开始时间
     */
    private LocalDateTime startTime;
    /**
     * 日程结束时间
     */
    private LocalDateTime endTime;
    /**
     * 视频会议类型	可选值有：	vc：飞书视频会议。取该类型时，vchat 内的其他字段无效。	third_party：第三方链接视频会议。取该类型时，vchat 内仅生效 icon_type、description、meeting_url 字段。	no_meeting：无视频会议。取该类型时，vchat 内的其他字段无效。	lark_live：飞书直播，只读参数。	unknown：未知类型，用于兼容的只读参数。
     */
    private String vcType;
    /**
     * 第三方视频会议 icon 类型	可选值有：	vc：飞书视频会议 icon	live：直播视频会议 icon	default：默认 icon
     */
    private String iconType;
    /**
     * 第三方视频会议文案
     */
    private String vcDescription;
    /**
     * 视频会议 URL
     */
    private String meetingUrl;
    /**
     * 日程公开范围:	default：默认权限，跟随日历权限，即默认仅向他人显示是否忙碌	public：公开，显示日程详情	private：私密，仅自己可见详情
     */
    private String visibility;
    /**
     * 参与人权限。	可选值有：	none：无法编辑日程、无法邀请其它参与人、无法查看参与人列表	can_see_others：无法编辑日程、无法邀请其它参与人、可以查看参与人列表	can_invite_others：无法编辑日程、可以邀请其它参与人、可以查看参与人列表	can_modify_event：可以编辑日程、可以邀请其它参与人、可以查看参与人列表
     */
    private String attendeeAbility;
    /**
     * 日程占用的忙闲状态。仅新建日程时对所有参与人生效，之后修改该属性仅对当前身份生效。	可选值有：	busy：忙碌	free：空闲
     */
    private String freeBusyStatus;
    /**
     * 地点名称
     */
    private String locationName;
    /**
     * 地点地址
     */
    private String locationAddress;
    /**
     * 地点坐标纬度信息
     */
    private Double locationLatitude;
    /**
     * 地点坐标经度信息
     */
    private Double locationLongitude;
    /**
     * 日程颜色，由颜色 RGB 值的 int32 表示
     */
    private Integer color;
    /**
     * 日程提醒时间的偏移量。该参数仅对当前身份生效。	正数时表示在日程开始前 X 分钟提醒。	负数时表示在日程开始后 X 分钟提醒。
     */
    private String remindersMinutes;
    /**
     * 重复日程的重复性规则
     */
    private String recurrence;
    /**
     * 日程状态。	可选值有：	tentative：未回应	confirmed：已确认	cancelled：日程已取消
     */
    private String status;
    /**
     * 日程是否是一个重复日程的例外日程
     */
    private Integer isException;
    /**
     * 例外日程对应的原重复日程的 event_id
     */
    private String recurringEventId;
    /**
     * 日程的创建时间（秒级时间戳）
     */
    private String calendarCreateTime;
    /**
     * 日程组织者 user ID
     */
    private String userId;
    /**
     * 日程组织者姓名
     */
    private String displayName;
    /**
     * 日程的 app_link，跳转到具体的某个日程
     */
    private String appLink;
    /**
     * 附件信息
     */
    private String fileInfo;


}