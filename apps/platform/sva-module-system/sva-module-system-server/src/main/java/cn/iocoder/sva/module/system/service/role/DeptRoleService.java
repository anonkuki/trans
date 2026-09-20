package cn.iocoder.sva.module.system.service.role;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.module.system.controller.admin.role.vo.DeptRolePageReqVO;
import cn.iocoder.sva.module.system.controller.admin.role.vo.DeptRoleSaveReqVO;
import cn.iocoder.sva.module.system.dal.dataobject.role.DeptRoleDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 部门和角色关联 Service 接口
 *
 * @author like
 */
public interface DeptRoleService {

    /**
     * 创建部门和角色关联
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createDeptRole(@Valid DeptRoleSaveReqVO createReqVO);

    /**
     * 更新部门和角色关联
     *
     * @param updateReqVO 更新信息
     */
    void updateDeptRole(@Valid DeptRoleSaveReqVO updateReqVO);

    /**
     * 删除部门和角色关联
     *
     * @param id 编号
     */
    void deleteDeptRole(Long id);

    /**
    * 批量删除部门和角色关联
    *
    * @param ids 编号
    */
    void deleteDeptRoleListByIds(List<Long> ids);

    /**
     * 获得部门和角色关联
     *
     * @param id 编号
     * @return 部门和角色关联
     */
    DeptRoleDO getDeptRole(Long id);

    /**
     * 获得部门和角色关联分页
     *
     * @param pageReqVO 分页查询
     * @return 部门和角色关联分页
     */
    PageResult<DeptRoleDO> getDeptRolePage(DeptRolePageReqVO pageReqVO);

    /**
     * 获得部门下所有角色
     *
     * @param deptId 部门编号
     * @return 角色列表
     */
    List<DeptRoleDO> getDeptRoleListByDeptId(Long deptId);

}