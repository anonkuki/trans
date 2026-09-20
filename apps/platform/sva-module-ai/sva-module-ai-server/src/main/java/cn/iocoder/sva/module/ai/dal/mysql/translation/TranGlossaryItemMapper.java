package cn.iocoder.sva.module.ai.dal.mysql.translation;

import java.util.*;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.sva.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranGlossaryItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.*;

@Mapper
public interface TranGlossaryItemMapper extends BaseMapperX<TranGlossaryItemDO> {

    default PageResult<TranGlossaryItemDO> selectPage(TranGlossaryItemPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TranGlossaryItemDO>()
                .eqIfPresent(TranGlossaryItemDO::getSourceLanguage, reqVO.getSourceLanguage())
                .eqIfPresent(TranGlossaryItemDO::getTargetLanguage, reqVO.getTargetLanguage())
                .eqIfPresent(TranGlossaryItemDO::getGlossaryId, reqVO.getGlossaryId())
                .betweenIfPresent(TranGlossaryItemDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(TranGlossaryItemDO::getId));
    }

    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    @Select("SELECT * FROM ai_tran_glossary_item WHERE glossary_id = #{glossaryId} ORDER BY id DESC")
    List<TranGlossaryItemDO> selectListByGlossaryId(Long glossaryId);

}