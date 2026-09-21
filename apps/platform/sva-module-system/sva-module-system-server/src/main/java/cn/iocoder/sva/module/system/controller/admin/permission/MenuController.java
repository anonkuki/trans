package cn.iocoder.sva.module.system.controller.admin.permission;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.sva.framework.common.enums.CommonStatusEnum;
import cn.iocoder.sva.framework.common.exception.ErrorCode;
import cn.iocoder.sva.framework.common.pojo.CommonResult;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;
import cn.iocoder.sva.module.system.controller.admin.permission.vo.menu.MenuListReqVO;
import cn.iocoder.sva.module.system.controller.admin.permission.vo.menu.MenuRespVO;
import cn.iocoder.sva.module.system.controller.admin.permission.vo.menu.MenuSaveVO;
import cn.iocoder.sva.module.system.controller.admin.permission.vo.menu.MenuSimpleRespVO;
import cn.iocoder.sva.module.system.convert.auth.AuthConvert;
import cn.iocoder.sva.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.sva.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.sva.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.sva.module.system.service.permission.MenuService;
import cn.iocoder.sva.module.system.service.permission.PermissionService;
import cn.iocoder.sva.module.system.service.permission.RoleService;
import cn.iocoder.sva.module.system.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static cn.iocoder.sva.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.sva.framework.common.pojo.CommonResult.success;
import static cn.iocoder.sva.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.sva.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 菜单")
@RestController
@RequestMapping("/system/menu")
@Validated
public class MenuController {

    @Resource
    private MenuService menuService;

    @Resource
    private AdminUserService userService;
    @Resource
    private RoleService roleService;
    @Resource
    private PermissionService permissionService;

    @PostMapping("/create")
    @Operation(summary = "创建菜单")
    @PreAuthorize("@ss.hasPermission('system:menu:create')")
    public CommonResult<Long> createMenu(@Valid @RequestBody MenuSaveVO createReqVO) {
        Long menuId = menuService.createMenu(createReqVO);
        return success(menuId);
    }

    @PutMapping("/update")
    @Operation(summary = "修改菜单")
    @PreAuthorize("@ss.hasPermission('system:menu:update')")
    public CommonResult<Boolean> updateMenu(@Valid @RequestBody MenuSaveVO updateReqVO) {
        menuService.updateMenu(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除菜单")
    @Parameter(name = "id", description = "菜单编号", required= true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:menu:delete')")
    public CommonResult<Boolean> deleteMenu(@RequestParam("id") Long id) {
        menuService.deleteMenu(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除菜单")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('system:menu:delete')")
    public CommonResult<Boolean> deleteMenuList(@RequestParam("ids") List<Long> ids) {
        menuService.deleteMenuList(ids);
        return success(true);
    }

    @GetMapping("/list")
    @Operation(summary = "获取菜单列表", description = "用于【菜单管理】界面")
    @PreAuthorize("@ss.hasPermission('system:menu:query')")
    public CommonResult<List<MenuRespVO>> getMenuList(MenuListReqVO reqVO) {
        List<MenuDO> menuList = getMenuListByRole(reqVO);
//        List<MenuDO> list = menuService.getMenuList(reqVO);
        menuList.sort(Comparator.comparing(MenuDO::getSort));
        return success(BeanUtils.toBean(menuList, MenuRespVO.class));
    }

    @GetMapping({"/list-all-simple", "simple-list"})
    @Operation(summary = "获取菜单精简信息列表",
            description = "只包含被开启的菜单，用于【角色分配菜单】功能的选项。在多租户的场景下，会只返回租户所在套餐有的菜单")
    public CommonResult<List<MenuSimpleRespVO>> getSimpleMenuList() {
//        List<MenuDO> list = menuService.getMenuListByTenant(
//                new MenuListReqVO().setStatus(CommonStatusEnum.ENABLE.getStatus()));
        List<MenuDO> list = getMenuListByRole(null);
        list = menuService.filterDisableMenus(list);
        list.sort(Comparator.comparing(MenuDO::getSort));
        return success(BeanUtils.toBean(list, MenuSimpleRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获取菜单信息")
    @PreAuthorize("@ss.hasPermission('system:menu:query')")
    public CommonResult<MenuRespVO> getMenu(Long id) {
        MenuDO menu = menuService.getMenu(id);
        return success(BeanUtils.toBean(menu, MenuRespVO.class));
    }


    private List<MenuDO> getMenuListByRole(MenuListReqVO reqVO){

        // 1.1 获得用户信息
        AdminUserDO user = userService.getUser(getLoginUserId());
        if (user == null) {
            throw exception(new ErrorCode(10001,"获取用户信息失败"));
        }

        // 1.2 获得角色列表（包含直接分配和部门继承的角色，与登录后菜单可见范围保持一致）
        Set<Long> roleIds = permissionService.getLoginUserAllRoleIds(getLoginUserId());
        if (CollUtil.isEmpty(roleIds)) {
            throw exception(new ErrorCode(10002,"获取用户角色失败"));
        }
        List<RoleDO> roles = roleService.getRoleList(roleIds);
        roles.removeIf(role -> !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus())); // 移除禁用的角色

        boolean superAdmin = false;

        for (RoleDO role : roles) {
            if("super_admin".equals(role.getCode())){
                superAdmin = true;
            }
        }

        List<MenuDO> menuList = new ArrayList<>();
        // 1.3 获得菜单列表
        if (superAdmin) {
            if(reqVO != null) {
                return menuService.getMenuList(reqVO);
            }
            Set<Long> menuIds = permissionService.getRoleMenuListByRoleId(convertSet(roles, RoleDO::getId));
            menuList = menuService.getMenuList(menuIds);
            menuList = menuService.filterDisableMenus(menuList);

            return menuList;
        }

        // 不是超级管理员去除系统管理相关菜单
        Set<Long> menuIds = permissionService.getRoleMenuListByRoleId(convertSet(roles, RoleDO::getId));
        menuList = menuService.getMenuList(menuIds);
        menuList = menuService.filterDisableMenus(menuList);

        // ====================== 核心过滤逻辑：去除id=1及其所有子孙菜单 ======================
        // 1. 递归获取需要排除的菜单ID（包含id=1自身 + 所有子/孙/曾孙...节点）
        Set<Long> excludeMenuIds = getExcludeMenuIds(1L, menuList);
        // 2. 批量移除排除列表中的菜单
        menuList.removeIf(menu -> excludeMenuIds.contains(menu.getId()));
        // ==================================================================================

        return menuList;
    }

    /**
     * 递归收集：需要排除的菜单ID（包含根节点 + 所有子孙节点）
     * @param rootExcludeId 要排除的根菜单ID（这里固定传1L）
     * @param allMenus 全部菜单列表
     * @return 所有需要排除的菜单ID集合
     */
    private Set<Long> getExcludeMenuIds(Long rootExcludeId, List<MenuDO> allMenus) {
        Set<Long> excludeIds = new HashSet<>();
        // 添加当前要排除的根节点ID
        excludeIds.add(rootExcludeId);
        // 找到当前节点的所有直接子菜单
        List<MenuDO> childMenus = allMenus.stream()
                .filter(menu -> rootExcludeId.equals(menu.getParentId()))
                .toList();
        // 递归处理子菜单，收集所有子孙ID
        for (MenuDO childMenu : childMenus) {
            excludeIds.addAll(getExcludeMenuIds(childMenu.getId(), allMenus));
        }
        return excludeIds;
    }

}
