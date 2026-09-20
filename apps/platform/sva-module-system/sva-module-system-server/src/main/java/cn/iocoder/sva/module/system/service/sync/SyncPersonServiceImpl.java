package cn.iocoder.sva.module.system.service.sync;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import cn.iocoder.sva.module.system.controller.admin.sync.vo.*;
import cn.iocoder.sva.module.system.dal.dataobject.sync.SyncPersonDO;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.pojo.PageParam;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;

import cn.iocoder.sva.module.system.dal.mysql.sync.SyncPersonMapper;

import static cn.iocoder.sva.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.sva.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.sva.framework.common.util.collection.CollectionUtils.diffList;
import static cn.iocoder.sva.module.system.enums.ErrorCodeConstants.*;

/**
 * 人员信息同步 Service 实现类
 *
 * @author 李可
 */
@Service
@Validated
public class SyncPersonServiceImpl implements SyncPersonService {

    @Resource
    private SyncPersonMapper syncPersonMapper;

    @Override
    public Long createSyncPerson(SyncPersonSaveReqVO createReqVO) {
        // 插入
        SyncPersonDO syncPerson = BeanUtils.toBean(createReqVO, SyncPersonDO.class);
        syncPersonMapper.insert(syncPerson);

        // 返回
        return syncPerson.getId();
    }

    @Override
    public void updateSyncPerson(SyncPersonSaveReqVO updateReqVO) {
        // 校验存在
        validateSyncPersonExists(updateReqVO.getId());
        // 更新
        SyncPersonDO updateObj = BeanUtils.toBean(updateReqVO, SyncPersonDO.class);
        syncPersonMapper.updateById(updateObj);
    }

    @Override
    public void deleteSyncPerson(Long id) {
        // 校验存在
        validateSyncPersonExists(id);
        // 删除
        syncPersonMapper.deleteById(id);
    }

    @Override
        public void deleteSyncPersonListByIds(List<Long> ids) {
        // 删除
        syncPersonMapper.deleteByIds(ids);
        }


    private void validateSyncPersonExists(Long id) {
        if (syncPersonMapper.selectById(id) == null) {
            throw exception(SYNC_PERSON_NOT_EXISTS);
        }
    }

    @Override
    public SyncPersonDO getSyncPerson(Long id) {
        return syncPersonMapper.selectById(id);
    }

    @Override
    public PageResult<SyncPersonDO> getSyncPersonPage(SyncPersonPageReqVO pageReqVO) {
        return syncPersonMapper.selectPage(pageReqVO);
    }

}