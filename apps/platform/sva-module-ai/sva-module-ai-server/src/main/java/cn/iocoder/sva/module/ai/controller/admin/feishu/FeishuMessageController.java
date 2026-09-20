package cn.iocoder.sva.module.ai.controller.admin.feishu;

import cn.iocoder.sva.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.sva.framework.common.pojo.CommonResult;
import cn.iocoder.sva.framework.common.pojo.PageParam;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;
import cn.iocoder.sva.framework.excel.core.util.ExcelUtils;
import cn.iocoder.sva.module.ai.controller.admin.feishu.vo.FeishuMessagePageReqVO;
import cn.iocoder.sva.module.ai.controller.admin.feishu.vo.FeishuMessageRespVO;
import cn.iocoder.sva.module.ai.controller.admin.feishu.vo.FeishuMessageSaveReqVO;
import cn.iocoder.sva.module.ai.dal.dataobject.feishu.FeishuMessageDO;
import cn.iocoder.sva.module.ai.dal.dataobject.feishu.FeishuMessageSendReqDTO;
import cn.iocoder.sva.module.ai.service.feishu.FeishuMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

import static cn.iocoder.sva.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.sva.framework.common.pojo.CommonResult.success;

/**
 * 飞书消息 Controller
 *
 * @author 科兴源码
 */
@RestController
@RequestMapping("/ai/feishu/message")
@Tag(name = "飞书消息接口", description = "提供飞书消息发送相关的接口能力")
public class FeishuMessageController {

    @Resource
    private FeishuMessageService feishuMessageService;

    @PostMapping("/send-text")
    @Operation(summary = "发送飞书文本消息", description = "支持向指定用户/群组发送文本类型的飞书消息")
    public CommonResult<String> sendTextMessage(@Valid @RequestBody FeishuMessageSendReqDTO req) throws Exception {
        // 调用Service层发送消息
        String messageId = feishuMessageService.sendTextMessage(
                req.getReceiveId(),
                req.getReceiveIdType(),
                req.getText()
        );
        // 返回统一响应结果，携带飞书返回的消息ID
        return success(messageId);
    }

    @PostMapping("/create")
    @Operation(summary = "创建飞书消息接收记录")
    @PreAuthorize("@ss.hasPermission('ai:feishu-message:create')")
    public CommonResult<Long> createFeishuMessage(@Valid @RequestBody FeishuMessageSaveReqVO createReqVO) {
        return success(feishuMessageService.createFeishuMessage(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新飞书消息接收记录")
    @PreAuthorize("@ss.hasPermission('ai:feishu-message:update')")
    public CommonResult<Boolean> updateFeishuMessage(@Valid @RequestBody FeishuMessageSaveReqVO updateReqVO) {
        feishuMessageService.updateFeishuMessage(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除飞书消息接收记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('ai:feishu-message:delete')")
    public CommonResult<Boolean> deleteFeishuMessage(@RequestParam("id") Long id) {
        feishuMessageService.deleteFeishuMessage(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除飞书消息接收记录")
    @PreAuthorize("@ss.hasPermission('ai:feishu-message:delete')")
    public CommonResult<Boolean> deleteFeishuMessageList(@RequestParam("ids") List<Long> ids) {
        feishuMessageService.deleteFeishuMessageListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得飞书消息接收记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('ai:feishu-message:query')")
    public CommonResult<FeishuMessageRespVO> getFeishuMessage(@RequestParam("id") Long id) {
        FeishuMessageDO feishuMessage = feishuMessageService.getFeishuMessage(id);
        return success(BeanUtils.toBean(feishuMessage, FeishuMessageRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得飞书消息接收记录分页")
    @PreAuthorize("@ss.hasPermission('ai:feishu-message:query')")
    public CommonResult<PageResult<FeishuMessageRespVO>> getFeishuMessagePage(@Valid FeishuMessagePageReqVO pageReqVO) {
        PageResult<FeishuMessageDO> pageResult = feishuMessageService.getFeishuMessagePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, FeishuMessageRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出飞书消息接收记录 Excel")
    @PreAuthorize("@ss.hasPermission('ai:feishu-message:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportFeishuMessageExcel(@Valid FeishuMessagePageReqVO pageReqVO,
                                         HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<FeishuMessageDO> list = feishuMessageService.getFeishuMessagePage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "飞书消息接收记录.xls", "数据", FeishuMessageRespVO.class,
                BeanUtils.toBean(list, FeishuMessageRespVO.class));
    }

}
