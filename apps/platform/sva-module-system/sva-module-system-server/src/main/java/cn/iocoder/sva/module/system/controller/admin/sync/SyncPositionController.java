package cn.iocoder.sva.module.system.controller.admin.sync;

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

import cn.iocoder.sva.module.system.controller.admin.sync.vo.*;
import cn.iocoder.sva.module.system.dal.dataobject.sync.SyncPositionDO;
import cn.iocoder.sva.module.system.service.sync.SyncPositionService;

@Tag(name = "管理后台 - 岗位信息同步")
@RestController
@RequestMapping("/system/sync-position")
@Validated
public class SyncPositionController {

    @Resource
    private SyncPositionService syncPositionService;

    @PostMapping("/create")
    @Operation(summary = "创建岗位信息同步")
    @PreAuthorize("@ss.hasPermission('system:sync-position:create')")
    public CommonResult<Long> createSyncPosition(@Valid @RequestBody SyncPositionSaveReqVO createReqVO) {
        return success(syncPositionService.createSyncPosition(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新岗位信息同步")
    @PreAuthorize("@ss.hasPermission('system:sync-position:update')")
    public CommonResult<Boolean> updateSyncPosition(@Valid @RequestBody SyncPositionSaveReqVO updateReqVO) {
        syncPositionService.updateSyncPosition(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除岗位信息同步")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:sync-position:delete')")
    public CommonResult<Boolean> deleteSyncPosition(@RequestParam("id") Long id) {
        syncPositionService.deleteSyncPosition(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除岗位信息同步")
                @PreAuthorize("@ss.hasPermission('system:sync-position:delete')")
    public CommonResult<Boolean> deleteSyncPositionList(@RequestParam("ids") List<Long> ids) {
        syncPositionService.deleteSyncPositionListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得岗位信息同步")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:sync-position:query')")
    public CommonResult<SyncPositionRespVO> getSyncPosition(@RequestParam("id") Long id) {
        SyncPositionDO syncPosition = syncPositionService.getSyncPosition(id);
        return success(BeanUtils.toBean(syncPosition, SyncPositionRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得岗位信息同步分页")
    @PreAuthorize("@ss.hasPermission('system:sync-position:query')")
    public CommonResult<PageResult<SyncPositionRespVO>> getSyncPositionPage(@Valid SyncPositionPageReqVO pageReqVO) {
        PageResult<SyncPositionDO> pageResult = syncPositionService.getSyncPositionPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, SyncPositionRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出岗位信息同步 Excel")
    @PreAuthorize("@ss.hasPermission('system:sync-position:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSyncPositionExcel(@Valid SyncPositionPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<SyncPositionDO> list = syncPositionService.getSyncPositionPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "岗位信息同步.xls", "数据", SyncPositionRespVO.class,
                        BeanUtils.toBean(list, SyncPositionRespVO.class));
    }

}