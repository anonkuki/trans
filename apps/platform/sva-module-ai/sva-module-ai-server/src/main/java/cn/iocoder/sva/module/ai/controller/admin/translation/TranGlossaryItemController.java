package cn.iocoder.sva.module.ai.controller.admin.translation;

import cn.hutool.core.io.IoUtil;
import cn.iocoder.sva.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.sva.framework.common.pojo.CommonResult;
import cn.iocoder.sva.framework.common.pojo.PageParam;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;
import cn.iocoder.sva.framework.excel.core.util.ExcelUtils;
import cn.iocoder.sva.module.ai.controller.admin.file.vo.file.FileUploadReqVO;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranGlossaryItemPageReqVO;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranGlossaryItemRespVO;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranGlossaryItemSaveReqVO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranGlossaryItemDO;
import cn.iocoder.sva.module.ai.service.translation.TranGlossaryItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static cn.iocoder.sva.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.sva.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 术语管理")
@RestController
@RequestMapping("/ai/tran-glossary-item")
@Validated
public class TranGlossaryItemController {

    @Resource
    private TranGlossaryItemService tranGlossaryItemService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "导入术语库")
    @Parameter(name = "file", description = "导入术语库", required = true,
            schema = @Schema(type = "string", format = "binary"))
    public CommonResult<String> uploadFile(@Valid FileUploadReqVO uploadReqVO) throws Exception {
        return CommonResult.success(tranGlossaryItemService.uploadFile(uploadReqVO));
    }

    @PostMapping("/create")
    @Operation(summary = "创建术语管理")
//    @PreAuthorize("@ss.hasPermission('ai:tran-glossary-item:create')")
    public CommonResult<Long> createTranGlossaryItem(@Valid @RequestBody TranGlossaryItemSaveReqVO createReqVO) {
        return success(tranGlossaryItemService.createTranGlossaryItem(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新术语管理")
//    @PreAuthorize("@ss.hasPermission('ai:tran-glossary-item:update')")
    public CommonResult<Boolean> updateTranGlossaryItem(@Valid @RequestBody TranGlossaryItemSaveReqVO updateReqVO) {
        tranGlossaryItemService.updateTranGlossaryItem(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除术语管理")
    @Parameter(name = "id", description = "编号", required = true)
//    @PreAuthorize("@ss.hasPermission('ai:tran-glossary-item:delete')")
    public CommonResult<Boolean> deleteTranGlossaryItem(@RequestParam("id") Long id) {
        tranGlossaryItemService.deleteTranGlossaryItem(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除术语管理")
//    @PreAuthorize("@ss.hasPermission('ai:tran-glossary-item:delete')")
    public CommonResult<Boolean> deleteTranGlossaryItemList(@RequestParam("ids") List<Long> ids) {
        tranGlossaryItemService.deleteTranGlossaryItemListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得术语管理")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
//    @PreAuthorize("@ss.hasPermission('ai:tran-glossary-item:query')")
    public CommonResult<TranGlossaryItemRespVO> getTranGlossaryItem(@RequestParam("id") Long id) {
        TranGlossaryItemDO tranGlossaryItem = tranGlossaryItemService.getTranGlossaryItem(id);
        return success(BeanUtils.toBean(tranGlossaryItem, TranGlossaryItemRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得术语管理分页")
//    @PreAuthorize("@ss.hasPermission('ai:tran-glossary-item:query')")
    public CommonResult<PageResult<TranGlossaryItemRespVO>> getTranGlossaryItemPage(@Valid TranGlossaryItemPageReqVO pageReqVO) {
        PageResult<TranGlossaryItemDO> pageResult = tranGlossaryItemService.getTranGlossaryItemPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TranGlossaryItemRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出术语管理 Excel")
//    @PreAuthorize("@ss.hasPermission('ai:tran-glossary-item:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportTranGlossaryItemExcel(@Valid TranGlossaryItemPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TranGlossaryItemDO> list = tranGlossaryItemService.getTranGlossaryItemPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "术语管理.xls", "数据", TranGlossaryItemRespVO.class,
                        BeanUtils.toBean(list, TranGlossaryItemRespVO.class));
    }

}