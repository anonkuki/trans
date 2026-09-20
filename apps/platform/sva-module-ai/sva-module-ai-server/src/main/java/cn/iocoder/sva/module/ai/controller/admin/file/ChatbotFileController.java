package cn.iocoder.sva.module.ai.controller.admin.file;

import cn.iocoder.sva.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.sva.framework.common.pojo.CommonResult;
import cn.iocoder.sva.framework.common.pojo.PageParam;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;
import cn.iocoder.sva.framework.excel.core.util.ExcelUtils;
import cn.iocoder.sva.module.ai.controller.admin.file.vo.ChatbotFilePageReqVO;
import cn.iocoder.sva.module.ai.controller.admin.file.vo.ChatbotFileRespVO;
import cn.iocoder.sva.module.ai.controller.admin.file.vo.ChatbotFileSaveReqVO;
import cn.iocoder.sva.module.ai.dal.dataobject.file.ChatbotFileDO;
import cn.iocoder.sva.module.ai.service.file.ChatbotFileService;
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

@Tag(name = "管理后台 - 对话文件记录")
@RestController
@RequestMapping("/ai/chatbot-file")
@Validated
public class ChatbotFileController {

    @Resource
    private ChatbotFileService chatbotFileService;

    @PostMapping("/create")
    @Operation(summary = "创建对话文件记录")
    @PreAuthorize("@ss.hasPermission('ai:chatbot-file:create')")
    public CommonResult<Long> createChatbotFile(@Valid @RequestBody ChatbotFileSaveReqVO createReqVO) {
        return success(chatbotFileService.createChatbotFile(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新对话文件记录")
    @PreAuthorize("@ss.hasPermission('ai:chatbot-file:update')")
    public CommonResult<Boolean> updateChatbotFile(@Valid @RequestBody ChatbotFileSaveReqVO updateReqVO) {
        chatbotFileService.updateChatbotFile(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除对话文件记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('ai:chatbot-file:delete')")
    public CommonResult<Boolean> deleteChatbotFile(@RequestParam("id") Long id) {
        chatbotFileService.deleteChatbotFile(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除对话文件记录")
                @PreAuthorize("@ss.hasPermission('ai:chatbot-file:delete')")
    public CommonResult<Boolean> deleteChatbotFileList(@RequestParam("ids") List<Long> ids) {
        chatbotFileService.deleteChatbotFileListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得对话文件记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('ai:chatbot-file:query')")
    public CommonResult<ChatbotFileRespVO> getChatbotFile(@RequestParam("id") Long id) {
        ChatbotFileDO chatbotFile = chatbotFileService.getChatbotFile(id);
        return success(BeanUtils.toBean(chatbotFile, ChatbotFileRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得对话文件记录分页")
    @PreAuthorize("@ss.hasPermission('ai:chatbot-file:query')")
    public CommonResult<PageResult<ChatbotFileRespVO>> getChatbotFilePage(@Valid ChatbotFilePageReqVO pageReqVO) {
        PageResult<ChatbotFileDO> pageResult = chatbotFileService.getChatbotFilePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ChatbotFileRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出对话文件记录 Excel")
    @PreAuthorize("@ss.hasPermission('ai:chatbot-file:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportChatbotFileExcel(@Valid ChatbotFilePageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ChatbotFileDO> list = chatbotFileService.getChatbotFilePage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "对话文件记录.xls", "数据", ChatbotFileRespVO.class,
                        BeanUtils.toBean(list, ChatbotFileRespVO.class));
    }

}