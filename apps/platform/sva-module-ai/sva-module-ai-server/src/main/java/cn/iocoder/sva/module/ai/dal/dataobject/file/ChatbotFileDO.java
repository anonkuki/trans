package cn.iocoder.sva.module.ai.dal.dataobject.file;

import cn.iocoder.sva.framework.mybatis.core.dataobject.BaseCommonDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 对话文件记录 DO
 *
 * @author like
 */
@TableName("ai_chatbot_file")
@KeySequence("ai_chatbot_file_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotFileDO extends BaseCommonDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 文件名称
     */
    private String fileName;
    /**
     * 文件路径
     */
    private String filePath;
    /**
     * 用户问题
     */
    private String userQuestion;
    /**
     * 文件内容
     */
    private String fileContent;
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    /**
     * 文件类型（如：feishuBot、chatBot等）
     */
    private String fileType;
    /**
     * 文件后缀（如：.jpg, .pdf, .docx）
     */
    private String fileExtension;
    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;
    /**
     * 用户工号
     */
    private String userJobNumber;
    /**
     * 所属会话
     */
    private String sessionId;


}