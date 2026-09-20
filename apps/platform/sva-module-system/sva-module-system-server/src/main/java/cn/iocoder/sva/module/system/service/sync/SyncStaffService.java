package cn.iocoder.sva.module.system.service.sync;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.module.system.controller.admin.sync.vo.SyncStaffPageReqVO;
import cn.iocoder.sva.module.system.controller.admin.sync.vo.SyncStaffSaveReqVO;
import cn.iocoder.sva.module.system.dal.dataobject.sync.SyncStaffDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 全部人员信息同步 Service 接口
 *
 * @author like
 */
public interface SyncStaffService {

    /**
     * 创建全部人员信息同步
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createSyncStaff(@Valid SyncStaffSaveReqVO createReqVO);

    /**
     * 更新全部人员信息同步
     *
     * @param updateReqVO 更新信息
     */
    void updateSyncStaff(@Valid SyncStaffSaveReqVO updateReqVO);

    /**
     * 删除全部人员信息同步
     *
     * @param id 编号
     */
    void deleteSyncStaff(Long id);

    /**
    * 批量删除全部人员信息同步
    *
    * @param ids 编号
    */
    void deleteSyncStaffListByIds(List<Long> ids);

    /**
     * 获得全部人员信息同步
     *
     * @param id 编号
     * @return 全部人员信息同步
     */
    SyncStaffDO getSyncStaff(Long id);

    /**
     * 获得全部人员信息同步分页
     *
     * @param pageReqVO 分页查询
     * @return 全部人员信息同步分页
     */
    PageResult<SyncStaffDO> getSyncStaffPage(SyncStaffPageReqVO pageReqVO);

}