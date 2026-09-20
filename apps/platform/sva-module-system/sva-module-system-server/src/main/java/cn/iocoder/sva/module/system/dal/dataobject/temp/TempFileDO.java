package cn.iocoder.sva.module.system.dal.dataobject.temp;

import cn.iocoder.sva.framework.mybatis.core.dataobject.BaseCommonDO;
import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.sva.framework.mybatis.core.dataobject.BaseDO;

/**
 * 模板管理 DO
 *
 * @author like
 */
@TableName("system_temp_file")
@KeySequence("system_temp_file_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TempFileDO extends BaseCommonDO {

    /**
     * 文件编号
     */
    @TableId
    private Long id;
    /**
     * 模板标识
     */
    private String tempName;
    /**
     * 文件名
     */
    private String name;
    /**
     * 文件 URL
     */
    private String url;
    /**
     * 文件类型
     */
    private String type;
    /**
     * 文件大小
     */
    private Integer size;


}