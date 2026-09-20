package cn.iocoder.sva.module.system.dal.mysql.sync;

import java.util.*;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.sva.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.sva.module.system.dal.dataobject.sync.SyncPositionDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.sva.module.system.controller.admin.sync.vo.*;

/**
 * 岗位信息同步 Mapper
 *
 * @author 李可
 */
@Mapper
public interface SyncPositionMapper extends BaseMapperX<SyncPositionDO> {

    default PageResult<SyncPositionDO> selectPage(SyncPositionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SyncPositionDO>()
                .eqIfPresent(SyncPositionDO::getGuid, reqVO.getGuid())
                .eqIfPresent(SyncPositionDO::getDcInfDtStatus, reqVO.getDcInfDtStatus())
                .eqIfPresent(SyncPositionDO::getDcInfDtStatusDescr, reqVO.getDcInfDtStatusDescr())
                .eqIfPresent(SyncPositionDO::getPositionNbr, reqVO.getPositionNbr())
                .eqIfPresent(SyncPositionDO::getEffdt, reqVO.getEffdt())
                .eqIfPresent(SyncPositionDO::getEffStatus, reqVO.getEffStatus())
                .eqIfPresent(SyncPositionDO::getEffStatusDescr, reqVO.getEffStatusDescr())
                .eqIfPresent(SyncPositionDO::getDescr, reqVO.getDescr())
                .eqIfPresent(SyncPositionDO::getDescrshort, reqVO.getDescrshort())
                .eqIfPresent(SyncPositionDO::getBusinessUnit, reqVO.getBusinessUnit())
                .eqIfPresent(SyncPositionDO::getBusinessDescr, reqVO.getBusinessDescr())
                .eqIfPresent(SyncPositionDO::getRegRegion, reqVO.getRegRegion())
                .eqIfPresent(SyncPositionDO::getRegRegionDescr, reqVO.getRegRegionDescr())
                .eqIfPresent(SyncPositionDO::getLocation, reqVO.getLocation())
                .eqIfPresent(SyncPositionDO::getDcLocationDescr, reqVO.getDcLocationDescr())
                .eqIfPresent(SyncPositionDO::getDeptid, reqVO.getDeptid())
                .eqIfPresent(SyncPositionDO::getDcDeptDescr50, reqVO.getDcDeptDescr50())
                .eqIfPresent(SyncPositionDO::getDcDeptDescrshort, reqVO.getDcDeptDescrshort())
                .eqIfPresent(SyncPositionDO::getJobcode, reqVO.getJobcode())
                .eqIfPresent(SyncPositionDO::getDcJobcodeDescr, reqVO.getDcJobcodeDescr())
                .eqIfPresent(SyncPositionDO::getDcJobcodeDescrs, reqVO.getDcJobcodeDescrs())
                .eqIfPresent(SyncPositionDO::getDcJobGroup, reqVO.getDcJobGroup())
                .eqIfPresent(SyncPositionDO::getDcJobGroupDescr, reqVO.getDcJobGroupDescr())
                .eqIfPresent(SyncPositionDO::getDcJobSequence, reqVO.getDcJobSequence())
                .eqIfPresent(SyncPositionDO::getDcJobSeqDescr, reqVO.getDcJobSeqDescr())
                .eqIfPresent(SyncPositionDO::getDcFirstJobCate, reqVO.getDcFirstJobCate())
                .eqIfPresent(SyncPositionDO::getDcJobcateDescr, reqVO.getDcJobcateDescr())
                .eqIfPresent(SyncPositionDO::getDcJobLevel, reqVO.getDcJobLevel())
                .eqIfPresent(SyncPositionDO::getDcJobLevelDescr, reqVO.getDcJobLevelDescr())
                .eqIfPresent(SyncPositionDO::getDcJobGrade, reqVO.getDcJobGrade())
                .eqIfPresent(SyncPositionDO::getDcJobGradeDescr, reqVO.getDcJobGradeDescr())
                .eqIfPresent(SyncPositionDO::getDcJobStage, reqVO.getDcJobStage())
                .eqIfPresent(SyncPositionDO::getDcJobStageDescr, reqVO.getDcJobStageDescr())
                .eqIfPresent(SyncPositionDO::getReportsTo, reqVO.getReportsTo())
                .eqIfPresent(SyncPositionDO::getReportsToDescr, reqVO.getReportsToDescr())
                .eqIfPresent(SyncPositionDO::getDcPositionDescra, reqVO.getDcPositionDescra())
                .orderByDesc(SyncPositionDO::getId));
    }

}