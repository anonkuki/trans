package cn.iocoder.sva.module.system.service.role;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;
import cn.iocoder.sva.module.system.controller.admin.role.vo.DeptRolePageReqVO;
import cn.iocoder.sva.module.system.controller.admin.role.vo.DeptRoleSaveReqVO;
import cn.iocoder.sva.module.system.dal.dataobject.role.DeptRoleDO;
import cn.iocoder.sva.module.system.dal.mysql.role.DeptRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.sva.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.sva.module.system.enums.ErrorCodeConstants.DEPT_ROLE_NOT_EXISTS;

/**
 * 部门和角色关联 Service 实现类
 *
 * @author like
 */
@Service
@Validated
public class DeptRoleServiceImpl implements DeptRoleService {

    @Resource
    private DeptRoleMapper deptRoleMapper;

    @Override
    public Long createDeptRole(DeptRoleSaveReqVO createReqVO) {
        // 插入
        DeptRoleDO deptRole = BeanUtils.toBean(createReqVO, DeptRoleDO.class);
        deptRoleMapper.insert(deptRole);

        // 返回
        return deptRole.getId();
    }

    @Override
    public void updateDeptRole(DeptRoleSaveReqVO updateReqVO) {
        // 校验存在
        validateDeptRoleExists(updateReqVO.getId());
        // 更新
        DeptRoleDO updateObj = BeanUtils.toBean(updateReqVO, DeptRoleDO.class);
        deptRoleMapper.updateById(updateObj);
    }

    @Override
    public void deleteDeptRole(Long id) {
        // 校验存在
        validateDeptRoleExists(id);
        // 删除
        deptRoleMapper.deleteById(id);
    }

    @Override
        public void deleteDeptRoleListByIds(List<Long> ids) {
        // 删除
        deptRoleMapper.deleteByIds(ids);
        }


    private void validateDeptRoleExists(Long id) {
        if (deptRoleMapper.selectById(id) == null) {
            throw exception(DEPT_ROLE_NOT_EXISTS);
        }
    }

    @Override
    public DeptRoleDO getDeptRole(Long id) {
        return deptRoleMapper.selectById(id);
    }

    @Override
    public PageResult<DeptRoleDO> getDeptRolePage(DeptRolePageReqVO pageReqVO) {
        return deptRoleMapper.selectPage(pageReqVO);
    }

    @Override
    public List<DeptRoleDO> getDeptRoleListByDeptId(Long deptId) {
        return deptRoleMapper.selectList(new QueryWrapper<DeptRoleDO>().eq("dept_id", deptId));
    }


}