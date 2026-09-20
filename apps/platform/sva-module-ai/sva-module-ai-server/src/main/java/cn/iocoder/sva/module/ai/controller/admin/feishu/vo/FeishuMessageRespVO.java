package cn.iocoder.sva.module.ai.controller.admin.feishu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 飞书消息接收记录 Response VO")
@Data
@ExcelIgnoreUnannotated
public class FeishuMessageRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "25003")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "飞书用户ID", example = "8933")
    @ExcelProperty("飞书用户ID")
    private String userId;

    @Schema(description = "飞书OpenID", example = "15319")
    @ExcelProperty("飞书OpenID")
    private String openId;

    @Schema(description = "飞书UnionID", example = "10625")
    @ExcelProperty("飞书UnionID")
    private String unionId;

    @Schema(description = "用户发送的消息内容")
    @ExcelProperty("用户发送的消息内容")
    private String userMessage;

    @Schema(description = "原始消息JSON")
    @ExcelProperty("原始消息JSON")
    private String originalJson;

    @Schema(description = "消息类型", example = "1")
    @ExcelProperty("消息类型")
    private String messageType;

    @Schema(description = "消息ID", example = "8444")
    @ExcelProperty("消息ID")
    private String messageId;

    @Schema(description = "会话类型(p2p/group)", example = "2")
    @ExcelProperty("会话类型(p2p/group)")
    private String chatType;

    @Schema(description = "会话ID", example = "11243")
    @ExcelProperty("会话ID")
    private String chatId;

    @Schema(description = "企业唯一标识")
    @ExcelProperty("企业唯一标识")
    private String tenantKey;

    @Schema(description = "发送者类型", example = "1")
    @ExcelProperty("发送者类型")
    private String senderType;

    @Schema(description = "机器人回复内容")
    @ExcelProperty("机器人回复内容")
    private String replyContent;

    @Schema(description = "是否回复成功 0-否 1-是")
    @ExcelProperty("是否回复成功 0-否 1-是")
    private Boolean replySuccess;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}