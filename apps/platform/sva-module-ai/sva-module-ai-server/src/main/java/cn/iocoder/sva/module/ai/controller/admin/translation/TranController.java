package cn.iocoder.sva.module.ai.controller.admin.translation;

import cn.iocoder.sva.framework.common.pojo.CommonResult;
import cn.iocoder.sva.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranTranslateReqVO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranGlossaryDO;
import cn.iocoder.sva.module.ai.service.translation.TranService;
import cn.iocoder.sva.module.ai.service.translation.tran.model.TaskInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static cn.iocoder.sva.framework.common.pojo.CommonResult.success;

/**
 * AI翻译服务 REST API 控制器
 * <p>
 * 提供术语库管理和翻译任务相关的 API 端点：
 * <ul>
 *   <li>GET /ai/tran/glossary/visible-list - 获取当前用户可见的术语库列表</li>
 *   <li>POST /ai/tran/glossary/save - 保存术语到术语库</li>
 *   <li>POST /ai/tran/glossary/import - 导入术语库（Excel文件）</li>
 *   <li>POST /ai/tran/translate - 提交翻译任务</li>
 *   <li>GET /ai/tran/task/{taskId} - 查询任务状态</li>
 *   <li>GET /ai/tran/download/{taskId}/{fileType} - 下载结果文件</li>
 * </ul>
 */
@Tag(name = "管理后台 - AI翻译")
@Slf4j
@RestController
@RequestMapping("/ai/tran")
@Validated
public class TranController {

    @Autowired
    private TranService tranService;

    /**
     * 获取当前登录用户可见的术语库列表
     * <p>
     * 根据用户ID、用户名和目标语言查询可见的术语库。
     * 超级管理员可以看到所有术语库，普通用户只能看到自己创建的或角色可见的术语库。
     * 如果用户在该目标语言下没有术语库，会自动创建一个默认术语库。
     *
     * @param targetLanguage 目标语言（如 "en" 表示英文）
     * @return 术语库列表
     */
    @GetMapping("/glossary/visible-list")
    @Operation(summary = "获取当前登录用户可见的术语库列表")
    public CommonResult<List<TranGlossaryDO>> getVisibleGlossaryList(
            @Parameter(description = "目标语言", example = "en", required = true)
            @RequestParam("targetLanguage") String targetLanguage) {
        // 从安全上下文获取当前登录用户的ID和用户名
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        String username = SecurityFrameworkUtils.getLoginUserUsername();

        // 调用 Service 层查询可见术语库列表
        List<TranGlossaryDO> glossaryList = tranService.getVisibleGlossaryList(userId, username, targetLanguage);
        return success(glossaryList);
    }

    /**
     * 获取当前登录用户可编辑的术语库列表
     * <p>
     * 只返回用户有权限修改的术语库：
     * - 超级管理员：所有术语库
     * - 普通用户：自己创建的术语库 + 所属角色的术语库（roleId匹配）
     * <p>
     * 注意：不包含仅可见但不可修改的术语库（roleShow匹配但不属于用户角色的）
     *
     * @param targetLanguage 目标语言（如 "en" 表示英文）
     * @return 可编辑的术语库列表
     */
    @GetMapping("/glossary/editable-list")
    @Operation(summary = "获取当前登录用户可编辑的术语库列表")
    public CommonResult<List<TranGlossaryDO>> getEditableGlossaryList(
            @Parameter(description = "目标语言", example = "en", required = true)
            @RequestParam("targetLanguage") String targetLanguage) {
        // 从安全上下文获取当前登录用户的ID和用户名
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        String username = SecurityFrameworkUtils.getLoginUserUsername();

        // 调用 Service 层查询可编辑术语库列表
        List<TranGlossaryDO> glossaryList = tranService.getEditableGlossaryList(userId, username, targetLanguage);
        return success(glossaryList);
    }

    /**
     * 保存术语到术语库
     * <p>
     * 接收前端传来的术语映射（源语言->目标语言）和术语库ID，
     * 先校验当前用户是否有权限操作该术语库，然后将术语保存到数据库中。
     * 如果术语已存在则更新，否则新增。
     *
     * @param terms 术语映射，key 为源语言文本，value 为目标语言文本
     * @param glossaryId 术语库ID（必填）
     * @return 保存结果，包含成功标志、保存数量、术语库ID等信息
     */
    @PostMapping("/glossary/save")
    @Operation(summary = "保存术语到术语库")
    public CommonResult<Map<String, Object>> saveTerms(
            @RequestBody Map<String, String> terms,
            @Parameter(description = "术语库ID", required = true)
            @RequestParam("glossaryId") Long glossaryId) {
        // 从安全上下文获取当前登录用户名
        String username = SecurityFrameworkUtils.getLoginUserUsername();
        // 调用 Service 层保存术语（传入选定的术语库ID）
        Map<String, Object> result = tranService.saveTerms(terms, username, glossaryId);
        return success(result);
    }

    /**
     * 导入术语库（从 Excel 文件）
     * <p>
     * 接收用户上传的 Excel 文件，解析其中的术语数据并导入到术语库中。
     * 临时文件在处理完成后会被自动删除。
     *
     * @param file 上传的 Excel 文件
     * @return 导入结果，包含成功标志、导入数量等信息
     */
    @PostMapping("/glossary/import")
    @Operation(summary = "导入术语库(Excel文件)")
    public CommonResult<Map<String, Object>> importGlossary(
            @Parameter(description = "Excel文件") @RequestParam("file") MultipartFile file) {
        // 调用 Service 层导入术语库
        Map<String, Object> result = tranService.importGlossary(file);
        return success(result);
    }

    /**
     * 提交翻译任务
     * <p>
     * 接收用户上传的待翻译文件和翻译参数，创建翻译任务并异步执行。
     * 任务会在后台线程池中执行，不会阻塞当前请求。
     * 支持的文件类型：.docx、.xlsx/.xlsm、.pdf
     * <p>
     * 主要流程：
     * 1. 创建任务记录
     * 2. 查询术语库（如果启用术语替换）
     * 3. 上传原文件到 MinIO
     * 4. 创建文件记录到数据库
     * 5. 异步执行翻译任务
     *
     * @param file 待翻译的文件
     * @param targetLang 目标语言（如 "en"）
     * @param glossaryIds 术语库ID列表（可选，支持多选）
     * @param useGlossaryReplace 是否使用术语替换
     * @param strictFormat 是否严格保持格式
     * @param enableComparison 是否启用双语对照
     * @param enableQc 是否启用质量检查
     * @param modelId AI模型ID（可选，不传则使用默认模型）
     * @param disableCache 是否禁用缓存（1=禁用，0=启用）
     * @param roleId 聊天角色ID（可选，用于自定义翻译提示词）
     * @return 任务ID，用于后续查询任务状态和下载结果
     */
    @PostMapping("/translate")
    @Operation(summary = "提交翻译任务")
    public CommonResult<Map<String, String>> translate(
            @Parameter(description = "翻译文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "目标语言") @RequestParam("targetLang") String targetLang,
            @Parameter(description = "术语库ID列表") @RequestParam(value = "glossaryIds", required = false) List<Long> glossaryIds,
            @Parameter(description = "是否使用术语替换") @RequestParam(value = "useGlossaryReplace", defaultValue = "false") boolean useGlossaryReplace,
            @Parameter(description = "是否严格格式") @RequestParam(value = "strictFormat", defaultValue = "false") boolean strictFormat,
            @Parameter(description = "是否启用对照") @RequestParam(value = "enableComparison", defaultValue = "false") boolean enableComparison,
            @Parameter(description = "是否启用质检") @RequestParam(value = "enableQc", defaultValue = "false") boolean enableQc,
            @Parameter(description = "是否译文前置（仅双语对照模式有效）") @RequestParam(value = "translationFirst", defaultValue = "false") boolean translationFirst,
            @Parameter(description = "AI模型ID") @RequestParam(value = "modelId", required = false) Long modelId,
            @Parameter(description = "是否禁用缓存（1=禁用，0=启用）") @RequestParam(value = "disableCache", defaultValue = "0") Integer disableCache,
            @Parameter(description = "聊天角色ID") @RequestParam(value = "roleId", required = false) Long roleId) {

        // 校验文件大小（限制为 100MB）
        long maxSize = 100 * 1024 * 1024; // 100MB
        if (file.getSize() > maxSize) {
            log.warn("[translate] 文件大小超过限制: fileName={}, fileSize={} bytes, maxSize={} bytes",
                    file.getOriginalFilename(), file.getSize(), maxSize);
            return CommonResult.error(400, "文件大小不能超过 100MB，当前文件大小: " + formatFileSize(file.getSize()));
        }

        // 将请求参数封装到 VO 对象
        TranTranslateReqVO reqVO = new TranTranslateReqVO();
        reqVO.setFile(file);
        reqVO.setTargetLang(targetLang);
        reqVO.setGlossaryIds(glossaryIds);  // 设置术语库ID列表
        reqVO.setUseGlossaryReplace(useGlossaryReplace);
        reqVO.setStrictFormat(strictFormat);
        reqVO.setEnableComparison(enableComparison);
        reqVO.setEnableQc(enableQc);
        reqVO.setTranslationFirst(translationFirst);
        reqVO.setModelId(modelId);
        reqVO.setDisableCache(disableCache);
        reqVO.setRoleId(roleId);

        // 调用 Service 层提交翻译任务，返回任务ID
        String taskId = tranService.submitTranslationTask(reqVO);
        return success(Map.of("taskId", taskId));
    }

    /**
     * 查询翻译任务状态
     * <p>
     * 根据任务ID查询任务的当前状态、进度、实时翻译对等信息。
     * 前端可以轮询此接口来获取翻译进度。
     *
     * @param taskId 任务ID
     * @return 任务信息，包含状态、进度、翻译对等
     */
    @GetMapping("/task/{taskId}")
    @Operation(summary = "查询任务状态")
    public CommonResult<TaskInfo> getTask(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        // 调用 Service 层查询任务状态
        TaskInfo task = tranService.getTaskStatus(taskId);
        return success(task);
    }

    /**
     * 下载翻译结果文件
     * <p>
     * 根据任务ID和文件类型下载对应的结果文件。
     * 支持下载的文件类型：
     * - file: 翻译后的文档
     * - excel: 对照表（原文和译文对照）
     * - qc: 质量检查报告
     * - contrast: 双语对照文档
     * <p>
     * 该方法会生成 MinIO 的预签名 URL，然后重定向到该 URL 进行下载。
     *
     * @param taskId 任务ID
     * @param fileType 文件类型（file/excel/qc/contrast）
     * @return 重定向到 MinIO 预签名 URL 的响应
     */
    @GetMapping("/download/{taskId}/{fileType}")
    @Operation(summary = "下载结果文件")
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "任务ID") @PathVariable String taskId,
            @Parameter(description = "文件类型(file/excel/qc/contrast)") @PathVariable String fileType) {
        // 调用 Service 层处理文件下载
        return tranService.downloadFile(taskId, fileType);
    }

    /**
     * 格式化文件大小为可读字符串
     *
     * @param size 文件大小（字节）
     * @return 格式化后的文件大小字符串
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
