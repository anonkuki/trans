package cn.iocoder.sva.module.system.controller.admin.permission;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.sva.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.sva.framework.common.enums.CommonStatusEnum;
import cn.iocoder.sva.framework.common.exception.ErrorCode;
import cn.iocoder.sva.framework.common.pojo.CommonResult;
import cn.iocoder.sva.framework.common.pojo.PageParam;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;
import cn.iocoder.sva.framework.excel.core.util.ExcelUtils;
import cn.iocoder.sva.module.system.controller.admin.permission.vo.role.RolePageReqVO;
import cn.iocoder.sva.module.system.controller.admin.permission.vo.role.RoleRespVO;
import cn.iocoder.sva.module.system.controller.admin.permission.vo.role.RoleSaveReqVO;
import cn.iocoder.sva.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.sva.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.sva.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.sva.module.system.service.permission.PermissionService;
import cn.iocoder.sva.module.system.service.permission.RoleService;
import cn.iocoder.sva.module.system.service.user.AdminUserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.sva.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.sva.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.sva.framework.common.pojo.CommonResult.success;
import static cn.iocoder.sva.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.sva.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static java.util.Collections.singleton;

@Tag(name = "管理后台 - 角色")
@RestController
@RequestMapping("/system/role")
@Validated
public class RoleController {

    @Resource
    private RoleService roleService;

    @Resource
    private AdminUserService userService;
    @Resource
    private PermissionService permissionService;

    @PostMapping("/create")
    @Operation(summary = "创建角色")
    @PreAuthorize("@ss.hasPermission('system:role:create')")
    public CommonResult<Long> createRole(@Valid @RequestBody RoleSaveReqVO createReqVO) {
        createReqVO.setFlag(superAdmin());
        return success(roleService.createRole(createReqVO, null));
    }

    @PutMapping("/update")
    @Operation(summary = "修改角色")
    @PreAuthorize("@ss.hasPermission('system:role:update')")
    public CommonResult<Boolean> updateRole(@Valid @RequestBody RoleSaveReqVO updateReqVO) {
        updateReqVO.setFlag(superAdmin());
        roleService.updateRole(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除角色")
    @Parameter(name = "id", description = "角色编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:role:delete')")
    public CommonResult<Boolean> deleteRole(@RequestParam("id") Long id) {
        roleService.deleteRole(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除角色")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('system:role:delete')")
    public CommonResult<Boolean> deleteRoleList(@RequestParam("ids") List<Long> ids) {
        roleService.deleteRoleList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得角色信息")
    @PreAuthorize("@ss.hasPermission('system:role:query')")
    public CommonResult<RoleRespVO> getRole(@RequestParam("id") Long id) {
        RoleDO role = roleService.getRole(id);
        return success(BeanUtils.toBean(role, RoleRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得角色分页")
    @PreAuthorize("@ss.hasPermission('system:role:query')")
    public CommonResult<PageResult<RoleRespVO>> getRolePage(RolePageReqVO pageReqVO) {
        // 获取当前登录用户的角色列表
        Set<Long> roleListByUser = getRoleListByUser();
        pageReqVO.setRoleIds(roleListByUser.stream().toList());
        // 默认根据角色查询
        pageReqVO.setFlag(false);
        // 判断当前用户有超级管理员角色则查询全部
        if(roleListByUser.contains(1L)){
            pageReqVO.setFlag(true);
        }
        PageResult<RoleDO> pageResult = roleService.getRolePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, RoleRespVO.class));
    }

    @GetMapping("/listself")
    @Operation(summary = "获得角色分页")
//    @PreAuthorize("@ss.hasPermission('system:role:query')")
    public CommonResult<List<RoleRespVO>> getRolePageAndSelf(RolePageReqVO pageReqVO) {
        // 获取当前登录用户的角色列表
        Set<Long> roleListByUser = getRoleListByUser();
        pageReqVO.setRoleIds(roleListByUser.stream().toList());
        // 默认根据角色查询
        pageReqVO.setFlag(false);
        // 判断当前用户有超级管理员角色则查询全部
        if(roleListByUser.contains(1L)){
            pageReqVO.setFlag(true);
        }
        List<RoleDO> pageResult = roleService.getRoleListSelf(pageReqVO);
        // 追加超级管理员创建的自己的角色信息
        List<RoleDO> roleSelf = roleService.getRoleSelf(roleListByUser);
        pageResult.addAll(roleSelf);

        return success(BeanUtils.toBean(pageResult, RoleRespVO.class));
    }

    @GetMapping({"/list-all-simple", "/simple-list"})
    @Operation(summary = "获取角色精简信息列表", description = "只包含被开启的角色，主要用于前端的下拉选项")
    public CommonResult<List<RoleRespVO>> getSimpleRoleList() {

        List<RoleDO> list = roleService.getRoleListByStatus(singleton(CommonStatusEnum.ENABLE.getStatus()));
        list.sort(Comparator.comparing(RoleDO::getSort));
        return success(BeanUtils.toBean(list, RoleRespVO.class));
    }

    @GetMapping("/login-role-select")
    @Operation(summary = "获取角色精简信息下拉框", description = "只包含被开启的角色，主要用于前端的下拉选项")
    public CommonResult<List<RoleRespVO>> getLoginUserRoleSelectList() {

        if(superAdmin()){
            List<RoleDO> list = roleService.getRoleListByStatus(singleton(CommonStatusEnum.ENABLE.getStatus()));
            list.sort(Comparator.comparing(RoleDO::getSort));
            return success(BeanUtils.toBean(list, RoleRespVO.class));
        }

        Set<Long> roleListByUser = getRoleListByUserSelect();

        List<Long> idList = roleListByUser.stream().toList();
        List<RoleDO> list = roleService.getRoleList(idList);

        list.sort(Comparator.comparing(RoleDO::getSort));
        return success(BeanUtils.toBean(list, RoleRespVO.class));
    }

    @GetMapping("/login-role")
    @Operation(summary = "获取当前登录角色精简信息列表", description = "只包含被开启的角色，主要用于前端的下拉选项")
    public CommonResult<List<RoleRespVO>> getLoginUserRoleList() {
        // 0. 判断是否是超级管理员，是的话返回所有启用的角色
        if (superAdmin()) {
            List<RoleDO> list = roleService.getRoleListByStatus(singleton(CommonStatusEnum.ENABLE.getStatus()));
            list.sort(Comparator.comparing(RoleDO::getSort));
            return success(BeanUtils.toBean(list, RoleRespVO.class));
        }

        // 1.1 获得用户信息
        AdminUserDO user = userService.getUser(getLoginUserId());
        if (user == null) {
            throw exception(new ErrorCode(10001, "获取用户信息失败"));
        }

        // 1.2 获得角色列表
        Set<Long> roleIds = permissionService.getUserRoleIdListByUserId(getLoginUserId());
        if (CollUtil.isEmpty(roleIds)) {
            throw exception(new ErrorCode(10002, "获取用户角色失败"));
        }
        List<RoleDO> roles = roleService.getRoleList(roleIds);
        roles.removeIf(role -> !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus())); // 移除禁用的角色

        // 1.3 处理角色替换逻辑
        List<RoleDO> finalRoles = new ArrayList<>();
        for (RoleDO role : roles) {
            Long creatorRoleId = role.getCreatorRole();
            // 如果 creatorRole 是 1 或者 null，则保留当前角色
            if (creatorRoleId == null || Long.valueOf(1).equals(creatorRoleId)) {
                finalRoles.add(role);
            } else {
                // 否则查询 creatorRole 对应的角色信息，并添加到列表中（不保留当前角色）
                RoleDO creatorRoleDO = roleService.getRole(creatorRoleId);
                if (creatorRoleDO != null && CommonStatusEnum.ENABLE.getStatus().equals(creatorRoleDO.getStatus())) {
                    finalRoles.add(creatorRoleDO);
                }
            }
        }

        // 去重（避免同一个角色被重复添加）
        finalRoles = finalRoles.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(RoleDO::getId))),
                        ArrayList::new
                ));

        return success(BeanUtils.toBean(finalRoles, RoleRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出角色 Excel")
    @ApiAccessLog(operateType = EXPORT)
    @PreAuthorize("@ss.hasPermission('system:role:export')")
    public void export(HttpServletResponse response, @Validated RolePageReqVO exportReqVO) throws IOException {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<RoleDO> list = roleService.getRolePage(exportReqVO).getList();
        // 输出
        ExcelUtils.write(response, "角色数据.xls", "数据", RoleRespVO.class,
                BeanUtils.toBean(list, RoleRespVO.class));
    }

    private Set<Long> getRoleListByUser(){

        // 1.1 获得用户信息
        AdminUserDO user = userService.getUser(getLoginUserId());
        if (user == null) {
            throw exception(new ErrorCode(10001,"获取用户信息失败"));
        }

        // 1.2 获得角色列表（包含直接分配和部门角色）
        Set<Long> roleIds = permissionService.getLoginUserAllRoleIds(getLoginUserId());
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptySet();
        }
        
        List<RoleDO> roles = roleService.getRoleList(roleIds);
        roles.removeIf(role -> !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus())); // 移除禁用的角色
        Set<Long> longs = convertSet(roles, RoleDO::getId);

        for (RoleDO role : roles) {
            if("super_admin".equals(role.getCode())){
                longs = new HashSet<Long>();
                longs.add(1L);
                return longs;
            }
            Long creatorRole = role.getCreatorRole();
            if(creatorRole != null && creatorRole != 1L){
                longs.add(creatorRole);
            }
        }


        return longs;
    }

    private Set<Long> getRoleListByUserSelect(){

        // 1.1 获得用户信息
        AdminUserDO user = userService.getUser(getLoginUserId());
        if (user == null) {
            throw exception(new ErrorCode(10001,"获取用户信息失败"));
        }
        // 1.2 获得角色列表（包含直接分配和部门角色）
        Set<Long> roleIds = permissionService.getLoginUserAllRoleIds(getLoginUserId());
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptySet();
        }
        
        List<RoleDO> roles = roleService.getRoleList(roleIds);
        for (RoleDO role : roles) {
            if(role.getCreatorRole() != null &&  role.getCreatorRole() != 1L){
                roleIds.add(role.getCreatorRole());
            }
        }

        List<RoleDO> list = roleService.getRoleList(new QueryWrapper<RoleDO>().in("creator_role", roleIds));

//        List<RoleDO> roles = roleService.getRoleList(roleIds);
        roles.addAll(list);
        roles.removeIf(role -> !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus())); // 移除禁用的角色
        Set<Long> longs = convertSet(roles, RoleDO::getId);

        return longs;
    }

    private Boolean superAdmin(){

        // 1.1 获得用户信息
        AdminUserDO user = userService.getUser(getLoginUserId());
        if (user == null) {
            throw exception(new ErrorCode(10001,"获取用户信息失败"));
        }

        // 1.2 获得角色列表（包含直接分配和部门角色）
        Set<Long> roleIds = permissionService.getLoginUserAllRoleIds(getLoginUserId());
        if (CollUtil.isEmpty(roleIds)) {
            return false;
        }
        
        List<RoleDO> roles = roleService.getRoleList(roleIds);
        roles.removeIf(role -> !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus())); // 移除禁用的角色

        for (RoleDO role : roles) {
            if("super_admin".equals(role.getCode())){
                return true;
            }
        }

        return false;
    }


}
