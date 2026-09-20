package cn.iocoder.sva.module.ai.controller.admin.feishu.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.sva.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.sva.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 飞书消息接收记录分页 Request VO")
@Data
public class FeishuMessagePageReqVO extends PageParam {

    @Schema(description = "飞书用户ID", example = "8933")
    private String userId;

    @Schema(description = "飞书OpenID", example = "15319")
    private String openId;

    @Schema(description = "飞书UnionID", example = "10625")
    private String unionId;

    @Schema(description = "用户发送的消息内容")
    private String userMessage;

    @Schema(description = "原始消息JSON")
    private String originalJson;

    @Schema(description = "消息类型", example = "1")
    private String messageType;

    @Schema(description = "消息ID", example = "8444")
    private String messageId;

    @Schema(description = "会话类型(p2p/group)", example = "2")
    private String chatType;

    @Schema(description = "会话ID", example = "11243")
    private String chatId;

    @Schema(description = "企业唯一标识")
    private String tenantKey;

    @Schema(description = "发送者类型", example = "1")
    private String senderType;

    @Schema(description = "机器人回复内容")
    private String replyContent;

    @Schema(description = "是否回复成功 0-否 1-是")
    private Boolean replySuccess;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}