package cn.iocoder.sva.module.system.service.sync;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;
import cn.iocoder.sva.module.system.controller.admin.sync.vo.SyncStaffPageReqVO;
import cn.iocoder.sva.module.system.controller.admin.sync.vo.SyncStaffSaveReqVO;
import cn.iocoder.sva.module.system.dal.dataobject.sync.SyncStaffDO;
import cn.iocoder.sva.module.system.dal.mysql.sync.SyncStaffMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.sva.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.sva.module.system.enums.ErrorCodeConstants.SYNC_STAFF_NOT_EXISTS;

/**
 * 全部人员信息同步 Service 实现类
 *
 * @author like
 */
@Service
@Validated
public class SyncStaffServiceImpl implements SyncStaffService {

    @Resource
    private SyncStaffMapper syncStaffMapper;

    @Override
    public Long createSyncStaff(SyncStaffSaveReqVO createReqVO) {
        // 插入
        SyncStaffDO syncStaff = BeanUtils.toBean(createReqVO, SyncStaffDO.class);
        syncStaffMapper.insert(syncStaff);

        // 返回
        return syncStaff.getId();
    }

    @Override
    public void updateSyncStaff(SyncStaffSaveReqVO updateReqVO) {
        // 校验存在
        validateSyncStaffExists(updateReqVO.getId());
        // 更新
        SyncStaffDO updateObj = BeanUtils.toBean(updateReqVO, SyncStaffDO.class);
        syncStaffMapper.updateById(updateObj);
    }

    @Override
    public void deleteSyncStaff(Long id) {
        // 校验存在
        validateSyncStaffExists(id);
        // 删除
        syncStaffMapper.deleteById(id);
    }

    @Override
        public void deleteSyncStaffListByIds(List<Long> ids) {
        // 删除
        syncStaffMapper.deleteByIds(ids);
        }


    private void validateSyncStaffExists(Long id) {
        if (syncStaffMapper.selectById(id) == null) {
            throw exception(SYNC_STAFF_NOT_EXISTS);
        }
    }

    @Override
    public SyncStaffDO getSyncStaff(Long id) {
        return syncStaffMapper.selectById(id);
    }

    @Override
    public PageResult<SyncStaffDO> getSyncStaffPage(SyncStaffPageReqVO pageReqVO) {
        return syncStaffMapper.selectPage(pageReqVO);
    }

}