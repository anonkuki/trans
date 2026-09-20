package cn.iocoder.sva.module.system.service.sync;

import java.util.*;
import jakarta.validation.*;
import cn.iocoder.sva.module.system.controller.admin.sync.vo.*;
import cn.iocoder.sva.module.system.dal.dataobject.sync.SyncDeptDO;
import cn.iocoder.sva.framework.common.pojo.PageResult;

/**
 * 部门信息同步 Service 接口
 *
 * @author 李可
 */
public interface SyncDeptService {

    /**
     * 创建部门信息同步
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createSyncDept(@Valid SyncDeptSaveReqVO createReqVO);

    /**
     * 更新部门信息同步
     *
     * @param updateReqVO 更新信息
     */
    void updateSyncDept(@Valid SyncDeptSaveReqVO updateReqVO);

    /**
     * 删除部门信息同步
     *
     * @param id 编号
     */
    void deleteSyncDept(Long id);

    /**
    * 批量删除部门信息同步
    *
    * @param ids 编号
    */
    void deleteSyncDeptListByIds(List<Long> ids);

    /**
     * 获得部门信息同步
     *
     * @param id 编号
     * @return 部门信息同步
     */
    SyncDeptDO getSyncDept(Long id);

    /**
     * 获得部门信息同步分页
     *
     * @param pageReqVO 分页查询
     * @return 部门信息同步分页
     */
    PageResult<SyncDeptDO> getSyncDeptPage(SyncDeptPageReqVO pageReqVO);

}