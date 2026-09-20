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
import cn.iocoder.sva.module.system.dal.dataobject.sync.SyncPersonDO;
import cn.iocoder.sva.module.system.service.sync.SyncPersonService;

@Tag(name = "管理后台 - 人员信息同步")
@RestController
@RequestMapping("/system/sync-person")
@Validated
public class SyncPersonController {

    @Resource
    private SyncPersonService syncPersonService;

    @PostMapping("/create")
    @Operation(summary = "创建人员信息同步")
    @PreAuthorize("@ss.hasPermission('system:sync-person:create')")
    public CommonResult<Long> createSyncPerson(@Valid @RequestBody SyncPersonSaveReqVO createReqVO) {
        return success(syncPersonService.createSyncPerson(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新人员信息同步")
    @PreAuthorize("@ss.hasPermission('system:sync-person:update')")
    public CommonResult<Boolean> updateSyncPerson(@Valid @RequestBody SyncPersonSaveReqVO updateReqVO) {
        syncPersonService.updateSyncPerson(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除人员信息同步")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:sync-person:delete')")
    public CommonResult<Boolean> deleteSyncPerson(@RequestParam("id") Long id) {
        syncPersonService.deleteSyncPerson(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除人员信息同步")
                @PreAuthorize("@ss.hasPermission('system:sync-person:delete')")
    public CommonResult<Boolean> deleteSyncPersonList(@RequestParam("ids") List<Long> ids) {
        syncPersonService.deleteSyncPersonListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得人员信息同步")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:sync-person:query')")
    public CommonResult<SyncPersonRespVO> getSyncPerson(@RequestParam("id") Long id) {
        SyncPersonDO syncPerson = syncPersonService.getSyncPerson(id);
        return success(BeanUtils.toBean(syncPerson, SyncPersonRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得人员信息同步分页")
    @PreAuthorize("@ss.hasPermission('system:sync-person:query')")
    public CommonResult<PageResult<SyncPersonRespVO>> getSyncPersonPage(@Valid SyncPersonPageReqVO pageReqVO) {
        PageResult<SyncPersonDO> pageResult = syncPersonService.getSyncPersonPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, SyncPersonRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出人员信息同步 Excel")
    @PreAuthorize("@ss.hasPermission('system:sync-person:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSyncPersonExcel(@Valid SyncPersonPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<SyncPersonDO> list = syncPersonService.getSyncPersonPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "人员信息同步.xls", "数据", SyncPersonRespVO.class,
                        BeanUtils.toBean(list, SyncPersonRespVO.class));
    }

}