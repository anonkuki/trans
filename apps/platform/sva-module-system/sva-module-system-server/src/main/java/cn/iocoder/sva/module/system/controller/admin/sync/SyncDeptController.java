package cn.iocoder.sva.module.system.controller.admin.sync;

import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

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

import cn.iocoder.sva.module.system.controller.admin.sync.vo.*;
import cn.iocoder.sva.module.system.dal.dataobject.sync.SyncDeptDO;
import cn.iocoder.sva.module.system.service.sync.SyncDeptService;

@Tag(name = "管理后台 - 部门信息同步")
@RestController
@RequestMapping("/system/sync-dept")
@Validated
public class SyncDeptController {

    @Resource
    private SyncDeptService syncDeptService;

    @PostMapping("/create")
    @Operation(summary = "创建部门信息同步")
    @PreAuthorize("@ss.hasPermission('system:sync-dept:create')")
    public CommonResult<Long> createSyncDept(@Valid @RequestBody SyncDeptSaveReqVO createReqVO) {
        return success(syncDeptService.createSyncDept(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新部门信息同步")
    @PreAuthorize("@ss.hasPermission('system:sync-dept:update')")
    public CommonResult<Boolean> updateSyncDept(@Valid @RequestBody SyncDeptSaveReqVO updateReqVO) {
        syncDeptService.updateSyncDept(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除部门信息同步")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:sync-dept:delete')")
    public CommonResult<Boolean> deleteSyncDept(@RequestParam("id") Long id) {
        syncDeptService.deleteSyncDept(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除部门信息同步")
                @PreAuthorize("@ss.hasPermission('system:sync-dept:delete')")
    public CommonResult<Boolean> deleteSyncDeptList(@RequestParam("ids") List<Long> ids) {
        syncDeptService.deleteSyncDeptListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得部门信息同步")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:sync-dept:query')")
    public CommonResult<SyncDeptRespVO> getSyncDept(@RequestParam("id") Long id) {
        SyncDeptDO syncDept = syncDeptService.getSyncDept(id);
        return success(BeanUtils.toBean(syncDept, SyncDeptRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得部门信息同步分页")
    @PreAuthorize("@ss.hasPermission('system:sync-dept:query')")
    public CommonResult<PageResult<SyncDeptRespVO>> getSyncDeptPage(@Valid SyncDeptPageReqVO pageReqVO) {
        PageResult<SyncDeptDO> pageResult = syncDeptService.getSyncDeptPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, SyncDeptRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出部门信息同步 Excel")
    @PreAuthorize("@ss.hasPermission('system:sync-dept:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSyncDeptExcel(@Valid SyncDeptPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<SyncDeptDO> list = syncDeptService.getSyncDeptPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "部门信息同步.xls", "数据", SyncDeptRespVO.class,
                        BeanUtils.toBean(list, SyncDeptRespVO.class));
    }

}