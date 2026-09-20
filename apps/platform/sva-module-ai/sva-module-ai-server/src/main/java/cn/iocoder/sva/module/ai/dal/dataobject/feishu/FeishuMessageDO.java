package cn.iocoder.sva.module.ai.dal.dataobject.feishu;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.sva.framework.mybatis.core.dataobject.BaseDO;

/**
 * 飞书消息接收记录 DO
 *
 * @author like
 */
@TableName("ai_feishu_message")
@KeySequence("ai_feishu_message_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeishuMessageDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 飞书用户ID
     */
    private String userId;
    /**
     * 飞书OpenID
     */
    private String openId;
    /**
     * 飞书UnionID
     */
    private String unionId;
    /**
     * 用户发送的消息内容
     */
    private String userMessage;
    /**
     * 原始消息JSON
     */
    private String originalJson;
    /**
     * 消息类型
     */
    private String messageType;
    /**
     * 消息ID
     */
    private String messageId;
    /**
     * 会话类型(p2p/group)
     */
    private String chatType;
    /**
     * 会话ID
     */
    private String chatId;
    /**
     * 企业唯一标识
     */
    private String tenantKey;
    /**
     * 发送者类型
     */
    private String senderType;
    /**
     * 机器人回复内容
     */
    private String replyContent;
    /**
     * 是否回复成功 0-否 1-是
     */
    private Boolean replySuccess;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;


}