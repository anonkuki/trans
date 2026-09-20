package cn.iocoder.sva.module.system.dal.mysql.sync;

import java.util.*;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.sva.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.sva.module.system.dal.dataobject.sync.SyncStaffDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.sva.module.system.controller.admin.sync.vo.*;

/**
 * 全部人员信息同步 Mapper
 *
 * @author like
 */
@Mapper
public interface SyncStaffMapper extends BaseMapperX<SyncStaffDO> {

    default PageResult<SyncStaffDO> selectPage(SyncStaffPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SyncStaffDO>()
                .eqIfPresent(SyncStaffDO::getGuid, reqVO.getGuid())
                .eqIfPresent(SyncStaffDO::getDcInfDtStatus, reqVO.getDcInfDtStatus())
                .eqIfPresent(SyncStaffDO::getDcInfDtStatusDescr, reqVO.getDcInfDtStatusDescr())
                .eqIfPresent(SyncStaffDO::getEmplid, reqVO.getEmplid())
                .eqIfPresent(SyncStaffDO::getNameDisplay, reqVO.getNameDisplay())
                .eqIfPresent(SyncStaffDO::getSex, reqVO.getSex())
                .eqIfPresent(SyncStaffDO::getSexDescr, reqVO.getSexDescr())
                .eqIfPresent(SyncStaffDO::getEmailAddr, reqVO.getEmailAddr())
                .eqIfPresent(SyncStaffDO::getPhone, reqVO.getPhone())
                .eqIfPresent(SyncStaffDO::getEmplClass, reqVO.getEmplClass())
                .eqIfPresent(SyncStaffDO::getDcEmplClsDescr, reqVO.getDcEmplClsDescr())
                .eqIfPresent(SyncStaffDO::getHrStatus, reqVO.getHrStatus())
                .eqIfPresent(SyncStaffDO::getHrStatusDescr, reqVO.getHrStatusDescr())
                .eqIfPresent(SyncStaffDO::getRegTemp, reqVO.getRegTemp())
                .eqIfPresent(SyncStaffDO::getRegTempDescr, reqVO.getRegTempDescr())
                .eqIfPresent(SyncStaffDO::getReportsTo, reqVO.getReportsTo())
                .eqIfPresent(SyncStaffDO::getPositionNbr, reqVO.getPositionNbr())
                .eqIfPresent(SyncStaffDO::getDcPositionDescr, reqVO.getDcPositionDescr())
                .eqIfPresent(SyncStaffDO::getRegRegion, reqVO.getRegRegion())
                .eqIfPresent(SyncStaffDO::getCompany, reqVO.getCompany())
                .eqIfPresent(SyncStaffDO::getDcCompanyDescr, reqVO.getDcCompanyDescr())
                .eqIfPresent(SyncStaffDO::getBusinessUnit, reqVO.getBusinessUnit())
                .eqIfPresent(SyncStaffDO::getBusinessDescr, reqVO.getBusinessDescr())
                .eqIfPresent(SyncStaffDO::getDeptid, reqVO.getDeptid())
                .eqIfPresent(SyncStaffDO::getDcDeptDescr50, reqVO.getDcDeptDescr50())
                .eqIfPresent(SyncStaffDO::getManagerPosn, reqVO.getManagerPosn())
                .eqIfPresent(SyncStaffDO::getDcDirectorPosn, reqVO.getDcDirectorPosn())
                .eqIfPresent(SyncStaffDO::getDcManagerPosn, reqVO.getDcManagerPosn())
                .eqIfPresent(SyncStaffDO::getDcCostCenter, reqVO.getDcCostCenter())
                .eqIfPresent(SyncStaffDO::getProbationDt, reqVO.getProbationDt())
                .eqIfPresent(SyncStaffDO::getLocation, reqVO.getLocation())
                .eqIfPresent(SyncStaffDO::getDcLocationDescr, reqVO.getDcLocationDescr())
                .eqIfPresent(SyncStaffDO::getDcPdhFellowYn, reqVO.getDcPdhFellowYn())
                .eqIfPresent(SyncStaffDO::getDcPdhFellowYnDescr, reqVO.getDcPdhFellowYnDescr())
                .eqIfPresent(SyncStaffDO::getDcDisabledYn, reqVO.getDcDisabledYn())
                .eqIfPresent(SyncStaffDO::getDcDisabledYnDescr, reqVO.getDcDisabledYnDescr())
                .eqIfPresent(SyncStaffDO::getLastHireDt, reqVO.getLastHireDt())
                .eqIfPresent(SyncStaffDO::getDcInternHireDt, reqVO.getDcInternHireDt())
                .eqIfPresent(SyncStaffDO::getJobcode, reqVO.getJobcode())
                .eqIfPresent(SyncStaffDO::getDcJobcodeDescr, reqVO.getDcJobcodeDescr())
                .eqIfPresent(SyncStaffDO::getPositionEntryDt, reqVO.getPositionEntryDt())
                .eqIfPresent(SyncStaffDO::getDcJobGroup, reqVO.getDcJobGroup())
                .eqIfPresent(SyncStaffDO::getDcJobGroupDescr, reqVO.getDcJobGroupDescr())
                .eqIfPresent(SyncStaffDO::getDcJobSequence, reqVO.getDcJobSequence())
                .eqIfPresent(SyncStaffDO::getDcJobSeqDescr, reqVO.getDcJobSeqDescr())
                .eqIfPresent(SyncStaffDO::getDcFirstJobCate, reqVO.getDcFirstJobCate())
                .eqIfPresent(SyncStaffDO::getDcJobcateDescr, reqVO.getDcJobcateDescr())
                .eqIfPresent(SyncStaffDO::getDcJobStage, reqVO.getDcJobStage())
                .eqIfPresent(SyncStaffDO::getDcJobStageDescr, reqVO.getDcJobStageDescr())
                .eqIfPresent(SyncStaffDO::getDcJobLevel, reqVO.getDcJobLevel())
                .eqIfPresent(SyncStaffDO::getDcJobLevelDescr, reqVO.getDcJobLevelDescr())
                .eqIfPresent(SyncStaffDO::getDcJobGrade, reqVO.getDcJobGrade())
                .eqIfPresent(SyncStaffDO::getDcJobGradeDescr, reqVO.getDcJobGradeDescr())
                .eqIfPresent(SyncStaffDO::getDcSapCompanyid, reqVO.getDcSapCompanyid())
                .eqIfPresent(SyncStaffDO::getDcEmpLevel, reqVO.getDcEmpLevel())
                .eqIfPresent(SyncStaffDO::getDcEmplStage, reqVO.getDcEmplStage())
                .eqIfPresent(SyncStaffDO::getDcEmplStageDescr, reqVO.getDcEmplStageDescr())
                .eqIfPresent(SyncStaffDO::getNationalIdType, reqVO.getNationalIdType())
                .eqIfPresent(SyncStaffDO::getNationalId, reqVO.getNationalId())
                .eqIfPresent(SyncStaffDO::getEmplRcd, reqVO.getEmplRcd())
                .eqIfPresent(SyncStaffDO::getEffdt, reqVO.getEffdt())
                .eqIfPresent(SyncStaffDO::getEffseq, reqVO.getEffseq())
                .eqIfPresent(SyncStaffDO::getAction, reqVO.getAction())
                .eqIfPresent(SyncStaffDO::getActionDescr, reqVO.getActionDescr())
                .eqIfPresent(SyncStaffDO::getActionReason, reqVO.getActionReason())
                .eqIfPresent(SyncStaffDO::getActionReasnDescr, reqVO.getActionReasnDescr())
                .likeIfPresent(SyncStaffDO::getFirstName, reqVO.getFirstName())
                .likeIfPresent(SyncStaffDO::getLastName, reqVO.getLastName())
                .orderByDesc(SyncStaffDO::getId));
    }

}