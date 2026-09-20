package cn.iocoder.sva.module.system.dal.mysql.role;

import java.util.*;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.sva.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.sva.module.system.dal.dataobject.role.DeptRoleDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.sva.module.system.controller.admin.role.vo.*;

/**
 * 部门和角色关联 Mapper
 *
 * @author like
 */
@Mapper
public interface DeptRoleMapper extends BaseMapperX<DeptRoleDO> {

    default PageResult<DeptRoleDO> selectPage(DeptRolePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DeptRoleDO>()
                .eqIfPresent(DeptRoleDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(DeptRoleDO::getRoleId, reqVO.getRoleId())
                .betweenIfPresent(DeptRoleDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(DeptRoleDO::getId));
    }

}