package cn.iocoder.sva.module.system.service.sync;

import java.util.*;
import jakarta.validation.*;
import cn.iocoder.sva.module.system.controller.admin.sync.vo.*;
import cn.iocoder.sva.module.system.dal.dataobject.sync.SyncPositionDO;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.pojo.PageParam;

/**
 * 岗位信息同步 Service 接口
 *
 * @author 李可
 */
public interface SyncPositionService {

    /**
     * 创建岗位信息同步
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createSyncPosition(@Valid SyncPositionSaveReqVO createReqVO);

    /**
     * 更新岗位信息同步
     *
     * @param updateReqVO 更新信息
     */
    void updateSyncPosition(@Valid SyncPositionSaveReqVO updateReqVO);

    /**
     * 删除岗位信息同步
     *
     * @param id 编号
     */
    void deleteSyncPosition(Long id);

    /**
    * 批量删除岗位信息同步
    *
    * @param ids 编号
    */
    void deleteSyncPositionListByIds(List<Long> ids);

    /**
     * 获得岗位信息同步
     *
     * @param id 编号
     * @return 岗位信息同步
     */
    SyncPositionDO getSyncPosition(Long id);

    /**
     * 获得岗位信息同步分页
     *
     * @param pageReqVO 分页查询
     * @return 岗位信息同步分页
     */
    PageResult<SyncPositionDO> getSyncPositionPage(SyncPositionPageReqVO pageReqVO);

}