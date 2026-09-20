package cn.iocoder.sva.module.system.controller.admin.permission;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.sva.framework.common.enums.CommonStatusEnum;
import cn.iocoder.sva.framework.common.exception.ErrorCode;
import cn.iocoder.sva.framework.common.pojo.CommonResult;
import cn.iocoder.sva.module.system.controller.admin.permission.vo.permission.PermissionAssignRoleDataScopeReqVO;
import cn.iocoder.sva.module.system.controller.admin.permission.vo.permission.PermissionAssignRoleMenuReqVO;
import cn.iocoder.sva.module.system.controller.admin.permission.vo.permission.PermissionAssignUserRoleReqVO;
import cn.iocoder.sva.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.sva.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.sva.module.system.service.permission.PermissionService;
import cn.iocoder.sva.module.system.service.permission.RoleService;
import cn.iocoder.sva.module.system.service.tenant.TenantService;
import cn.iocoder.sva.module.system.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Set;

import static cn.iocoder.sva.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.sva.framework.common.pojo.CommonResult.success;
import static cn.iocoder.sva.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 权限 Controller，提供赋予用户、角色的权限的 API 接口
 *
 * @author 科兴源码
 */
@Tag(name = "管理后台 - 权限")
@RestController
@RequestMapping("/system/permission")
public class PermissionController {

    @Resource
    private PermissionService permissionService;
    @Resource
    private TenantService tenantService;

    @Resource
    private RoleService roleService;

    @Resource
    private AdminUserService userService;

    @Operation(summary = "获得角色拥有的菜单编号")
    @Parameter(name = "roleId", description = "角色编号", required = true)
    @GetMapping("/list-role-menus")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-role-menu')")
    public CommonResult<Set<Long>> getRoleMenuList(Long roleId) {
        return success(permissionService.getRoleMenuListByRoleId(roleId));
    }

    @PostMapping("/assign-role-menu")
    @Operation(summary = "赋予角色菜单")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-role-menu')")
    public CommonResult<Boolean> assignRoleMenu(@Validated @RequestBody PermissionAssignRoleMenuReqVO reqVO) {
        tenantService.handleTenantMenu(menuIds -> reqVO.getMenuIds().removeIf(menuId -> !CollUtil.contains(menuIds, menuId)));

        permissionService.assignRoleMenu(reqVO.getRoleId(), reqVO.getMenuIds());
        return success(true);
    }

    @PostMapping("/assign-role-data-scope")
    @Operation(summary = "赋予角色数据权限")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-role-data-scope')")
    public CommonResult<Boolean> assignRoleDataScope(@Valid @RequestBody PermissionAssignRoleDataScopeReqVO reqVO) {
        if(!superAdmin() && reqVO.getDataScope() == 1){
            RoleDO role = roleService.getRole(reqVO.getRoleId());
            Long creatorRole = role.getCreatorRole();
            if(creatorRole != null){
                RoleDO rolePar = roleService.getRole(creatorRole);
                if(rolePar != null){
                    reqVO.setDataScope(rolePar.getDataScope());
                    reqVO.setDataScopeDeptIds(rolePar.getDataScopeDeptIds());
                }
            }
        }

        permissionService.assignRoleDataScope(reqVO.getRoleId(), reqVO.getDataScope(), reqVO.getDataScopeDeptIds());
        return success(true);
    }

    @PostMapping("/assign-role-org-scope")
    @Operation(summary = "赋予组织机构角色权限")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-role-org')")
    public CommonResult<Boolean> assignRoleDeptScope(@Valid @RequestBody PermissionAssignRoleDataScopeReqVO reqVO) {

        permissionService.assignRoleDeptScope(reqVO.getRoleId(), reqVO.getDataScope(), reqVO.getDataScopeDeptIds());
        return success(true);
    }

    @Operation(summary = "获得管理员拥有的角色编号列表")
    @Parameter(name = "userId", description = "用户编号", required = true)
    @GetMapping("/list-user-roles")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-user-role')")
    public CommonResult<Set<Long>> listAdminRoles(@RequestParam("userId") Long userId) {
        return success(permissionService.getUserRoleIdListByUserId(userId));
    }

    @Operation(summary = "赋予用户角色")
    @PostMapping("/assign-user-role")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-user-role')")
    public CommonResult<Boolean> assignUserRole(@Validated @RequestBody PermissionAssignUserRoleReqVO reqVO) {
        permissionService.assignUserRole(reqVO.getUserId(), reqVO.getRoleIds());
        return success(true);
    }

    @GetMapping("/get-user-role-ids")
    @Operation(summary = "获得用户拥有的角色编号集合")
    @Parameter(name = "userId", description = "用户编号", example = "1", required = true)
    public CommonResult<Set<Long>> getUserRoleIds(@RequestParam("userId") Long userId) {
        return success(permissionService.getUserRoleIdListByUserId(userId));
    }

    private Boolean superAdmin(){

        AdminUserDO user = userService.getUser(getLoginUserId());
        if (user == null) {
            throw exception(new ErrorCode(10001,"获取用户信息失败"));
        }

        Set<Long> roleIds = permissionService.getUserRoleIdListByUserId(getLoginUserId());
        if (CollUtil.isEmpty(roleIds)) {
            throw exception(new ErrorCode(10002,"获取用户角色失败"));
        }
        List<RoleDO> roles = roleService.getRoleList(roleIds);
        roles.removeIf(role -> !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus()));

        for (RoleDO role : roles) {
            if("super_admin".equals(role.getCode())){
                return true;
            }
        }

        return false;
    }

    @Operation(summary = "查询拥有当前角色的部门列表")
    @Parameter(name = "roleId", description = "角色id", required = true)
    @GetMapping("/get-role-org-scope")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-role-org')")
    public CommonResult<Set<Long>> listRoleDepts(@RequestParam("roleId") Long roleId) {
        return success(permissionService.getDeptIdListByRoleId(roleId));
    }

}
