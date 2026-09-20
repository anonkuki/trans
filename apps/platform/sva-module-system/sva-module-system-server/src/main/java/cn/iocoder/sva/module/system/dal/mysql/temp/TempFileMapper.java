package cn.iocoder.sva.module.system.dal.mysql.temp;

import java.util.*;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.sva.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.sva.module.system.dal.dataobject.temp.TempFileDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.sva.module.system.controller.admin.temp.vo.*;

/**
 * 模板管理 Mapper
 *
 * @author like
 */
@Mapper
public interface TempFileMapper extends BaseMapperX<TempFileDO> {

    default PageResult<TempFileDO> selectPage(TempFilePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TempFileDO>()
                .likeIfPresent(TempFileDO::getTempName, reqVO.getTempName())
                .likeIfPresent(TempFileDO::getName, reqVO.getName())
                .betweenIfPresent(TempFileDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(TempFileDO::getId));
    }

}