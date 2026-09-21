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
import cn.iocoder.sva.module.system.dal.dataobject.extlink.ExternalLinkDO;
import cn.iocoder.sva.module.system.service.extlink.ExternalLinkService;

import static cn.iocoder.sva.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 系统外链")
@RestController
@RequestMapping("/system/external-link")
@Validated
public class ExternalLinkController {

    @Resource
    private ExternalLinkService externalLinkService;

    @PostMapping("/create")
    @Operation(summary = "创建系统外链")
    @PreAuthorize("@ss.hasPermission('system:external-link:create')")
    public CommonResult<Long> createExternalLink(@Valid @RequestBody ExternalLinkSaveReqVO createReqVO) {
        return success(externalLinkService.createExternalLink(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新系统外链")
    @PreAuthorize("@ss.hasPermission('system:external-link:update')")
    public CommonResult<Boolean> updateExternalLink(@Valid @RequestBody ExternalLinkSaveReqVO updateReqVO) {
        externalLinkService.updateExternalLink(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除系统外链")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:external-link:delete')")
    public CommonResult<Boolean> deleteExternalLink(@RequestParam("id") Long id) {
        externalLinkService.deleteExternalLink(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除系统外链")
                @PreAuthorize("@ss.hasPermission('system:external-link:delete')")
    public CommonResult<Boolean> deleteExternalLinkList(@RequestParam("ids") List<Long> ids) {
        externalLinkService.deleteExternalLinkListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得系统外链")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:external-link:query')")
    public CommonResult<ExternalLinkRespVO> getExternalLink(@RequestParam("id") Long id) {
        ExternalLinkDO externalLink = externalLinkService.getExternalLink(id);
        return success(BeanUtils.toBean(externalLink, ExternalLinkRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得系统外链分页")
    @PreAuthorize("@ss.hasPermission('system:external-link:query')")
    public CommonResult<PageResult<ExternalLinkRespVO>> getExternalLinkPage(@Valid ExternalLinkPageReqVO pageReqVO) {
        PageResult<ExternalLinkDO> pageResult = externalLinkService.getExternalLinkPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ExternalLinkRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获取系统外链精简信息列表",
            description = "只包含被开启的外链，用于【角色分配外链】功能的选项")
    public CommonResult<List<ExternalLinkRespVO>> getSimpleExternalLinkList() {
        List<ExternalLinkDO> list = externalLinkService.getSimpleExternalLinkList();
        return success(BeanUtils.toBean(list, ExternalLinkRespVO.class));
    }

    @GetMapping("/home-list")
    @Operation(summary = "获得首页展示的外链列表",
            description = "返回当前登录用户的角色有权限、且开启状态的外链，用于【首页外链卡片】展示")
    public CommonResult<List<ExternalLinkRespVO>> getHomeExternalLinkList() {
        List<ExternalLinkDO> list = externalLinkService.getHomeExternalLinkList(getLoginUserId());
        return success(BeanUtils.toBean(list, ExternalLinkRespVO.class));
    }

    @PostMapping("/click")
    @Operation(summary = "记录外链点击")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> clickExternalLink(@RequestParam("id") Long id) {
        externalLinkService.clickExternalLink(id);
        return success(true);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出系统外链 Excel")
    @PreAuthorize("@ss.hasPermission('system:external-link:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExternalLinkExcel(@Valid ExternalLinkPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ExternalLinkDO> list = externalLinkService.getExternalLinkPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "系统外链.xls", "数据", ExternalLinkRespVO.class,
                        BeanUtils.toBean(list, ExternalLinkRespVO.class));
    }

}