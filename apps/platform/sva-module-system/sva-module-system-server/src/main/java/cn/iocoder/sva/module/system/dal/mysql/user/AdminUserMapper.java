package cn.iocoder.sva.module.system.dal.mysql.user;

import cn.iocoder.sva.framework.common.enums.CommonStatusEnum;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.sva.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.sva.module.system.controller.admin.user.vo.user.UserPageReqVO;
import cn.iocoder.sva.module.system.dal.dataobject.user.AdminUserDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface AdminUserMapper extends BaseMapperX<AdminUserDO> {

    default AdminUserDO selectByUsername(String username) {
        return selectOne(AdminUserDO::getUsername, username);
    }

    default AdminUserDO selectByEmail(String email) {
        // 只查询已启用的用户，忽略未启用的用户
        return selectOne(new LambdaQueryWrapperX<AdminUserDO>()
                .eq(AdminUserDO::getEmail, email)
                .eq(AdminUserDO::getStatus, CommonStatusEnum.ENABLE.getStatus()));
    }

    default AdminUserDO selectByMobile(String mobile) {
        // 只查询已启用的用户，忽略未启用的用户
        return selectOne(new LambdaQueryWrapperX<AdminUserDO>()
                .eq(AdminUserDO::getMobile, mobile)
                .eq(AdminUserDO::getStatus, CommonStatusEnum.ENABLE.getStatus()));
    }

    default PageResult<AdminUserDO> selectPage(UserPageReqVO reqVO, Collection<Long> deptIds, Collection<Long> userIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AdminUserDO>()
                .likeIfPresent(AdminUserDO::getUsername, reqVO.getUsername())
                .likeIfPresent(AdminUserDO::getNickname, reqVO.getNickname())
                .likeIfPresent(AdminUserDO::getMobile, reqVO.getMobile())
                .eqIfPresent(AdminUserDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(AdminUserDO::getCreateTime, reqVO.getCreateTime())
                .inIfPresent(AdminUserDO::getDeptId, deptIds)
                .inIfPresent(AdminUserDO::getId, userIds)
                .orderByDesc(AdminUserDO::getId));
    }

    default List<AdminUserDO> selectListByNickname(String nickname) {
        return selectList(new LambdaQueryWrapperX<AdminUserDO>().like(AdminUserDO::getNickname, nickname));
    }

    default List<AdminUserDO> selectListByStatus(Integer status) {
        return selectList(AdminUserDO::getStatus, status);
    }

    default List<AdminUserDO> selectListByDeptIds(Collection<Long> deptIds) {
        return selectList(AdminUserDO::getDeptId, deptIds);
    }

}
