package cn.iocoder.sva.module.system.controller.admin.temp;

import io.swagger.v3.oas.annotations.media.Schema;
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

import cn.iocoder.sva.module.system.controller.admin.temp.vo.*;
import cn.iocoder.sva.module.system.dal.dataobject.temp.TempFileDO;
import cn.iocoder.sva.module.system.service.temp.TempFileService;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "管理后台 - 模板管理")
@RestController
@RequestMapping("/system/temp-file")
@Validated
public class TempFileController {

    @Resource
    private TempFileService tempFileService;

    @PostMapping("/create")
    @Operation(summary = "创建模板管理")
    @PreAuthorize("@ss.hasPermission('system:temp-file:create')")
    public CommonResult<Long> createTempFile(@Valid @RequestBody TempFileSaveReqVO createReqVO) {
        return success(tempFileService.createTempFile(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新模板管理")
    @PreAuthorize("@ss.hasPermission('system:temp-file:update')")
    public CommonResult<Boolean> updateTempFile(@Valid @RequestBody TempFileSaveReqVO updateReqVO) {
        tempFileService.updateTempFile(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除模板管理")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:temp-file:delete')")
    public CommonResult<Boolean> deleteTempFile(@RequestParam("id") Long id) {
        tempFileService.deleteTempFile(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除模板管理")
                @PreAuthorize("@ss.hasPermission('system:temp-file:delete')")
    public CommonResult<Boolean> deleteTempFileList(@RequestParam("ids") List<Long> ids) {
        tempFileService.deleteTempFileListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得模板管理")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:temp-file:query')")
    public CommonResult<TempFileRespVO> getTempFile(@RequestParam("id") Long id) {
        TempFileDO tempFile = tempFileService.getTempFile(id);
        return success(BeanUtils.toBean(tempFile, TempFileRespVO.class));
    }

    @GetMapping("/get-by-temp-name")
    @Operation(summary = "根据模板标识获得模板管理")
    @Parameter(name = "tempName", description = "模板标识", required = true)
    public CommonResult<TempFileRespVO> getTempFileByTempName(@RequestParam("tempName") String tempName) {
        TempFileDO tempFile = tempFileService.getTempFileByTempName(tempName);
        return success(BeanUtils.toBean(tempFile, TempFileRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得模板管理分页")
    @PreAuthorize("@ss.hasPermission('system:temp-file:query')")
    public CommonResult<PageResult<TempFileRespVO>> getTempFilePage(@Valid TempFilePageReqVO pageReqVO) {
        PageResult<TempFileDO> pageResult = tempFileService.getTempFilePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TempFileRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出模板管理 Excel")
    @PreAuthorize("@ss.hasPermission('system:temp-file:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportTempFileExcel(@Valid TempFilePageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TempFileDO> list = tempFileService.getTempFilePage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "模板管理.xls", "数据", TempFileRespVO.class,
                        BeanUtils.toBean(list, TempFileRespVO.class));
    }

    @PostMapping("/upload")
    @Operation(summary = "上传文件")
    @Parameter(name = "file", description = "文件", required = true,
            schema = @Schema(type = "string", format = "binary"))
    @Parameter(name = "tempName", description = "模板标识", required = true)
    public CommonResult<Map<String, Object>> uploadFile(
            @Parameter(description = "文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "模板标识") @RequestParam("tempName") String tempName) throws Exception {
        Map<String, Object> result = tempFileService.uploadFile(file, tempName);
        return success(result);
    }

}