package cn.iocoder.sva.module.ai.dal.dataobject.translation;

import cn.iocoder.sva.framework.mybatis.core.dataobject.BaseCommonDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * AI翻译文件信息 DO
 *
 * @author like
 */
@TableName("ai_tran_file")
@KeySequence("ai_tran_file_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranFileDO extends BaseCommonDO {

    /**
     * 主键ID，自增
     */
    @TableId
    private Long id;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件路径
     */
    private String fileUrl;

    /**
     * 文件状态：0-未翻译 1-翻译完成 2-异常文件
     */
    private Integer fileStatus;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件说明
     */
    private String context;

    /**
     * 用户名称
     */
    private String username;

    /**
     * 原始文件路径
     */
    private String sourceFileUrl;

    /**
     * 对比文件路径
     */
    private String compareFileUrl;

    /**
     * QC文件路径
     */
    private String qcFileUrl;

    /**
     * 双语文件路径
     */
    private String contrastFileUrl;

    /**
     * 文件后缀
     */
    private String fileExt;

    /**
     * 文件大小
     */
    private Long fileSize;

}