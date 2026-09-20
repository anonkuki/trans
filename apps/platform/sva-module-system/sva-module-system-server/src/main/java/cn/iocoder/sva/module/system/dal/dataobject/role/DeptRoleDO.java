package cn.iocoder.sva.module.system.dal.dataobject.role;

import cn.iocoder.sva.framework.mybatis.core.dataobject.BaseCommonDO;
import cn.iocoder.sva.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 部门和角色关联 DO
 *
 * @author like
 */
@TableName("system_dept_role")
@KeySequence("system_dept_role_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeptRoleDO extends BaseCommonDO {

    /**
     * 自增编号
     */
    @TableId
    private Long id;
    /**
     * 部门ID
     */
    private Long deptId;
    /**
     * 角色ID
     */
    private Long roleId;


}