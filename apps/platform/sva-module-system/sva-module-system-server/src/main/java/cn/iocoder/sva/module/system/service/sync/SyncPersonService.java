package cn.iocoder.sva.module.system.service.sync;

import java.util.*;
import jakarta.validation.*;
import cn.iocoder.sva.module.system.controller.admin.sync.vo.*;
import cn.iocoder.sva.module.system.dal.dataobject.sync.SyncPersonDO;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.pojo.PageParam;

/**
 * 人员信息同步 Service 接口
 *
 * @author 李可
 */
public interface SyncPersonService {

    /**
     * 创建人员信息同步
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createSyncPerson(@Valid SyncPersonSaveReqVO createReqVO);

    /**
     * 更新人员信息同步
     *
     * @param updateReqVO 更新信息
     */
    void updateSyncPerson(@Valid SyncPersonSaveReqVO updateReqVO);

    /**
     * 删除人员信息同步
     *
     * @param id 编号
     */
    void deleteSyncPerson(Long id);

    /**
    * 批量删除人员信息同步
    *
    * @param ids 编号
    */
    void deleteSyncPersonListByIds(List<Long> ids);

    /**
     * 获得人员信息同步
     *
     * @param id 编号
     * @return 人员信息同步
     */
    SyncPersonDO getSyncPerson(Long id);

    /**
     * 获得人员信息同步分页
     *
     * @param pageReqVO 分页查询
     * @return 人员信息同步分页
     */
    PageResult<SyncPersonDO> getSyncPersonPage(SyncPersonPageReqVO pageReqVO);

}