package cn.iocoder.sva.module.system.controller.admin.role;

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

import cn.iocoder.sva.module.system.controller.admin.role.vo.*;
import cn.iocoder.sva.module.system.dal.dataobject.role.DeptRoleDO;
import cn.iocoder.sva.module.system.service.role.DeptRoleService;

@Tag(name = "管理后台 - 部门和角色关联")
@RestController
@RequestMapping("/system/dept-role")
@Validated
public class DeptRoleController {

    @Resource
    private DeptRoleService deptRoleService;

    @PostMapping("/create")
    @Operation(summary = "创建部门和角色关联")
    @PreAuthorize("@ss.hasPermission('system:dept-role:create')")
    public CommonResult<Long> createDeptRole(@Valid @RequestBody DeptRoleSaveReqVO createReqVO) {
        return success(deptRoleService.createDeptRole(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新部门和角色关联")
    @PreAuthorize("@ss.hasPermission('system:dept-role:update')")
    public CommonResult<Boolean> updateDeptRole(@Valid @RequestBody DeptRoleSaveReqVO updateReqVO) {
        deptRoleService.updateDeptRole(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除部门和角色关联")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:dept-role:delete')")
    public CommonResult<Boolean> deleteDeptRole(@RequestParam("id") Long id) {
        deptRoleService.deleteDeptRole(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除部门和角色关联")
                @PreAuthorize("@ss.hasPermission('system:dept-role:delete')")
    public CommonResult<Boolean> deleteDeptRoleList(@RequestParam("ids") List<Long> ids) {
        deptRoleService.deleteDeptRoleListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得部门和角色关联")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:dept-role:query')")
    public CommonResult<DeptRoleRespVO> getDeptRole(@RequestParam("id") Long id) {
        DeptRoleDO deptRole = deptRoleService.getDeptRole(id);
        return success(BeanUtils.toBean(deptRole, DeptRoleRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得部门和角色关联分页")
    @PreAuthorize("@ss.hasPermission('system:dept-role:query')")
    public CommonResult<PageResult<DeptRoleRespVO>> getDeptRolePage(@Valid DeptRolePageReqVO pageReqVO) {
        PageResult<DeptRoleDO> pageResult = deptRoleService.getDeptRolePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, DeptRoleRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出部门和角色关联 Excel")
    @PreAuthorize("@ss.hasPermission('system:dept-role:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportDeptRoleExcel(@Valid DeptRolePageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<DeptRoleDO> list = deptRoleService.getDeptRolePage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "部门和角色关联.xls", "数据", DeptRoleRespVO.class,
                        BeanUtils.toBean(list, DeptRoleRespVO.class));
    }

}