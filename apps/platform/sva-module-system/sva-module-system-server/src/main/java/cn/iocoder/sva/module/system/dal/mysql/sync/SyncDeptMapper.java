package cn.iocoder.sva.module.system.dal.mysql.sync;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.sva.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.sva.module.system.dal.dataobject.sync.SyncDeptDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.sva.module.system.controller.admin.sync.vo.*;

/**
 * 部门信息同步 Mapper
 *
 * @author 李可
 */
@Mapper
public interface SyncDeptMapper extends BaseMapperX<SyncDeptDO> {

    default PageResult<SyncDeptDO> selectPage(SyncDeptPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SyncDeptDO>()
                .eqIfPresent(SyncDeptDO::getGuid, reqVO.getGuid())
                .eqIfPresent(SyncDeptDO::getDcInfDtStatus, reqVO.getDcInfDtStatus())
                .eqIfPresent(SyncDeptDO::getSetid, reqVO.getSetid())
                .eqIfPresent(SyncDeptDO::getDeptid, reqVO.getDeptid())
                .eqIfPresent(SyncDeptDO::getEffdt, reqVO.getEffdt())
                .eqIfPresent(SyncDeptDO::getEffStatus, reqVO.getEffStatus())
                .eqIfPresent(SyncDeptDO::getEffStatusDescr, reqVO.getEffStatusDescr())
                .eqIfPresent(SyncDeptDO::getDescr, reqVO.getDescr())
                .eqIfPresent(SyncDeptDO::getDescrshort, reqVO.getDescrshort())
                .eqIfPresent(SyncDeptDO::getSetidLocation, reqVO.getSetidLocation())
                .eqIfPresent(SyncDeptDO::getLocation, reqVO.getLocation())
                .eqIfPresent(SyncDeptDO::getDcLocationDescr, reqVO.getDcLocationDescr())
                .eqIfPresent(SyncDeptDO::getCompany, reqVO.getCompany())
                .eqIfPresent(SyncDeptDO::getDcCompanyDescr, reqVO.getDcCompanyDescr())
                .eqIfPresent(SyncDeptDO::getDcOrgType, reqVO.getDcOrgType())
                .eqIfPresent(SyncDeptDO::getDcOrgTypeDescr, reqVO.getDcOrgTypeDescr())
                .eqIfPresent(SyncDeptDO::getDcOrgLevel, reqVO.getDcOrgLevel())
                .eqIfPresent(SyncDeptDO::getDcOrgLevelDescr, reqVO.getDcOrgLevelDescr())
                .eqIfPresent(SyncDeptDO::getPartDeptidChn, reqVO.getPartDeptidChn())
                .eqIfPresent(SyncDeptDO::getDcParDeptDescr, reqVO.getDcParDeptDescr())
                .eqIfPresent(SyncDeptDO::getManagerPosn, reqVO.getManagerPosn())
                .eqIfPresent(SyncDeptDO::getDcDirectorPosn, reqVO.getDcDirectorPosn())
                .eqIfPresent(SyncDeptDO::getDcManagerPosn, reqVO.getDcManagerPosn())
                .betweenIfPresent(SyncDeptDO::getDcSetupDate, reqVO.getDcSetupDate())
                .eqIfPresent(SyncDeptDO::getDcSetupNum, reqVO.getDcSetupNum())
                .eqIfPresent(SyncDeptDO::getDcCostCenter, reqVO.getDcCostCenter())
                .eqIfPresent(SyncDeptDO::getDcOrgBranch, reqVO.getDcOrgBranch())
                .eqIfPresent(SyncDeptDO::getDcOrgBranchDescr, reqVO.getDcOrgBranchDescr())
                .eqIfPresent(SyncDeptDO::getDcSetupReason, reqVO.getDcSetupReason())
                .eqIfPresent(SyncDeptDO::getDcDeptRespon, reqVO.getDcDeptRespon())
                .eqIfPresent(SyncDeptDO::getDcDeptFullCode, reqVO.getDcDeptFullCode())
                .eqIfPresent(SyncDeptDO::getDcDeptFullDescr, reqVO.getDcDeptFullDescr())
                .eqIfPresent(SyncDeptDO::getDcHonghaiDeptid, reqVO.getDcHonghaiDeptid())
                .eqIfPresent(SyncDeptDO::getDcSapCompanyid, reqVO.getDcSapCompanyid())
                .eqIfPresent(SyncDeptDO::getDcDeptidLv01, reqVO.getDcDeptidLv01())
                .eqIfPresent(SyncDeptDO::getDcDeptidLv02, reqVO.getDcDeptidLv02())
                .eqIfPresent(SyncDeptDO::getDcDeptidLv03, reqVO.getDcDeptidLv03())
                .orderByDesc(SyncDeptDO::getId));
    }

}