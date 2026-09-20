package cn.iocoder.sva.module.system.dal.mysql.permission;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.sva.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.sva.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.sva.module.system.controller.admin.permission.vo.role.RolePageReqVO;
import cn.iocoder.sva.module.system.dal.dataobject.permission.RoleDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Mapper
public interface RoleMapper extends BaseMapperX<RoleDO> {

    default PageResult<RoleDO> selectPage(RolePageReqVO reqVO) {

        Boolean flag = reqVO.getFlag();
        if(flag){
            return selectPage(reqVO, new LambdaQueryWrapperX<RoleDO>()
                    .likeIfPresent(RoleDO::getName, reqVO.getName())
                    .likeIfPresent(RoleDO::getCode, reqVO.getCode())
                    .eqIfPresent(RoleDO::getStatus, reqVO.getStatus())
                    .betweenIfPresent(BaseDO::getCreateTime, reqVO.getCreateTime())
                    .orderByAsc(RoleDO::getSort));
        }
        return selectPage(reqVO, new LambdaQueryWrapperX<RoleDO>()
                .likeIfPresent(RoleDO::getName, reqVO.getName())
                .likeIfPresent(RoleDO::getCode, reqVO.getCode())
                .eqIfPresent(RoleDO::getStatus, reqVO.getStatus())
                .in(RoleDO::getCreatorRole,reqVO.getRoleIds())
                .betweenIfPresent(BaseDO::getCreateTime, reqVO.getCreateTime())
                .orderByAsc(RoleDO::getSort));

    }

    default List<RoleDO> selectListSelf(RolePageReqVO reqVO) {
        Boolean flag = reqVO.getFlag();
        if(flag){
            return selectList(new LambdaQueryWrapperX<RoleDO>()
                    .likeIfPresent(RoleDO::getName, reqVO.getName())
                    .likeIfPresent(RoleDO::getCode, reqVO.getCode())
                    .eqIfPresent(RoleDO::getStatus, reqVO.getStatus())
                    .betweenIfPresent(BaseDO::getCreateTime, reqVO.getCreateTime())
                    .orderByAsc(RoleDO::getSort));
        }
        return selectList(new LambdaQueryWrapperX<RoleDO>()
                .likeIfPresent(RoleDO::getName, reqVO.getName())
                .likeIfPresent(RoleDO::getCode, reqVO.getCode())
                .eqIfPresent(RoleDO::getStatus, reqVO.getStatus())
                .in(RoleDO::getCreatorRole,reqVO.getRoleIds())
                .betweenIfPresent(BaseDO::getCreateTime, reqVO.getCreateTime())
                .orderByAsc(RoleDO::getSort));
    }

    default List<RoleDO> selectRoleSelf(Set<Long> ids,Integer status) {

        return selectList(new LambdaQueryWrapperX<RoleDO>()
                .eqIfPresent(RoleDO::getStatus, status)
                .eq(RoleDO::getCreatorRole,1)
                .in(RoleDO::getId,ids)
                .orderByAsc(RoleDO::getSort));
    }

    default RoleDO selectByName(String name) {
        return selectOne(RoleDO::getName, name);
    }

    default RoleDO selectByCode(String code) {
        return selectOne(RoleDO::getCode, code);
    }

    default List<RoleDO> selectListByStatus(@Nullable Collection<Integer> statuses) {
        return selectList(RoleDO::getStatus, statuses);
    }

}
