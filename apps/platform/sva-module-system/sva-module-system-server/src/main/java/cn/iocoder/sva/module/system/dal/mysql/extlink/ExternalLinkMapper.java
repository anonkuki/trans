package cn.iocoder.sva.module.system.dal.mysql.extlink;

import java.util.*;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.sva.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.sva.module.system.dal.dataobject.extlink.ExternalLinkDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import cn.iocoder.sva.module.system.controller.admin.extlink.vo.*;

/**
 * 系统外链 Mapper
 *
 * @author like
 */
@Mapper
public interface ExternalLinkMapper extends BaseMapperX<ExternalLinkDO> {

    default PageResult<ExternalLinkDO> selectPage(ExternalLinkPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ExternalLinkDO>()
                .likeIfPresent(ExternalLinkDO::getName, reqVO.getName())
                .eqIfPresent(ExternalLinkDO::getUrl, reqVO.getUrl())
                .eqIfPresent(ExternalLinkDO::getIcon, reqVO.getIcon())
                .eqIfPresent(ExternalLinkDO::getDescription, reqVO.getDescription())
                .eqIfPresent(ExternalLinkDO::getCategory, reqVO.getCategory())
                .eqIfPresent(ExternalLinkDO::getSort, reqVO.getSort())
                .eqIfPresent(ExternalLinkDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ExternalLinkDO::getOpenTarget, reqVO.getOpenTarget())
                .eqIfPresent(ExternalLinkDO::getClickCount, reqVO.getClickCount())
                .betweenIfPresent(ExternalLinkDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ExternalLinkDO::getId));
    }

    default List<ExternalLinkDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<ExternalLinkDO>()
                .eq(ExternalLinkDO::getStatus, status)
                .orderByAsc(ExternalLinkDO::getSort));
    }

    default List<ExternalLinkDO> selectListByIdsAndStatus(Collection<Long> ids, Integer status) {
        return selectList(new LambdaQueryWrapperX<ExternalLinkDO>()
                .in(ExternalLinkDO::getId, ids)
                .eq(ExternalLinkDO::getStatus, status)
                .orderByAsc(ExternalLinkDO::getSort));
    }

    default void incrementClickCount(Long id) {
        update(null, new LambdaUpdateWrapper<ExternalLinkDO>()
                .eq(ExternalLinkDO::getId, id)
                .setSql("click_count = click_count + 1"));
    }

    /**
     * 物理删除外链，真实删除，不做逻辑删除，并忽略 deleted 字段
     */
    @Delete("DELETE FROM system_external_link WHERE id = #{id}")
    void physicalDeleteById(@Param("id") Long id);

    /**
     * 物理批量删除外链，真实删除，不做逻辑删除，并忽略 deleted 字段
     */
    @Delete("<script>DELETE FROM system_external_link WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    void physicalDeleteByIds(@Param("ids") Collection<Long> ids);

}