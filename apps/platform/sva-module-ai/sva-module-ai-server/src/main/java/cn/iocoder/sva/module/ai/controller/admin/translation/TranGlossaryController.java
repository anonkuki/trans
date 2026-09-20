package cn.iocoder.sva.module.ai.controller.admin.translation;

import cn.iocoder.sva.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.sva.framework.common.pojo.CommonResult;
import cn.iocoder.sva.framework.common.pojo.PageParam;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;
import cn.iocoder.sva.framework.excel.core.util.ExcelUtils;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranGlossaryPageReqVO;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranGlossaryRespVO;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranGlossarySaveReqVO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranGlossaryDO;
import cn.iocoder.sva.module.ai.service.translation.TranGlossaryService;
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

@Tag(name = "管理后台 - 术语库管理")
@RestController
@RequestMapping("/ai/tran-glossary")
@Validated
public class TranGlossaryController {

    @Resource
    private TranGlossaryService tranGlossaryService;

    @PostMapping("/create")
    @Operation(summary = "创建术语库管理")
    @PreAuthorize("@ss.hasPermission('ai:tran-glossary:create')")
    public CommonResult<Long> createTranGlossary(@Valid @RequestBody TranGlossarySaveReqVO createReqVO) {
        return success(tranGlossaryService.createTranGlossary(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新术语库管理")
    @PreAuthorize("@ss.hasPermission('ai:tran-glossary:update')")
    public CommonResult<Boolean> updateTranGlossary(@Valid @RequestBody TranGlossarySaveReqVO updateReqVO) {
        tranGlossaryService.updateTranGlossary(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除术语库管理")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('ai:tran-glossary:delete')")
    public CommonResult<Boolean> deleteTranGlossary(@RequestParam("id") Long id) {
        tranGlossaryService.deleteTranGlossary(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除术语库管理")
                @PreAuthorize("@ss.hasPermission('ai:tran-glossary:delete')")
    public CommonResult<Boolean> deleteTranGlossaryList(@RequestParam("ids") List<Long> ids) {
        tranGlossaryService.deleteTranGlossaryListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得术语库管理")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('ai:tran-glossary:query')")
    public CommonResult<TranGlossaryRespVO> getTranGlossary(@RequestParam("id") Long id) {
        TranGlossaryDO tranGlossary = tranGlossaryService.getTranGlossary(id);
        return success(BeanUtils.toBean(tranGlossary, TranGlossaryRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得术语库管理分页")
    @PreAuthorize("@ss.hasPermission('ai:tran-glossary:query')")
    public CommonResult<PageResult<TranGlossaryRespVO>> getTranGlossaryPage(@Valid TranGlossaryPageReqVO pageReqVO) {
        PageResult<TranGlossaryDO> pageResult = tranGlossaryService.getTranGlossaryPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TranGlossaryRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出术语库管理 Excel")
    @PreAuthorize("@ss.hasPermission('ai:tran-glossary:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportTranGlossaryExcel(@Valid TranGlossaryPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TranGlossaryDO> list = tranGlossaryService.getTranGlossaryPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "术语库管理.xls", "数据", TranGlossaryRespVO.class,
                        BeanUtils.toBean(list, TranGlossaryRespVO.class));
    }

}