package cn.iocoder.sva.module.system.controller.admin.extlink;

import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.constraints.*;
import jakarta.validation.*;
import jakarta.servlet.http.*;
import java.util.*;
import java.io.IOException;

import cn.iocoder.sva.framework.common.pojo.PageParam;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.pojo.CommonResult;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;
import static cn.iocoder.sva.framework.common.pojo.CommonResult.success;

import cn.iocoder.sva.framework.excel.core.util.ExcelUtils;

import cn.iocoder.sva.framework.apilog.core.annotation.ApiAccessLog;
import static cn.iocoder.sva.framework.apilog.core.enums.OperateTypeEnum.*;

import cn.iocoder.sva.module.system.controller.admin.extlink.vo.*;
import cn.iocoder.sva.module.system.dal.dataobject.extlink.ExternalLinkRoleDO;
import cn.iocoder.sva.module.system.service.extlink.ExternalLinkRoleService;

@Tag(name = "管理后台 - 外链和角色关联")
@RestController
@RequestMapping("/system/external-link-role")
@Validated
public class ExternalLinkRoleController {

    @Resource
    private ExternalLinkRoleService externalLinkRoleService;

    @PostMapping("/create")
    @Operation(summary = "创建外链和角色关联")
    @PreAuthorize("@ss.hasPermission('system:external-link-role:create')")
    public CommonResult<Long> createExternalLinkRole(@Valid @RequestBody ExternalLinkRoleSaveReqVO createReqVO) {
        return success(externalLinkRoleService.createExternalLinkRole(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新外链和角色关联")
    @PreAuthorize("@ss.hasPermission('system:external-link-role:update')")
    public CommonResult<Boolean> updateExternalLinkRole(@Valid @RequestBody ExternalLinkRoleSaveReqVO updateReqVO) {
        externalLinkRoleService.updateExternalLinkRole(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除外链和角色关联")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:external-link-role:delete')")
    public CommonResult<Boolean> deleteExternalLinkRole(@RequestParam("id") Long id) {
        externalLinkRoleService.deleteExternalLinkRole(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除外链和角色关联")
                @PreAuthorize("@ss.hasPermission('system:external-link-role:delete')")
    public CommonResult<Boolean> deleteExternalLinkRoleList(@RequestParam("ids") List<Long> ids) {
        externalLinkRoleService.deleteExternalLinkRoleListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得外链和角色关联")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:external-link-role:query')")
    public CommonResult<ExternalLinkRoleRespVO> getExternalLinkRole(@RequestParam("id") Long id) {
        ExternalLinkRoleDO externalLinkRole = externalLinkRoleService.getExternalLinkRole(id);
        return success(BeanUtils.toBean(externalLinkRole, ExternalLinkRoleRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得外链和角色关联分页")
    @PreAuthorize("@ss.hasPermission('system:external-link-role:query')")
    public CommonResult<PageResult<ExternalLinkRoleRespVO>> getExternalLinkRolePage(@Valid ExternalLinkRolePageReqVO pageReqVO) {
        PageResult<ExternalLinkRoleDO> pageResult = externalLinkRoleService.getExternalLinkRolePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ExternalLinkRoleRespVO.class));
    }

    @GetMapping("/list-role-links")
    @Operation(summary = "获得角色拥有的外链编号")
    @Parameter(name = "roleId", description = "角色编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:external-link:assign')")
    public CommonResult<Set<Long>> getRoleExternalLinkList(@RequestParam("roleId") Long roleId) {
        return success(externalLinkRoleService.getLinkIdsByRoleId(roleId));
    }

    @PostMapping("/assign-role-link")
    @Operation(summary = "赋予角色外链")
    @PreAuthorize("@ss.hasPermission('system:external-link:assign')")
    public CommonResult<Boolean> assignRoleExternalLink(@Validated @RequestBody ExternalLinkRoleAssignReqVO reqVO) {
        externalLinkRoleService.assignRoleExternalLink(reqVO.getRoleId(), reqVO.getLinkIds());
        return success(true);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出外链和角色关联 Excel")
    @PreAuthorize("@ss.hasPermission('system:external-link-role:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExternalLinkRoleExcel(@Valid ExternalLinkRolePageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ExternalLinkRoleDO> list = externalLinkRoleService.getExternalLinkRolePage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "外链和角色关联.xls", "数据", ExternalLinkRoleRespVO.class,
                        BeanUtils.toBean(list, ExternalLinkRoleRespVO.class));
    }

}