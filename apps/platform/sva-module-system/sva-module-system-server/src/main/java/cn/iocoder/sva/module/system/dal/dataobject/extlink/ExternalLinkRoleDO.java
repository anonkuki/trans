package cn.iocoder.sva.module.system.dal.dataobject.extlink;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.sva.framework.mybatis.core.dataobject.BaseDO;

/**
 * 外链和角色关联 DO
 *
 * @author like
 */
@TableName("system_external_link_role")
@KeySequence("system_external_link_role_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalLinkRoleDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 外链编号
     */
    private Long linkId;
    /**
     * 角色编号
     */
    private Long roleId;


}