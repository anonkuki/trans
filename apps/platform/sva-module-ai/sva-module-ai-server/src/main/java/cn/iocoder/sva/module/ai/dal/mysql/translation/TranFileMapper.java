package cn.iocoder.sva.module.ai.dal.mysql.translation;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.sva.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranFilePageReqVO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranFileDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI翻译文件信息 Mapper
 *
 * @author like
 */
@Mapper
public interface TranFileMapper extends BaseMapperX<TranFileDO> {

    default PageResult<TranFileDO> selectPage(TranFilePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TranFileDO>()
                .likeIfPresent(TranFileDO::getFileName, reqVO.getFileName())
                .eqIfPresent(TranFileDO::getUsername, reqVO.getUsername())
                .betweenIfPresent(TranFileDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(TranFileDO::getId));
    }

}
