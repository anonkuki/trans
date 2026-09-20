package cn.iocoder.sva.module.system.util;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.sva.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.sva.module.system.service.permission.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Set;

/**
 * 用户角色工具类
 * 用于快速获取当前登录用户的角色信息
 *
 * @author 科兴源码
 */
@Slf4j
@Component
public class UserRoleUtils {

    private static PermissionService permissionService;

    @Resource
    public void setPermissionService(PermissionService permissionService) {
        UserRoleUtils.permissionService = permissionService;
    }

    /**
     * 获取当前登录用户的全部角色ID集合
     * 包含：
     * 1. 用户直接分配的角色（通过 /system/permission/assign-user-role 接口分配）
     * 2. 用户所属部门分配的角色（通过 /system/permission/assign-role-org-scope 接口分配）
     *
     * @return 角色ID集合，如果用户未登录则返回空集合
     */
    public static Set<Long> getCurrentUserAllRoleIds() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            log.warn("[getCurrentUserAllRoleIds][当前用户未登录]");
            return CollUtil.newHashSet();
        }
        return permissionService.getLoginUserAllRoleIds(userId);
    }

    /**
     * 获取指定用户的全部角色ID集合
     * 包含：
     * 1. 用户直接分配的角色（通过 /system/permission/assign-user-role 接口分配）
     * 2. 用户所属部门分配的角色（通过 /system/permission/assign-role-org-scope 接口分配）
     *
     * @param userId 用户ID
     * @return 角色ID集合
     */
    public static Set<Long> getUserAllRoleIds(Long userId) {
        if (userId == null) {
            log.warn("[getUserAllRoleIds][用户ID为空]");
            return CollUtil.newHashSet();
        }
        return permissionService.getLoginUserAllRoleIds(userId);
    }

    /**
     * 判断当前用户是否拥有指定角色
     *
     * @param roleId 角色ID
     * @return 是否拥有该角色
     */
    public static boolean currentUserHasRole(Long roleId) {
        if (roleId == null) {
            return false;
        }
        Set<Long> roleIds = getCurrentUserAllRoleIds();
        return CollUtil.contains(roleIds, roleId);
    }

    /**
     * 判断当前用户是否拥有任意一个指定角色
     *
     * @param roleIds 角色ID集合
     * @return 是否拥有任意一个角色
     */
    public static boolean currentUserHasAnyRole(Set<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return false;
        }
        Set<Long> currentUserRoleIds = getCurrentUserAllRoleIds();
        return CollUtil.containsAny(currentUserRoleIds, roleIds);
    }

    /**
     * 判断指定用户是否拥有指定角色
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 是否拥有该角色
     */
    public static boolean userHasRole(Long userId, Long roleId) {
        if (userId == null || roleId == null) {
            return false;
        }
        Set<Long> roleIds = getUserAllRoleIds(userId);
        return CollUtil.contains(roleIds, roleId);
    }
}
