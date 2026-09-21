package cn.iocoder.sva.module.system.dal.mysql.extlink;

import java.util.*;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.sva.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.sva.module.system.dal.dataobject.extlink.ExternalLinkRoleDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import cn.iocoder.sva.module.system.controller.admin.extlink.vo.*;

/**
 * 外链和角色关联 Mapper
 *
 * @author like
 */
@Mapper
public interface ExternalLinkRoleMapper extends BaseMapperX<ExternalLinkRoleDO> {

    default PageResult<ExternalLinkRoleDO> selectPage(ExternalLinkRolePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ExternalLinkRoleDO>()
                .eqIfPresent(ExternalLinkRoleDO::getLinkId, reqVO.getLinkId())
                .eqIfPresent(ExternalLinkRoleDO::getRoleId, reqVO.getRoleId())
                .betweenIfPresent(ExternalLinkRoleDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ExternalLinkRoleDO::getId));
    }

    default List<ExternalLinkRoleDO> selectListByRoleId(Long roleId) {
        return selectList(ExternalLinkRoleDO::getRoleId, roleId);
    }

    default List<ExternalLinkRoleDO> selectListByRoleIds(Collection<Long> roleIds) {
        return selectList(new LambdaQueryWrapperX<ExternalLinkRoleDO>()
                .in(ExternalLinkRoleDO::getRoleId, roleIds));
    }

    /**
     * 物理删除指定外链的关联数据，真实删除，不做逻辑删除，并忽略 deleted 字段
     */
    @Delete("DELETE FROM system_external_link_role WHERE link_id = #{linkId}")
    void physicalDeleteByLinkId(@Param("linkId") Long linkId);

    /**
     * 物理删除指定角色的关联数据，真实删除，不做逻辑删除，并忽略 deleted 字段
     */
    @Delete("DELETE FROM system_external_link_role WHERE role_id = #{roleId}")
    void physicalDeleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 物理删除指定角色下的指定外链关联，真实删除，不做逻辑删除，并忽略 deleted 字段
     */
    @Delete("<script>DELETE FROM system_external_link_role WHERE role_id = #{roleId} AND link_id IN " +
            "<foreach collection='linkIds' item='linkId' open='(' separator=',' close=')'>#{linkId}</foreach></script>")
    void physicalDeleteByRoleIdAndLinkIds(@Param("roleId") Long roleId, @Param("linkIds") Collection<Long> linkIds);

}