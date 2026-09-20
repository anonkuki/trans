package cn.iocoder.sva.module.ai.dal.dataobject.feishu;

import lombok.Data;

import java.util.List;

/**
 * 飞书消息接收实体
 * 存储：三个用户ID + 消息文本内容 + 原始JSON字符串
 */
@Data
public class FeishuMessageDTO {

    /**
     * 飞书用户ID（企业内唯一）
     */
    private String userId;

    /**
     * 飞书open_id（应用维度唯一）
     */
    private String openId;

    /**
     * 飞书union_id（跨应用唯一）
     */
    private String unionId;

    /**
     * 用户发送的消息文本
     */
    private String userMessage;

    private String messageType;   // 消息类型：text/file/image/post等
    private String messageId;     // 消息ID
    private String chatType;      // 会话类型：p2p/group
    private String chatId;        // 会话ID
    private String tenantKey;     // 企业唯一标识
    private String senderType;    // 发送者类型：user

    /**
     * 【新增】文件相关信息（当 messageType 为 file 时使用）
     */
    private String fileKey;       // 文件 key
    private String fileName;      // 文件名
    private Long fileSize;        // 文件大小（字节）
    private String fileType;      // 文件类型扩展名

    /**
     * 【新增】图片相关信息（当 messageType 为 image 时使用）
     */
    private String imageKey;      // 图片 key

    /**
     * 【新增】富文本消息中的图片列表（当 messageType 为 post 时使用）
     */
    private List<String> postImageKeys;  // 富文本中的图片 key 列表

    /**
     * 【新增】原始事件的完整JSON字符串
     */
    private String originalJson;

}