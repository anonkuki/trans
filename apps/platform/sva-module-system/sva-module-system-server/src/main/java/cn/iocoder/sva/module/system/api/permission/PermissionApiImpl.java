package cn.iocoder.sva.module.system.api.permission;

import cn.iocoder.sva.framework.common.pojo.CommonResult;
import cn.iocoder.sva.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.sva.module.system.service.permission.PermissionService;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.Set;

import static cn.iocoder.sva.framework.common.pojo.CommonResult.success;

@RestController
@Validated
@Primary
public class PermissionApiImpl implements PermissionApi {

    @Resource
    private PermissionService permissionService;

    @Override
    public CommonResult<Set<Long>> getUserRoleIdListByRoleIds(Collection<Long> roleIds) {
        return success(permissionService.getUserRoleIdListByRoleId(roleIds));
    }

    @Override
    public CommonResult<Boolean> hasAnyPermissions(Long userId, String... permissions) {
        return success(permissionService.hasAnyPermissions(userId, permissions));
    }

    @Override
    public CommonResult<Boolean> hasAnyRoles(Long userId, String... roles) {
        return success(permissionService.hasAnyRoles(userId, roles));
    }

    @Override
    public CommonResult<DeptDataPermissionRespDTO> getDeptDataPermission(Long userId) {
        return success(permissionService.getDeptDataPermission(userId));
    }

    @Override
    public CommonResult<Set<Long>> getUserRoleIds(Long userId) {
        return success(permissionService.getUserRoleIdListByUserId(userId));
    }

    @Override
    public CommonResult<Set<Long>> getLoginUserAllRoleIds(Long userId) {
        return success(permissionService.getLoginUserAllRoleIds(userId));
    }

}
