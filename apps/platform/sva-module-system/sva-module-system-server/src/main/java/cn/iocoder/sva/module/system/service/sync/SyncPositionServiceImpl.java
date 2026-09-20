package cn.iocoder.sva.module.system.service.sync;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import cn.iocoder.sva.module.system.controller.admin.sync.vo.*;
import cn.iocoder.sva.module.system.dal.dataobject.sync.SyncPositionDO;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.pojo.PageParam;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;

import cn.iocoder.sva.module.system.dal.mysql.sync.SyncPositionMapper;

import static cn.iocoder.sva.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.sva.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.sva.framework.common.util.collection.CollectionUtils.diffList;
import static cn.iocoder.sva.module.system.enums.ErrorCodeConstants.*;

/**
 * 岗位信息同步 Service 实现类
 *
 * @author 李可
 */
@Service
@Validated
public class SyncPositionServiceImpl implements SyncPositionService {

    @Resource
    private SyncPositionMapper syncPositionMapper;

    @Override
    public Long createSyncPosition(SyncPositionSaveReqVO createReqVO) {
        // 插入
        SyncPositionDO syncPosition = BeanUtils.toBean(createReqVO, SyncPositionDO.class);
        syncPositionMapper.insert(syncPosition);

        // 返回
        return syncPosition.getId();
    }

    @Override
    public void updateSyncPosition(SyncPositionSaveReqVO updateReqVO) {
        // 校验存在
        validateSyncPositionExists(updateReqVO.getId());
        // 更新
        SyncPositionDO updateObj = BeanUtils.toBean(updateReqVO, SyncPositionDO.class);
        syncPositionMapper.updateById(updateObj);
    }

    @Override
    public void deleteSyncPosition(Long id) {
        // 校验存在
        validateSyncPositionExists(id);
        // 删除
        syncPositionMapper.deleteById(id);
    }

    @Override
        public void deleteSyncPositionListByIds(List<Long> ids) {
        // 删除
        syncPositionMapper.deleteByIds(ids);
        }


    private void validateSyncPositionExists(Long id) {
        if (syncPositionMapper.selectById(id) == null) {
            throw exception(SYNC_POSITION_NOT_EXISTS);
        }
    }

    @Override
    public SyncPositionDO getSyncPosition(Long id) {
        return syncPositionMapper.selectById(id);
    }

    @Override
    public PageResult<SyncPositionDO> getSyncPositionPage(SyncPositionPageReqVO pageReqVO) {
        return syncPositionMapper.selectPage(pageReqVO);
    }

}