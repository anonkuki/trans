package cn.iocoder.sva.module.ai.controller.admin.translation;

import cn.iocoder.sva.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.sva.framework.common.pojo.CommonResult;
import cn.iocoder.sva.framework.common.pojo.PageParam;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;
import cn.iocoder.sva.framework.excel.core.util.ExcelUtils;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranFilePageReqVO;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranFileRespVO;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranFileSaveReqVO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranFileDO;
import cn.iocoder.sva.module.ai.service.translation.TranFileService;
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

@Tag(name = "管理后台 - AI翻译文件信息")
@RestController
@RequestMapping("/ai/tran-file")
@Validated
public class TranFileController {

    @Resource
    private TranFileService tranFileService;

    @PostMapping("/create")
    @Operation(summary = "创建AI翻译文件信息")
    @PreAuthorize("@ss.hasPermission('ai:tran-file:create')")
    public CommonResult<Long> createTranFile(@Valid @RequestBody TranFileSaveReqVO createReqVO) {
        return success(tranFileService.createTranFile(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新AI翻译文件信息")
    @PreAuthorize("@ss.hasPermission('ai:tran-file:update')")
    public CommonResult<Boolean> updateTranFile(@Valid @RequestBody TranFileSaveReqVO updateReqVO) {
        tranFileService.updateTranFile(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除AI翻译文件信息")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('ai:tran-file:delete')")
    public CommonResult<Boolean> deleteTranFile(@RequestParam("id") Long id) throws Exception {
        tranFileService.deleteTranFileWithMinioFiles(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除AI翻译文件信息")
    @PreAuthorize("@ss.hasPermission('ai:tran-file:delete')")
    public CommonResult<Boolean> deleteTranFileList(@RequestParam("ids") List<Long> ids) throws Exception {
        tranFileService.deleteTranFileListWithMinioFiles(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得AI翻译文件信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('ai:tran-file:query')")
    public CommonResult<TranFileRespVO> getTranFile(@RequestParam("id") Long id) {
        TranFileDO tranFile = tranFileService.getTranFile(id);
        return success(BeanUtils.toBean(tranFile, TranFileRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得AI翻译文件信息分页")
    @PreAuthorize("@ss.hasPermission('ai:tran-file:query')")
    public CommonResult<PageResult<TranFileRespVO>> getTranFilePage(@Valid TranFilePageReqVO pageReqVO) {
        PageResult<TranFileDO> pageResult = tranFileService.getTranFilePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TranFileRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出AI翻译文件信息 Excel")
    @PreAuthorize("@ss.hasPermission('ai:tran-file:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportTranFileExcel(@Valid TranFilePageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TranFileDO> list = tranFileService.getTranFilePage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "AI翻译文件信息.xls", "数据", TranFileRespVO.class,
                        BeanUtils.toBean(list, TranFileRespVO.class));
    }

}