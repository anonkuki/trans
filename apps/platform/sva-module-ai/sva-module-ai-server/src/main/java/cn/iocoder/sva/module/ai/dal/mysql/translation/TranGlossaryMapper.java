package cn.iocoder.sva.module.ai.dal.mysql.translation;

import java.util.*;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.sva.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranGlossaryDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.*;

/**
 * 术语库管理 Mapper
 *
 * @author like
 */
@Mapper
public interface TranGlossaryMapper extends BaseMapperX<TranGlossaryDO> {

    default PageResult<TranGlossaryDO> selectPage(TranGlossaryPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TranGlossaryDO>()
                .likeIfPresent(TranGlossaryDO::getGlossaryName, reqVO.getGlossaryName())
                .eqIfPresent(TranGlossaryDO::getSourceLanguage, reqVO.getSourceLanguage())
                .eqIfPresent(TranGlossaryDO::getTargetLanguage, reqVO.getTargetLanguage())
                .eqIfPresent(TranGlossaryDO::getLanguageDirection, reqVO.getLanguageDirection())
                .eqIfPresent(TranGlossaryDO::getItemCount, reqVO.getItemCount())
                .eqIfPresent(TranGlossaryDO::getRoleId, reqVO.getRoleId())
                .eqIfPresent(TranGlossaryDO::getUsername, reqVO.getUsername())
                .eqIfPresent(TranGlossaryDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(TranGlossaryDO::getIsEnabled, reqVO.getIsEnabled())
                .betweenIfPresent(TranGlossaryDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(TranGlossaryDO::getId));
    }

}