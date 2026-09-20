package cn.iocoder.sva.module.system.controller.admin.sync;

import cn.iocoder.sva.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.sva.framework.common.pojo.CommonResult;
import cn.iocoder.sva.framework.common.pojo.PageParam;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;
import cn.iocoder.sva.framework.excel.core.util.ExcelUtils;
import cn.iocoder.sva.module.system.controller.admin.sync.vo.SyncStaffPageReqVO;
import cn.iocoder.sva.module.system.controller.admin.sync.vo.SyncStaffRespVO;
import cn.iocoder.sva.module.system.controller.admin.sync.vo.SyncStaffSaveReqVO;
import cn.iocoder.sva.module.system.dal.dataobject.sync.SyncStaffDO;
import cn.iocoder.sva.module.system.service.sync.SyncStaffService;
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
import java.util.List;

import static cn.iocoder.sva.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.sva.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 全部人员信息同步")
@RestController
@RequestMapping("/system/sync-staff")
@Validated
public class SyncStaffController {

    @Resource
    private SyncStaffService syncStaffService;

    @PostMapping("/create")
    @Operation(summary = "创建全部人员信息同步")
    @PreAuthorize("@ss.hasPermission('system:sync-staff:create')")
    public CommonResult<Long> createSyncStaff(@Valid @RequestBody SyncStaffSaveReqVO createReqVO) {
        return success(syncStaffService.createSyncStaff(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新全部人员信息同步")
    @PreAuthorize("@ss.hasPermission('system:sync-staff:update')")
    public CommonResult<Boolean> updateSyncStaff(@Valid @RequestBody SyncStaffSaveReqVO updateReqVO) {
        syncStaffService.updateSyncStaff(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除全部人员信息同步")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:sync-staff:delete')")
    public CommonResult<Boolean> deleteSyncStaff(@RequestParam("id") Long id) {
        syncStaffService.deleteSyncStaff(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除全部人员信息同步")
                @PreAuthorize("@ss.hasPermission('system:sync-staff:delete')")
    public CommonResult<Boolean> deleteSyncStaffList(@RequestParam("ids") List<Long> ids) {
        syncStaffService.deleteSyncStaffListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得全部人员信息同步")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:sync-staff:query')")
    public CommonResult<SyncStaffRespVO> getSyncStaff(@RequestParam("id") Long id) {
        SyncStaffDO syncStaff = syncStaffService.getSyncStaff(id);
        return success(BeanUtils.toBean(syncStaff, SyncStaffRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得全部人员信息同步分页")
    @PreAuthorize("@ss.hasPermission('system:sync-staff:query')")
    public CommonResult<PageResult<SyncStaffRespVO>> getSyncStaffPage(@Valid SyncStaffPageReqVO pageReqVO) {
        PageResult<SyncStaffDO> pageResult = syncStaffService.getSyncStaffPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, SyncStaffRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出全部人员信息同步 Excel")
    @PreAuthorize("@ss.hasPermission('system:sync-staff:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSyncStaffExcel(@Valid SyncStaffPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<SyncStaffDO> list = syncStaffService.getSyncStaffPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "全部人员信息同步.xls", "数据", SyncStaffRespVO.class,
                        BeanUtils.toBean(list, SyncStaffRespVO.class));
    }

}