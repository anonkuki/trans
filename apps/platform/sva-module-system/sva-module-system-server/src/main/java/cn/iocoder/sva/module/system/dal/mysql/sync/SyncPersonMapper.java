package cn.iocoder.sva.module.system.dal.mysql.sync;

import java.util.*;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.sva.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.sva.module.system.dal.dataobject.sync.SyncPersonDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.sva.module.system.controller.admin.sync.vo.*;

/**
 * 人员信息同步 Mapper
 *
 * @author 李可
 */
@Mapper
public interface SyncPersonMapper extends BaseMapperX<SyncPersonDO> {

    default PageResult<SyncPersonDO> selectPage(SyncPersonPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SyncPersonDO>()
                .eqIfPresent(SyncPersonDO::getGuid, reqVO.getGuid())
                .eqIfPresent(SyncPersonDO::getDcInfDtStatus, reqVO.getDcInfDtStatus())
                .eqIfPresent(SyncPersonDO::getDcInfDtStatusDescr, reqVO.getDcInfDtStatusDescr())
                .eqIfPresent(SyncPersonDO::getEmplid, reqVO.getEmplid())
                .eqIfPresent(SyncPersonDO::getEmplRcd, reqVO.getEmplRcd())
                .eqIfPresent(SyncPersonDO::getEffdt, reqVO.getEffdt())
                .eqIfPresent(SyncPersonDO::getEffseq, reqVO.getEffseq())
                .eqIfPresent(SyncPersonDO::getEmplClass, reqVO.getEmplClass())
                .eqIfPresent(SyncPersonDO::getDcEmplClsDescr, reqVO.getDcEmplClsDescr())
                .eqIfPresent(SyncPersonDO::getAction, reqVO.getAction())
                .eqIfPresent(SyncPersonDO::getActionDescr, reqVO.getActionDescr())
                .eqIfPresent(SyncPersonDO::getActionReason, reqVO.getActionReason())
                .eqIfPresent(SyncPersonDO::getActionReasnDescr, reqVO.getActionReasnDescr())
                .eqIfPresent(SyncPersonDO::getHrStatus, reqVO.getHrStatus())
                .eqIfPresent(SyncPersonDO::getHrStatusDescr, reqVO.getHrStatusDescr())
                .eqIfPresent(SyncPersonDO::getRegTemp, reqVO.getRegTemp())
                .eqIfPresent(SyncPersonDO::getRegTempDescr, reqVO.getRegTempDescr())
                .eqIfPresent(SyncPersonDO::getReportsTo, reqVO.getReportsTo())
                .eqIfPresent(SyncPersonDO::getJobIndicator, reqVO.getJobIndicator())
                .eqIfPresent(SyncPersonDO::getJobIndicatorDescr, reqVO.getJobIndicatorDescr())
                .eqIfPresent(SyncPersonDO::getPositionNbr, reqVO.getPositionNbr())
                .eqIfPresent(SyncPersonDO::getDcPositionDescr, reqVO.getDcPositionDescr())
                .eqIfPresent(SyncPersonDO::getRegRegion, reqVO.getRegRegion())
                .eqIfPresent(SyncPersonDO::getCompany, reqVO.getCompany())
                .eqIfPresent(SyncPersonDO::getDcCompanyDescr, reqVO.getDcCompanyDescr())
                .eqIfPresent(SyncPersonDO::getBusinessUnit, reqVO.getBusinessUnit())
                .eqIfPresent(SyncPersonDO::getBusinessDescr, reqVO.getBusinessDescr())
                .eqIfPresent(SyncPersonDO::getDeptid, reqVO.getDeptid())
                .eqIfPresent(SyncPersonDO::getDcDeptDescr50, reqVO.getDcDeptDescr50())
                .eqIfPresent(SyncPersonDO::getManagerPosn, reqVO.getManagerPosn())
                .eqIfPresent(SyncPersonDO::getDcDirectorPosn, reqVO.getDcDirectorPosn())
                .eqIfPresent(SyncPersonDO::getDcManagerPosn, reqVO.getDcManagerPosn())
                .eqIfPresent(SyncPersonDO::getDcCostCenter, reqVO.getDcCostCenter())
                .eqIfPresent(SyncPersonDO::getProbationDt, reqVO.getProbationDt())
                .eqIfPresent(SyncPersonDO::getLocation, reqVO.getLocation())
                .eqIfPresent(SyncPersonDO::getDcLocationDescr, reqVO.getDcLocationDescr())
                .eqIfPresent(SyncPersonDO::getDcPdhFellowYn, reqVO.getDcPdhFellowYn())
                .eqIfPresent(SyncPersonDO::getDcPdhFellowYnDescr, reqVO.getDcPdhFellowYnDescr())
                .eqIfPresent(SyncPersonDO::getDcDisabledYn, reqVO.getDcDisabledYn())
                .eqIfPresent(SyncPersonDO::getDcDisabledYnDescr, reqVO.getDcDisabledYnDescr())
                .eqIfPresent(SyncPersonDO::getLastHireDt, reqVO.getLastHireDt())
                .eqIfPresent(SyncPersonDO::getDcInternHireDt, reqVO.getDcInternHireDt())
                .eqIfPresent(SyncPersonDO::getJobcode, reqVO.getJobcode())
                .eqIfPresent(SyncPersonDO::getDcJobcodeDescr, reqVO.getDcJobcodeDescr())
                .eqIfPresent(SyncPersonDO::getPositionEntryDt, reqVO.getPositionEntryDt())
                .eqIfPresent(SyncPersonDO::getDcJobGroup, reqVO.getDcJobGroup())
                .eqIfPresent(SyncPersonDO::getDcJobGroupDescr, reqVO.getDcJobGroupDescr())
                .eqIfPresent(SyncPersonDO::getDcJobSequence, reqVO.getDcJobSequence())
                .eqIfPresent(SyncPersonDO::getDcJobSeqDescr, reqVO.getDcJobSeqDescr())
                .eqIfPresent(SyncPersonDO::getDcFirstJobCate, reqVO.getDcFirstJobCate())
                .eqIfPresent(SyncPersonDO::getDcJobcateDescr, reqVO.getDcJobcateDescr())
                .eqIfPresent(SyncPersonDO::getDcJobStage, reqVO.getDcJobStage())
                .eqIfPresent(SyncPersonDO::getDcJobStageDescr, reqVO.getDcJobStageDescr())
                .eqIfPresent(SyncPersonDO::getDcJobLevel, reqVO.getDcJobLevel())
                .eqIfPresent(SyncPersonDO::getDcJobLevelDescr, reqVO.getDcJobLevelDescr())
                .eqIfPresent(SyncPersonDO::getDcJobGrade, reqVO.getDcJobGrade())
                .eqIfPresent(SyncPersonDO::getDcJobGradeDescr, reqVO.getDcJobGradeDescr())
                .eqIfPresent(SyncPersonDO::getDcSapCompanyid, reqVO.getDcSapCompanyid())
                .eqIfPresent(SyncPersonDO::getNameDisplay, reqVO.getNameDisplay())
                .eqIfPresent(SyncPersonDO::getEmailAddr, reqVO.getEmailAddr())
                .orderByDesc(SyncPersonDO::getId));
    }

}