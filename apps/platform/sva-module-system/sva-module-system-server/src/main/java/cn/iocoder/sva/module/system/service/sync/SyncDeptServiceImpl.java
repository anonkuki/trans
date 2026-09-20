package cn.iocoder.sva.module.system.service.sync;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;
import cn.iocoder.sva.module.system.controller.admin.sync.vo.SyncDeptPageReqVO;
import cn.iocoder.sva.module.system.controller.admin.sync.vo.SyncDeptSaveReqVO;
import cn.iocoder.sva.module.system.dal.dataobject.sync.SyncDeptDO;
import cn.iocoder.sva.module.system.dal.mysql.sync.SyncDeptMapper;
import cn.iocoder.sva.module.system.enums.ErrorCodeConstants;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.sva.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * 部门信息同步 Service 实现类
 *
 * @author 李可
 */
@Service
@Validated
public class SyncDeptServiceImpl implements SyncDeptService {

    @Resource
    private SyncDeptMapper syncDeptMapper;

    @Override
    public Long createSyncDept(SyncDeptSaveReqVO createReqVO) {
        // 插入
        SyncDeptDO syncDept = BeanUtils.toBean(createReqVO, SyncDeptDO.class);
        syncDeptMapper.insert(syncDept);

        // 返回
        return syncDept.getId();
    }

    @Override
    public void updateSyncDept(SyncDeptSaveReqVO updateReqVO) {
        // 校验存在
        validateSyncDeptExists(updateReqVO.getId());
        // 更新
        SyncDeptDO updateObj = BeanUtils.toBean(updateReqVO, SyncDeptDO.class);
        syncDeptMapper.updateById(updateObj);
    }

    @Override
    public void deleteSyncDept(Long id) {
        // 校验存在
        validateSyncDeptExists(id);
        // 删除
        syncDeptMapper.deleteById(id);
    }

    @Override
        public void deleteSyncDeptListByIds(List<Long> ids) {
        // 删除
        syncDeptMapper.deleteByIds(ids);
        }


    private void validateSyncDeptExists(Long id) {
        if (syncDeptMapper.selectById(id) == null) {
            throw exception(ErrorCodeConstants.SYNC_DEPT_NOT_EXISTS);
        }
    }

    @Override
    public SyncDeptDO getSyncDept(Long id) {
        return syncDeptMapper.selectById(id);
    }

    @Override
    public PageResult<SyncDeptDO> getSyncDeptPage(SyncDeptPageReqVO pageReqVO) {
        return syncDeptMapper.selectPage(pageReqVO);
    }

}