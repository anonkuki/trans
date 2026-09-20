package cn.iocoder.sva.module.ai.service.translation;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.sva.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranTranslateReqVO;
import cn.iocoder.sva.module.ai.dal.dataobject.model.AiChatRoleDO;
import cn.iocoder.sva.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranFileDO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranGlossaryDO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranGlossaryItemDO;
import cn.iocoder.sva.module.ai.service.file.FileService;
import cn.iocoder.sva.module.ai.service.model.AiChatRoleService;
import cn.iocoder.sva.module.ai.service.model.AiModelService;
import cn.iocoder.sva.module.ai.service.translation.helper.FileHelper;
import cn.iocoder.sva.module.ai.service.translation.helper.GlossaryHelper;
import cn.iocoder.sva.module.ai.service.translation.tran.*;
import cn.iocoder.sva.module.ai.service.translation.tran.config.TransDocProperties;
import cn.iocoder.sva.module.ai.service.translation.tran.context.AiModelContext;
import cn.iocoder.sva.module.ai.service.translation.tran.context.ChatModelContext;
import cn.iocoder.sva.module.ai.service.translation.tran.context.PromptContext;
import cn.iocoder.sva.module.ai.service.translation.tran.context.RoleContext;
import cn.iocoder.sva.module.ai.service.translation.tran.context.TranslationCacheContext;
import cn.iocoder.sva.module.ai.service.translation.tran.model.TaskInfo;
import cn.iocoder.sva.module.ai.service.translation.tran.model.TranslationPair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * AI翻译服务实现类
 * <p>
 * 作为翻译模块的主流程服务，协调各个辅助类完成翻译任务的核心业务逻辑。
 * 主要职责包括：
 * 1. 提交翻译任务并异步执行
 * 2. 查询任务状态
 * 3. 下载翻译结果文件
 * 4. 术语库管理（查询、保存、导入）
 * <p>
 * 翻译任务执行流程：
 * 1. 创建任务记录 -> 2. 查询术语库 -> 3. 上传原文件到MinIO -> 4. 创建文件记录
 * -> 5. 异步执行翻译 -> 6. 调用大模型翻译 -> 7. 上传结果文件 -> 8. 更新文件记录
 */
@Service
@Validated
@Slf4j
public class TranServiceImpl implements TranService {

    @Autowired
    private GlossaryHelper glossaryHelper;

    @Autowired
    private FileHelper fileHelper;

    @Autowired
    private TaskManagerService taskManager;

    @Autowired
    private DocxTranslationService docxTranslationService;

    @Autowired
    private ExcelTranslationService excelTranslationService;

    @Autowired
    private PdfTranslationService pdfTranslationService;

    @Autowired
    private TransDocProperties properties;

    @Autowired
    private FileService fileService;

    @Autowired
    private AiModelService aiModelService;

    @Autowired
    private AiChatRoleService chatRoleService;

    @Autowired
    private TranGlossaryItemService tranGlossaryItemService;

    @Autowired
    private Executor taskExecutor;

    /**
     * 提交翻译任务
     * <p>
     * 这是翻译功能的主入口方法，负责接收翻译请求并启动异步翻译任务。
     * <p>
     * 主要流程：
     * 1. 创建任务记录（在内存中）
     * 2. 如果启用术语替换，从数据库查询术语库
     * 3. 读取上传文件内容
     * 4. 上传原文件到 MinIO 存储
     * 5. 创建文件记录到数据库（tran_file 表）
     * 6. 异步执行翻译任务（在后台线程池中）
     *
     * @param reqVO 翻译请求参数，包含文件、目标语言、术语库ID、翻译选项等
     * @return 任务ID，用于后续查询任务状态和下载结果
     */
    @Override
    public String submitTranslationTask(TranTranslateReqVO reqVO) {
        // 创建新的翻译任务，生成唯一的任务ID
        String taskId = taskManager.createTask();
        // 设置是否启用质量检查
        taskManager.setEnableQc(taskId, reqVO.isEnableQc());

        try {
            // 如果启用了术语替换且指定了术语库ID，则从数据库查询术语库条目
            List<TranGlossaryItemDO> glossaryItems = null;
            if (reqVO.getGlossaryId() != null && reqVO.isUseGlossaryReplace()) {
                // 数据库查询：根据术语库ID查询所有术语条目
                glossaryItems = tranGlossaryItemService.getTranGlossaryItemListByGlossaryId(reqVO.getGlossaryId());
                log.info("[translate][taskId={}] 实时查询术语库 {} 的 {} 条术语",
                        taskId, reqVO.getGlossaryId(), glossaryItems != null ? glossaryItems.size() : 0);

                // 打印术语详情，用于调试
//                if (glossaryItems != null && !glossaryItems.isEmpty()) {
//                    log.info("[translate][taskId={}] ===== 术语库详情开始 =====", taskId);
//                    glossaryItems.forEach(item ->
//                        log.info("[术语项] source='{}', target='{}'",
//                            item.getSourceLanguage(), item.getTargetLanguage())
//                    );
//                    log.info("[translate][taskId={}] ===== 术语库详情结束 =====", taskId);
//                }
            }

            // 读取上传的文件
            MultipartFile file = reqVO.getFile();
            String origName = file.getOriginalFilename();
            String ext = getFileExtension(origName);

            // 读取文件内容为字节数组
            byte[] fileContent = IoUtil.readBytes(file.getInputStream());

            // 文件上传：将原文件上传到 MinIO 对象存储
            String minioUrl = fileHelper.uploadToMinio(fileContent, origName, "translation/input");
            log.info("[translate][taskId={}] 原文件已上传到 MinIO: {}", taskId, minioUrl);

            // 获取当前登录用户名
            String username = SecurityFrameworkUtils.getLoginUserUsername();
            // 数据库操作：创建文件记录到 tran_file 表
            Long fileId = fileHelper.createFileRecord(taskId, origName, minioUrl, ext, file.getSize(), reqVO.getTargetLang(), username);

            // 将文件ID关联到任务
            taskManager.setFileId(taskId, fileId);
            // 更新任务状态为 pending（等待执行）
            taskManager.updateStatus(taskId, "pending");

            // 异步执行翻译任务（在后台线程池中运行，不阻塞当前请求）
            executeTranslationAsync(taskId, fileId, fileContent, origName, reqVO, glossaryItems, minioUrl, ext);

        } catch (Exception e) {
            log.error("提交翻译任务失败", e);
            taskManager.setError(taskId, e.getMessage());
        }

        return taskId;
    }

    /**
     * 查询翻译任务状态
     * <p>
     * 从内存中的任务管理器获取任务的当前状态信息，
     * 包括任务状态、进度、实时翻译对、下载链接等。
     *
     * @param taskId 任务ID
     * @return 任务信息对象
     */
    @Override
    public TaskInfo getTaskStatus(String taskId) {
        return taskManager.getTask(taskId);
    }

    /**
     * 下载翻译结果文件
     * <p>
     * 根据任务ID和文件类型，从数据库查询文件记录，获取对应的 MinIO URL，
     * 然后生成预签名 URL 并重定向到该 URL 进行下载。
     * <p>
     * 支持的文件类型：
     * - file: 翻译后的文档
     * - excel: 对照表（原文和译文对照的 Excel）
     * - qc: 质量检查报告（TXT格式）
     * - contrast: 双语对照文档
     *
     * @param taskId 任务ID
     * @param fileType 文件类型（file/excel/qc/contrast）
     * @return HTTP 响应，重定向到 MinIO 预签名 URL
     */
    @Override
    public ResponseEntity<Resource> downloadFile(String taskId, String fileType) {
        // 从任务管理器获取任务信息
        TaskInfo task = taskManager.getTask(taskId);

        // 校验任务是否存在
        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        // 校验任务是否已完成
        if (!"completed".equals(task.getStatus())) {
            return ResponseEntity.badRequest().build();
        }

        // 获取任务关联的文件记录ID
        Long fileId = task.getFileId();
        if (fileId == null) {
            log.error("[downloadFile] 任务 {} 未关联文件记录", taskId);
            return ResponseEntity.notFound().build();
        }

        // 数据库查询：根据文件ID查询文件记录
        TranFileDO fileDO = fileHelper.getFileRecord(fileId);
        if (fileDO == null) {
            log.error("[downloadFile] 文件记录不存在, fileId={}", fileId);
            return ResponseEntity.notFound().build();
        }

        // 根据文件类型获取对应的 MinIO URL 和文件名
        String minioUrl = getMinioUrlByFileType(fileDO, fileType);
        String filename = getFilenameByFileType(fileDO, fileType);

        // 校验 MinIO URL 是否存在
        if (minioUrl == null || minioUrl.isEmpty()) {
            log.error("[downloadFile] 文件类型 {} 的 MinIO URL 为空, taskId={}, fileId={}", fileType, taskId, fileId);
            return ResponseEntity.notFound().build();
        }

        try {
            // 生成 MinIO 预签名 URL（有效期1小时），用于临时访问私有文件
            String presignedUrl = fileService.presignGetUrl(minioUrl, 3600);
            log.info("[downloadFile] 生成预签名 URL: {}, 原始 URL: {}", presignedUrl, minioUrl);

            // 返回 302 重定向响应，让浏览器直接从 MinIO 下载文件
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, presignedUrl)
                    .build();

        } catch (Exception e) {
            log.error("[downloadFile] 生成预签名 URL 失败, taskId={}, fileType={}, minioUrl={}",
                    taskId, fileType, minioUrl, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取当前登录用户可见的术语库列表
     * <p>
     * 委托给 GlossaryHelper 处理，根据用户权限过滤术语库。
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param targetLanguage 目标语言
     * @return 术语库列表
     */
    @Override
    public List<TranGlossaryDO> getVisibleGlossaryList(Long userId, String username, String targetLanguage) {
        return glossaryHelper.getVisibleGlossaryList(userId, username, targetLanguage);
    }

    /**
     * 保存术语到术语库
     * <p>
     * 接收术语映射和术语库ID，先校验用户是否有权限操作该术语库，
     * 然后将术语保存到数据库。如果术语已存在则更新，否则新增。
     *
     * @param terms 术语映射（key=源语言, value=目标语言）
     * @param username 用户名
     * @param glossaryId 术语库ID（必填）
     * @return 保存结果
     */
    @Override
    public Map<String, Object> saveTerms(Map<String, String> terms, String username, Long glossaryId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 校验术语映射是否为空
            if (terms == null || terms.isEmpty()) {
                result.put("success", false);
                result.put("message", "没有术语需要保存");
                return result;
            }

            // 校验术语库ID是否为空
            if (glossaryId == null) {
                result.put("success", false);
                result.put("message", "术语库ID不能为空");
                return result;
            }

            log.info("[saveTerms] 保存术语: username={}, glossaryId={}, termCount={}", 
                    username, glossaryId, terms.size());

            // 校验用户是否有权限操作该术语库
            // 权限校验逻辑：用户角色包含术语库的所属角色，或者用户是术语库的所属人员
            TranGlossaryDO glossary = 
                    glossaryHelper.getGlossaryById(glossaryId);
            
            if (glossary == null) {
                result.put("success", false);
                result.put("message", "术语库不存在");
                return result;
            }

            // 执行权限校验
            boolean hasPermission = glossaryHelper.checkGlossaryPermission(glossaryId, username);
            if (!hasPermission) {
                log.warn("[saveTerms] 用户无权限操作该术语库: username={}, glossaryId={}, roleId={}, owner={}", 
                        username, glossaryId, glossary.getRoleId(), glossary.getUsername());
                result.put("success", false);
                result.put("message", "权限不足，不能保存到该术语库");
                return result;
            }

            // 数据库操作：批量保存或更新术语到术语库
            int savedCount = glossaryHelper.saveTermsToGlossary(glossaryId, terms);

            result.put("success", true);
            result.put("count", savedCount);
            result.put("glossaryId", glossaryId);
            result.put("message", "术语保存成功");
            
            log.info("[saveTerms] 术语保存成功: glossaryId={}, savedCount={}", glossaryId, savedCount);

        } catch (Exception e) {
            log.error("[saveTerms] 保存术语失败", e);
            result.put("success", false);
            result.put("message", "保存失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 导入术语库（从 Excel 文件）
     * <p>
     * 接收用户上传的 Excel 文件，将其保存到临时目录，
     * 然后解析文件内容并导入到术语库中。
     * 处理完成后会自动删除临时文件。
     *
     * @param file 上传的 Excel 文件
     * @return 导入结果
     */
    @Override
    public Map<String, Object> importGlossary(MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        Path tmpPath = null;

        try {
            // 生成临时文件名
            String filename = "import_" + System.currentTimeMillis() + ".xlsx";
            // 构建临时文件路径
            tmpPath = Paths.get(properties.getTempDir()).resolve(filename);
            // 将上传的文件保存到临时目录
            file.transferTo(tmpPath.toFile());

            // TODO: 调用 GlossaryService 的导入方法解析 Excel 文件
            // int count = glossaryService.importFromExcel(tmpPath.toString());
            int count = 0;

            if (count > 0) {
                result.put("success", true);
                result.put("count", count);
            } else {
                result.put("success", false);
                result.put("message", "未识别到有效术语条目");
            }

        } catch (Exception e) {
            log.error("导入术语库失败", e);
            result.put("success", false);
            result.put("message", "导入失败: " + e.getMessage());
        } finally {
            // 清理临时文件
            if (tmpPath != null) {
                try {
                    Files.deleteIfExists(tmpPath);
                } catch (Exception ignored) {}
            }
        }

        return result;
    }

    /**
     * 后台翻译任务（供后端其他功能调用）
     * <p>
     * 执行完整的翻译流程，同步等待翻译完成。
     * 与前端接口的区别：
     * 1. 不使用异步执行，直接同步等待完成
     * 2. 不需要创建任务记录用于前端轮询
     * 3. 只返回成功或失败的结果
     * 4. 支持从任意来源获取文件内容（MinIO、本地文件、网络等）
     * <p>
     * 主要流程：
     * 1. 如果启用术语替换，从数据库查询术语库
     * 2. 读取文件内容
     * 3. 上传原文件到 MinIO 存储
     * 4. 创建文件记录到数据库（tran_file 表）
     * 5. 同步执行翻译任务
     * 6. 上传结果文件到 MinIO
     * 7. 更新数据库记录
     *
     * @param fileContent 文件内容（字节数组）
     * @param fileName 文件名（包含扩展名，如 "document.docx"）
     * @param targetLang 目标语言（如 "en"）
     * @param glossaryId 术语库ID（可选）
     * @param useGlossaryReplace 是否使用术语替换
     * @param strictFormat 是否严格保持格式
     * @param enableComparison 是否启用双语对照
     * @param enableQc 是否启用质量检查
     * @param modelId AI模型ID（可选，不传则使用默认模型）
     * @param disableCache 是否禁用缓存（1=禁用，0=启用）
     * @return true=翻译成功，false=翻译失败
     */
    @Override
    public boolean executeTranslationTask(byte[] fileContent, String fileName, String targetLang, Long glossaryId,
                                          boolean useGlossaryReplace, boolean strictFormat,
                                          boolean enableComparison, boolean enableQc,
                                          Long modelId, Integer disableCache, Long roleId) {
        // 获取当前登录用户名（如果没有登录用户，使用系统默认）
        String username = SecurityFrameworkUtils.getLoginUserUsername();
        if (StrUtil.isBlank(username)) {
            username = "system";
            log.warn("[executeTranslationTask] 无法获取登录用户，使用默认用户名: {}", username);
        }
        
        return executeTranslationTask(fileContent, fileName, targetLang, glossaryId,
                useGlossaryReplace, strictFormat, enableComparison, enableQc,
                modelId, disableCache, username, roleId);
    }

    @Override
    public boolean executeTranslationTask(byte[] fileContent, String fileName, String targetLang, Long glossaryId,
                                          boolean useGlossaryReplace, boolean strictFormat,
                                          boolean enableComparison, boolean enableQc,
                                          Long modelId, Integer disableCache, String username, Long roleId) {
        String taskId = null;
        Long fileId = null;

        try {
            // 创建新的翻译任务，生成唯一的任务ID
            taskId = taskManager.createTask();
            taskManager.setEnableQc(taskId, enableQc);

            // 如果启用了术语替换且指定了术语库ID，则从数据库查询术语库条目
            List<TranGlossaryItemDO> glossaryItems = null;
            if (glossaryId != null && useGlossaryReplace) {
                // 数据库查询：根据术语库ID查询所有术语条目
                glossaryItems = tranGlossaryItemService.getTranGlossaryItemListByGlossaryId(glossaryId);
                log.info("[executeTranslationTask][taskId={}] 实时查询术语库 {} 的 {} 条术语",
                        taskId, glossaryId, glossaryItems != null ? glossaryItems.size() : 0);
            }

            // 获取文件扩展名
            String ext = getFileExtension(fileName);

            // 文件上传：将原文件上传到 MinIO 对象存储
            String minioUrl = fileHelper.uploadToMinio(fileContent, fileName, "translation/input");
            log.info("[executeTranslationTask][taskId={}] 原文件已上传到 MinIO: {}", taskId, minioUrl);

            // 校验并设置用户名
            if (StrUtil.isBlank(username)) {
                username = "system";
                log.warn("[executeTranslationTask][taskId={}] 用户名为空，使用默认用户名: {}", taskId, username);
            }
            log.info("[executeTranslationTask][taskId={}] 使用用户名: {}", taskId, username);

            // 数据库操作：创建文件记录到 tran_file 表
            fileId = fileHelper.createFileRecord(taskId, fileName, minioUrl, ext, (long) fileContent.length, targetLang, username);

            // 将文件ID关联到任务
            taskManager.setFileId(taskId, fileId);
            // 更新任务状态为 pending（等待执行）
            taskManager.updateStatus(taskId, "pending");

            // 设置上下文（如果需要）
            boolean shouldDisableCache = disableCache != null && disableCache == 1;
            if (modelId != null) {
                try {
                    // 获取模型信息
                    AiModelDO modelDO = aiModelService.getModel(modelId);
                    ChatModel chatModel = aiModelService.getChatModel(modelId);
                    
                    // 设置 ChatModelContext
                    ChatModelContext.set(chatModel);
                    
                    // 设置 AiModelContext（包含模型代码）
                    AiModelContext.ModelInfo modelInfo = 
                        new AiModelContext.ModelInfo();
                    modelInfo.setModelId(modelId);
                    modelInfo.setModelName(modelDO.getName());
                    modelInfo.setModelCode(modelDO.getModel());  // 关键：设置实际的模型代码，如 deepseek-v3
                    modelInfo.setChatModel(chatModel);
                    AiModelContext.set(modelInfo);
                    
                    log.info("[executeTranslationTask][taskId={}] 成功设置自定义模型到 ThreadLocal, modelId={}, modelName={}, modelCode={}", 
                            taskId, modelId, modelDO.getName(), modelDO.getModel());
                } catch (Exception e) {
                    log.error("[executeTranslationTask][taskId={}] 获取自定义模型失败, modelId={}, 将使用默认模型", taskId, modelId, e);
                }
            }

            // 设置角色上下文
            if (roleId != null) {
                RoleContext.set(roleId);
                log.info("[executeTranslationTask][taskId={}] 设置角色ID: {}", taskId, roleId);
            }

            TranslationCacheContext.set(shouldDisableCache);
            log.info("[executeTranslationTask][taskId={}] 设置缓存禁用标志: {}", taskId, shouldDisableCache);

            try {
                // 构建后台翻译参数对象（不使用 MultipartFile）
                BackendTranslationParams params = new BackendTranslationParams();
                params.setTargetLang(targetLang);
                params.setGlossaryId(glossaryId);
                params.setUseGlossaryReplace(useGlossaryReplace);
                params.setStrictFormat(strictFormat);
                params.setEnableComparison(enableComparison);
                params.setEnableQc(enableQc);
                params.setModelId(modelId);
                params.setDisableCache(disableCache);
                params.setRoleId(roleId);

                // 同步执行翻译任务的核心逻辑
                runTranslationWithParams(taskId, fileId, fileContent, fileName, params,
                        glossaryItems, minioUrl, ext);
            } finally {
                TranslationCacheContext.clear();
                ChatModelContext.clear();
                AiModelContext.clear();
                RoleContext.clear();
                log.debug("[executeTranslationTask][taskId={}] 已清理所有 ThreadLocal", taskId);
            }

            // 检查任务是否成功完成
            TaskInfo taskInfo = taskManager.getTask(taskId);
            if (taskInfo != null && "completed".equals(taskInfo.getStatus())) {
                log.info("[executeTranslationTask][taskId={}] 翻译任务成功完成", taskId);
                return true;
            } else {
                log.error("[executeTranslationTask][taskId={}] 翻译任务失败，状态: {}", 
                        taskId, taskInfo != null ? taskInfo.getStatus() : "unknown");
                return false;
            }

        } catch (Exception e) {
            log.error("[executeTranslationTask][taskId={}] 翻译任务执行异常", taskId, e);
            if (taskId != null) {
                taskManager.setError(taskId, e.getMessage());
            }
            if (fileId != null) {
                fileHelper.updateFileRecord(fileId, null, null, null, null, 2);
            }
            return false;

        } finally {
            // 注意：临时文件会在 runTranslationWithParams 的 finally 块中清理
        }
    }

    /**
     * 后台翻译任务参数封装类
     * <p>
     * 用于后台调用翻译服务时传递参数，避免依赖 MultipartFile
     */
    private static class BackendTranslationParams {
        private String targetLang;
        private Long glossaryId;
        private boolean useGlossaryReplace;
        private boolean strictFormat;
        private boolean enableComparison;
        private boolean enableQc;
        private Long modelId;
        private Integer disableCache;
        private Long roleId;

        public String getTargetLang() {
            return targetLang;
        }

        public void setTargetLang(String targetLang) {
            this.targetLang = targetLang;
        }

        public Long getGlossaryId() {
            return glossaryId;
        }

        public void setGlossaryId(Long glossaryId) {
            this.glossaryId = glossaryId;
        }

        public boolean isUseGlossaryReplace() {
            return useGlossaryReplace;
        }

        public void setUseGlossaryReplace(boolean useGlossaryReplace) {
            this.useGlossaryReplace = useGlossaryReplace;
        }

        public boolean isStrictFormat() {
            return strictFormat;
        }

        public void setStrictFormat(boolean strictFormat) {
            this.strictFormat = strictFormat;
        }

        public boolean isEnableComparison() {
            return enableComparison;
        }

        public void setEnableComparison(boolean enableComparison) {
            this.enableComparison = enableComparison;
        }

        public boolean isEnableQc() {
            return enableQc;
        }

        public void setEnableQc(boolean enableQc) {
            this.enableQc = enableQc;
        }

        public Long getModelId() {
            return modelId;
        }

        public void setModelId(Long modelId) {
            this.modelId = modelId;
        }

        public Integer getDisableCache() {
            return disableCache;
        }

        public void setDisableCache(Integer disableCache) {
            this.disableCache = disableCache;
        }

        public Long getRoleId() {
            return roleId;
        }

        public void setRoleId(Long roleId) {
            this.roleId = roleId;
        }
    }

    /**
     * 获取文件扩展名
     * <p>
     * 从文件名中提取扩展名，如果文件名不包含扩展名则返回默认的 .docx
     *
     * @param filename 文件名
     * @return 文件扩展名（小写）
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".docx";
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    /**
     * 异步执行翻译任务
     * <p>
     * 在后台线程池中执行翻译任务，不会阻塞主线程。
     * 在异步线程中设置 ChatModelContext（AI模型上下文）和 TranslationCacheContext（缓存控制上下文），
     * 确保翻译任务使用正确的配置。
     *
     * @param taskId 任务ID
     * @param fileId 文件记录ID
     * @param inputFileContent 输入文件内容（字节数组）
     * @param origName 原始文件名
     * @param reqVO 翻译请求参数
     * @param glossaryItems 术语库条目列表
     * @param sourceFileUrl 原文件的 MinIO URL
     * @param fileExt 文件扩展名
     */
    private void executeTranslationAsync(String taskId, Long fileId, byte[] inputFileContent,
                                         String origName, TranTranslateReqVO reqVO,
                                         List<TranGlossaryItemDO> glossaryItems,
                                         String sourceFileUrl, String fileExt) {
        boolean shouldDisableCache = reqVO.getDisableCache() != null && reqVO.getDisableCache() == 1;

        CompletableFuture.runAsync(() -> {
            if (reqVO.getModelId() != null) {
                try {
                    // 获取模型信息
                    AiModelDO modelDO = aiModelService.getModel(reqVO.getModelId());
                    ChatModel chatModel = aiModelService.getChatModel(reqVO.getModelId());
                    
                    // 设置 ChatModelContext
                    ChatModelContext.set(chatModel);
                    
                    // 设置 AiModelContext（包含模型代码）
                    AiModelContext.ModelInfo modelInfo = 
                        new AiModelContext.ModelInfo();
                    modelInfo.setModelId(reqVO.getModelId());
                    modelInfo.setModelName(modelDO.getName());
                    modelInfo.setModelCode(modelDO.getModel());  // 关键：设置实际的模型代码，如 deepseek-v3
                    modelInfo.setChatModel(chatModel);
                    AiModelContext.set(modelInfo);
                    
                    log.info("[runAsync][taskId={}] 成功设置自定义模型到 ThreadLocal, modelId={}, modelName={}, modelCode={}", 
                            taskId, reqVO.getModelId(), modelDO.getName(), modelDO.getModel());
                } catch (Exception e) {
                    log.error("[runAsync][taskId={}] 获取自定义模型失败, modelId={}, 将使用默认模型", taskId, reqVO.getModelId(), e);
                }
            }

            // 设置角色上下文
            if (reqVO.getRoleId() != null) {
                RoleContext.set(reqVO.getRoleId());
                log.info("[runAsync][taskId={}] 设置角色ID: {}", taskId, reqVO.getRoleId());
            }

            // 构建并缓存提示词（只查询一次数据库）
            String systemPrompt = buildSystemPrompt(reqVO.getTargetLang(), reqVO.getRoleId());
            PromptContext.set(systemPrompt);
            log.info("[runAsync][taskId={}] 已缓存系统提示词，长度: {}, 前100字符: {}", 
                    taskId, systemPrompt.length(), 
                    systemPrompt.substring(0, Math.min(100, systemPrompt.length())));

            TranslationCacheContext.set(shouldDisableCache);
            log.info("[runAsync][taskId={}] 设置缓存禁用标志: {}", taskId, shouldDisableCache);

            try {
                runTranslation(taskId, fileId, inputFileContent, origName, reqVO,
                        glossaryItems, sourceFileUrl, fileExt);
            } finally {
                TranslationCacheContext.clear();
                ChatModelContext.clear();
                AiModelContext.clear();
                RoleContext.clear();
                PromptContext.clear();
                log.debug("[runAsync][taskId={}] 已清理所有 ThreadLocal", taskId);
            }
        }, taskExecutor);
    }

    /**
     * 执行翻译任务的核心逻辑（前端接口调用版本）
     * <p>
     * 这是翻译任务的主要执行方法，负责：
     * 1. 准备临时文件
     * 2. 根据文件类型调用对应的翻译服务（DOCX/Excel/PDF）
     * 3. 调用大模型进行翻译
     * 4. 生成对照表和质检报告
     * 5. 上传结果文件到 MinIO
     * 6. 更新数据库记录
     * <p>
     * 支持的文档处理类型：
     * - DOCX: 使用 DocxTranslationService 处理
     * - XLSX/XLSM: 使用 ExcelTranslationService 处理
     * - PDF: 使用 PdfTranslationService 处理（转换为 DOCX）
     *
     * @param taskId 任务ID
     * @param fileId 文件记录ID
     * @param inputFileContent 输入文件内容
     * @param origName 原始文件名
     * @param reqVO 翻译请求参数
     * @param glossaryItems 术语库条目列表
     * @param sourceFileUrl 原文件的 MinIO URL
     * @param fileExt 文件扩展名
     */
    private void runTranslation(String taskId, Long fileId, byte[] inputFileContent,
                                String origName, TranTranslateReqVO reqVO,
                                List<TranGlossaryItemDO> glossaryItems,
                                String sourceFileUrl, String fileExt) {
        // 将 TranTranslateReqVO 转换为 BackendTranslationParams
        BackendTranslationParams params = new BackendTranslationParams();
        params.setTargetLang(reqVO.getTargetLang());
        params.setGlossaryId(reqVO.getGlossaryId());
        params.setUseGlossaryReplace(reqVO.isUseGlossaryReplace());
        params.setStrictFormat(reqVO.isStrictFormat());
        params.setEnableComparison(reqVO.isEnableComparison());
        params.setEnableQc(reqVO.isEnableQc());
        params.setModelId(reqVO.getModelId());
        params.setDisableCache(reqVO.getDisableCache());
        params.setRoleId(reqVO.getRoleId());

        // 调用统一的翻译执行方法
        runTranslationWithParams(taskId, fileId, inputFileContent, origName, params,
                glossaryItems, sourceFileUrl, fileExt);
    }

    /**
     * 执行翻译任务的核心逻辑（统一入口）
     * <p>
     * 这是翻译任务的主要执行方法，负责：
     * 1. 准备临时文件
     * 2. 根据文件类型调用对应的翻译服务（DOCX/Excel/PDF）
     * 3. 调用大模型进行翻译
     * 4. 生成对照表和质检报告
     * 5. 上传结果文件到 MinIO
     * 6. 更新数据库记录
     * <p>
     * 支持的文档处理类型：
     * - DOCX: 使用 DocxTranslationService 处理
     * - XLSX/XLSM: 使用 ExcelTranslationService 处理
     * - PDF: 使用 PdfTranslationService 处理（转换为 DOCX）
     *
     * @param taskId 任务ID
     * @param fileId 文件记录ID
     * @param inputFileContent 输入文件内容
     * @param origName 原始文件名
     * @param params 翻译参数（通用参数对象）
     * @param glossaryItems 术语库条目列表
     * @param sourceFileUrl 原文件的 MinIO URL
     * @param fileExt 文件扩展名
     */
    private void runTranslationWithParams(String taskId, Long fileId, byte[] inputFileContent,
                                          String origName, BackendTranslationParams params,
                                          List<TranGlossaryItemDO> glossaryItems,
                                          String sourceFileUrl, String fileExt) {
        long startTime = System.currentTimeMillis();
        Path tmpInputPath = null;
        Path tmpOutputPath = null;
        Path tmpContrastPath = null;
        Path tmpComparePath = null;
        Path tmpQcPath = null;

        try {
            // 更新任务状态为 running（执行中）
            taskManager.updateStatus(taskId, "running");

            // 确定文件扩展名和基础文件名
            String ext = fileExt != null ? fileExt : getFileExtension(origName);
            String origBase = getBaseName(origName);

            // 创建临时输入文件
            tmpInputPath = Paths.get(properties.getTempDir()).resolve("input_" + System.currentTimeMillis() + ext);
            Files.write(tmpInputPath, inputFileContent);

            // 创建临时输出文件路径
            String outFilename = "output_" + System.currentTimeMillis() + ext;
            tmpOutputPath = Paths.get(properties.getTempDir()).resolve(outFilename);

            // 定义进度回调函数，用于实时更新任务进度
            ProgressCallback progressCallback = (current, total, message) -> {
                int pct = total > 0 ? (int) (current * 100.0 / total) : 0;
                taskManager.updateProgress(taskId, pct, message);
            };

            // 定义文本回调函数，用于实时收集翻译对
            TextCallback textCallback = (source, translated, status) -> {
                TranslationPair pair = new TranslationPair();
                pair.setSource(source);
                pair.setTarget(translated);
                pair.setStatus(status);
                taskManager.addRealtimePair(taskId, pair);
            };

            // 构建术语映射表（源语言->目标语言）
            Map<String, String> glossaryMap = glossaryHelper.buildGlossaryMap(glossaryItems);

            // 根据文件类型调用对应的翻译服务进行文档处理和大模型调用
            String outputPath;
            String contrastPath = null;
            DocxTranslationService.TranslationResult docxResult = null;
            ExcelTranslationService.ExcelResult excelResult = null;
            PdfTranslationService.PdfResult pdfResult = null;

            if (".docx".equals(ext)) {
                // 文档处理：调用 DOCX 翻译服务
                // 内部会解析 DOCX 结构，提取文本，调用大模型翻译，然后重建文档
                docxResult = docxTranslationService.processDocument(
                        tmpInputPath.toString(), tmpOutputPath.toString(), params.getTargetLang(),
                        params.isUseGlossaryReplace(), glossaryMap, progressCallback, textCallback,
                        params.isStrictFormat(), params.isEnableQc(), params.isEnableComparison()
                );
                outputPath = docxResult.getOutputPath();

                // 如果启用了对照模式，获取对照文档路径
                if (params.isEnableComparison() && docxResult.getContrastPath() != null) {
                    contrastPath = docxResult.getContrastPath();
                }
            } else if (".xlsx".equals(ext) || ".xlsm".equals(ext)) {
                // 文档处理：调用 Excel 翻译服务
                // 内部会解析 Excel 表格，逐行翻译，保持公式和格式
                excelResult = excelTranslationService.processExcel(
                        tmpInputPath.toString(), tmpOutputPath.toString(), params.getTargetLang(),
                        params.isUseGlossaryReplace(), glossaryMap, progressCallback, textCallback,
                        params.isEnableQc()
                );
                outputPath = excelResult.getOutputPath();
            } else if (".pdf".equals(ext)) {
                // 文档处理：调用 PDF 翻译服务
                // 内部会将 PDF 转换为 DOCX，然后进行翻译
                pdfResult = pdfTranslationService.processPdf(
                        tmpInputPath.toString(), tmpOutputPath.toString(), params.getTargetLang(),
                        params.isUseGlossaryReplace(), glossaryMap, progressCallback, textCallback,
                        params.isStrictFormat(), params.isEnableQc(), params.isEnableComparison()
                );
                outputPath = pdfResult.getOutputPath();
                // PDF 翻译后转换为 DOCX 格式
                ext = ".docx";
                
                // 如果启用了对照模式，获取对照文档路径
                if (params.isEnableComparison() && pdfResult.getContrastPath() != null && !pdfResult.getContrastPath().isEmpty()) {
                    contrastPath = pdfResult.getContrastPath();
                }
            } else {
                throw new UnsupportedOperationException("暂不支持的文件类型: " + ext);
            }

            // 提取翻译结果数据
            List<TranslationPair> pairs = extractTranslationPairs(docxResult, excelResult, pdfResult);
            Map<String, Object> qcReport = extractQcReport(docxResult, excelResult, pdfResult);
            String qcTxtPath = extractQcTxtPath(docxResult, excelResult, pdfResult);
            String error = extractError(docxResult, excelResult, pdfResult);

            // 设置最终的翻译对列表到任务信息
            taskManager.setFinalPairs(taskId, pairs);

            // 根据严格格式选项确定文件名标签
            String modeTag = params.isStrictFormat() ? "strict" : "fast";

            // 文件上传：上传翻译后的文档到 MinIO
            if (!Files.exists(Paths.get(outputPath))) {
                throw new IllegalStateException("翻译输出文件不存在: " + outputPath + "，翻译过程可能失败");
            }
            
            byte[] outputContent = fileHelper.readFileToBytes(outputPath);
            String outName = origBase + "_" + modeTag + ext;
            String translatedFileUrl = fileHelper.uploadToMinio(outputContent, outName, "translation/output");
            log.info("[runTranslationWithParams][taskId={}] 纯英文翻译结果已上传到 MinIO: {}", taskId, translatedFileUrl);
            taskManager.setDownloadMinioUrl(taskId, "file", translatedFileUrl);

            // 如果启用了对照模式且生成了对照文档，上传对照版本到 MinIO
            String contrastFileUrl = null;
            if (params.isEnableComparison() && contrastPath != null && Files.exists(Paths.get(contrastPath))) {
                byte[] contrastContent = fileHelper.readFileToBytes(contrastPath);
                String contrastName = origBase + "_" + modeTag + "_双语对照" + ext;
                tmpContrastPath = Paths.get(contrastPath);
                contrastFileUrl = fileHelper.uploadToMinio(contrastContent, contrastName, "translation/contrast");
                log.info("[runTranslationWithParams][taskId={}] 双语对照文档已上传到 MinIO: {}", taskId, contrastFileUrl);
                taskManager.setDownloadMinioUrl(taskId, "contrast", contrastFileUrl);
            }

            // 生成并上传对照表（Excel 格式，包含原文和译文对照）
            String compareFileUrl = null;
            if (!pairs.isEmpty()) {
                String compareName = origBase + "_" + modeTag + "_对照表.xlsx";
                tmpComparePath = Paths.get(properties.getTempDir()).resolve("compare_" + System.currentTimeMillis() + ".xlsx");
                // 生成对照表 Excel 文件
                generateComparisonExcel(pairs, tmpComparePath.toString());

                byte[] compareContent = fileHelper.readFileToBytes(tmpComparePath.toString());
                compareFileUrl = fileHelper.uploadToMinio(compareContent, compareName, "translation/compare");
                log.info("[runTranslationWithParams][taskId={}] 对照表文件已上传到 MinIO: {}", taskId, compareFileUrl);
                taskManager.setDownloadMinioUrl(taskId, "excel", compareFileUrl);
            }

            // 上传质检报告（如果生成了的话）
            String qcFileUrl = null;
            if (qcTxtPath != null && !qcTxtPath.isEmpty()) {
                String qcName = origBase + "_QC报告.txt";
                tmpQcPath = Paths.get(qcTxtPath);

                byte[] qcContent = fileHelper.readFileToBytes(qcTxtPath);
                qcFileUrl = fileHelper.uploadToMinio(qcContent, qcName, "translation/qc");
                log.info("[runTranslationWithParams][taskId={}] QC 质检报告已上传到 MinIO: {}", taskId, qcFileUrl);
                taskManager.setDownloadMinioUrl(taskId, "qc", qcFileUrl);
            }

            // 如果质检报告中有问题数，设置到任务信息
            if (qcReport != null && qcReport.containsKey("stats")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> stats = (Map<String, Object>) qcReport.get("stats");
                Object issuesCount = stats.get("issuesCount");
                if (issuesCount instanceof Number) {
                    taskManager.setQcIssues(taskId, ((Number) issuesCount).intValue());
                }
            }

            // 如果有错误，记录警告日志
            if (error != null && !error.isEmpty()) {
                log.warn("翻译任务完成但有错误: taskId={}, error={}", taskId, error);
            }

            // 根据是否有错误确定文件状态（1=成功，2=失败）
            Integer fileStatus = (error != null && !error.isEmpty()) ? 2 : 1;
            // 数据库操作：更新文件记录，保存各种结果文件的 MinIO URL
            fileHelper.updateFileRecord(fileId, translatedFileUrl, compareFileUrl, qcFileUrl, contrastFileUrl, fileStatus);

            // 更新任务状态为 completed（已完成）
            taskManager.updateStatus(taskId, "completed");

        } catch (Exception e) {
            log.error("翻译任务执行失败: taskId={}", taskId, e);
            taskManager.setError(taskId, e.getMessage());
            fileHelper.updateFileRecord(fileId, null, null, null, null, 2);

        } finally {
            Path[] pathsToDelete = {tmpInputPath, tmpOutputPath, tmpContrastPath, 
                                    tmpComparePath, tmpQcPath};
            for (Path path : pathsToDelete) {
                if (path != null) {
                    try {
                        Files.deleteIfExists(path);
                        log.debug("[runTranslationWithParams][taskId={}] 已删除临时文件: {}", taskId, path.getFileName());
                    } catch (Exception e) {
                        log.error("[runTranslationWithParams][taskId={}] 删除临时文件失败: {}", 
                                 taskId, path, e);
                    }
                }
            }

            ChatModelContext.clear();
            log.debug("[runTranslationWithParams][taskId={}] 已清理 ThreadLocal 中的 ChatModel", taskId);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("翻译任务完成: taskId={}, elapsed={}ms", taskId, elapsed);
        }
    }

    /**
     * 获取文件的基础名称（不含扩展名）
     *
     * @param filename 文件名
     * @return 基础名称
     */
    private String getBaseName(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "output";
        }
        return filename.substring(0, filename.lastIndexOf("."));
    }

    /**
     * 从翻译结果中提取翻译对列表
     * <p>
     * 根据不同的文档类型（DOCX/Excel/PDF），从对应的结果对象中提取翻译对。
     *
     * @param docxResult DOCX 翻译结果
     * @param excelResult Excel 翻译结果
     * @param pdfResult PDF 翻译结果
     * @return 翻译对列表
     */
    private List<TranslationPair> extractTranslationPairs(
            DocxTranslationService.TranslationResult docxResult,
            ExcelTranslationService.ExcelResult excelResult,
            PdfTranslationService.PdfResult pdfResult) {

        List<TranslationPair> pairs = new ArrayList<>();

        if (docxResult != null) {
            // 从 DOCX 翻译结果中提取翻译对
            for (DocxTranslationService.TranslationPair dp : docxResult.getPairs()) {
                TranslationPair p = new TranslationPair();
                p.setSource(dp.getSource());
                p.setTarget(dp.getTarget());
                p.setStatus(dp.getStatus());
                p.setAddToGlossary(dp.isAddToGlossary());
                pairs.add(p);
            }
        } else if (excelResult != null) {
            // 从 Excel 翻译结果中提取翻译对
            for (ExcelTranslationService.ExcelPair ep : excelResult.getPairs()) {
                TranslationPair p = new TranslationPair();
                p.setSource(ep.getSource());
                p.setTarget(ep.getTarget());
                p.setStatus(ep.getStatus());
                pairs.add(p);
            }
        } else if (pdfResult != null) {
            // 从 PDF 翻译结果中提取翻译对
            for (Map<String, String> pp : pdfResult.getPairs()) {
                TranslationPair p = new TranslationPair();
                p.setSource(pp.get("原文"));
                p.setTarget(pp.get("译文"));
                p.setStatus(pp.get("状态"));
                pairs.add(p);
            }
        }

        return pairs;
    }

    /**
     * 从翻译结果中提取质检报告
     */
    private Map<String, Object> extractQcReport(DocxTranslationService.TranslationResult docxResult,
                                                 ExcelTranslationService.ExcelResult excelResult,
                                                 PdfTranslationService.PdfResult pdfResult) {
        if (docxResult != null) return docxResult.getQcReport();
        if (excelResult != null) return excelResult.getQcReport();
        if (pdfResult != null) return pdfResult.getQcReport();
        return null;
    }

    /**
     * 从翻译结果中提取质检报告文件路径
     */
    private String extractQcTxtPath(DocxTranslationService.TranslationResult docxResult,
                                     ExcelTranslationService.ExcelResult excelResult,
                                     PdfTranslationService.PdfResult pdfResult) {
        if (docxResult != null) return docxResult.getQcTxtPath();
        if (excelResult != null) return excelResult.getQcTxtPath();
        if (pdfResult != null) return pdfResult.getQcTxtPath();
        return null;
    }

    /**
     * 从翻译结果中提取错误信息
     */
    private String extractError(DocxTranslationService.TranslationResult docxResult,
                                 ExcelTranslationService.ExcelResult excelResult,
                                 PdfTranslationService.PdfResult pdfResult) {
        if (docxResult != null) return docxResult.getError();
        if (excelResult != null) return excelResult.getError();
        if (pdfResult != null) return pdfResult.getError();
        return null;
    }

    /**
     * 根据文件类型获取对应的 MinIO URL
     * <p>
     * 从数据库文件记录中获取不同文件类型的 MinIO 访问地址。
     *
     * @param fileDO 文件记录对象
     * @param fileType 文件类型（file/excel/qc/contrast）
     * @return MinIO URL
     */
    private String getMinioUrlByFileType(TranFileDO fileDO, String fileType) {
        switch (fileType) {
            case "file":
                return fileDO.getFileUrl();
            case "excel":
                return fileDO.getCompareFileUrl();
            case "qc":
                return fileDO.getQcFileUrl();
            case "contrast":
                return fileDO.getContrastFileUrl();
            default:
                return null;
        }
    }

    /**
     * 根据文件类型生成对应的下载文件名
     *
     * @param fileDO 文件记录对象
     * @param fileType 文件类型（file/excel/qc/contrast）
     * @return 下载文件名
     */
    private String getFilenameByFileType(TranFileDO fileDO, String fileType) {
        String baseName = getBaseName(fileDO.getFileName());
        switch (fileType) {
            case "file":
                return fileDO.getFileName();
            case "excel":
                return baseName + "_对照表.xlsx";
            case "qc":
                return baseName + "_QC报告.txt";
            case "contrast":
                return baseName + "_双语对照" + fileDO.getFileExt();
            default:
                return null;
        }
    }

    /**
     * 生成对照表 Excel 文件
     * <p>
     * 创建一个 Excel 文件，包含三列：原文、译文、状态。
     * 用于展示翻译结果的对照。
     *
     * @param pairs 翻译对列表
     * @param outputPath 输出文件路径
     */
    private void generateComparisonExcel(List<TranslationPair> pairs, String outputPath) {
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            var sheet = workbook.createSheet("对照表");

            // 创建表头
            var headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("原文");
            headerRow.createCell(1).setCellValue("译文");
            headerRow.createCell(2).setCellValue("状态");

            // 填充数据
            for (int i = 0; i < pairs.size(); i++) {
                var row = sheet.createRow(i + 1);
                TranslationPair pair = pairs.get(i);
                row.createCell(0).setCellValue(pair.getSource());
                row.createCell(1).setCellValue(pair.getTarget());
                row.createCell(2).setCellValue(pair.getStatus() != null ? pair.getStatus() : "");
            }

            // 写入文件
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(outputPath)) {
                workbook.write(fos);
            }

        } catch (Exception e) {
            log.error("生成对照表失败", e);
        }
    }

    /**
     * 构建系统提示词（用于缓存）
     * <p>
     * 根据角色ID或配置构建翻译提示词，整个翻译任务只调用一次
     *
     * @param targetLanguage 目标语言
     * @param roleId 角色ID（可选）
     * @return 系统提示词
     */
    private String buildSystemPrompt(String targetLanguage, Long roleId) {
        // 1. 如果提供了角色ID，尝试从数据库加载角色的 systemMessage
        if (roleId != null) {
            try {
                AiChatRoleDO chatRole = chatRoleService.getChatRole(roleId);
                if (chatRole != null && chatRole.getSystemMessage() != null && !chatRole.getSystemMessage().isEmpty()) {
                    log.info("[buildSystemPrompt] 使用自定义角色提示词: roleId={}, roleName={}", 
                            roleId, chatRole.getName());
                    String prompt = chatRole.getSystemMessage() + "\n\n目标语言：" + targetLanguage + "。";
                    log.info("[buildSystemPrompt] 最终提示词:\n{}", prompt);
                    return prompt;
                } else {
                    log.warn("[buildSystemPrompt] 角色不存在或没有设定提示词: roleId={}", roleId);
                }
            } catch (Exception e) {
                log.error("[buildSystemPrompt] 加载角色提示词失败: roleId={}", roleId, e);
            }
        }

        // 2. 如果没有提供角色ID，尝试从 Nacos 配置中获取角色名称
        try {
            String roleName = properties.getDefaultRoleName();
            if (roleName != null && !roleName.isEmpty()) {
                log.info("[buildSystemPrompt] 从 Nacos 配置中获取到角色名称: {}", roleName);
                List<AiChatRoleDO> roles = chatRoleService.getChatRoleListByName(roleName);
                if (roles != null && !roles.isEmpty()) {
                    AiChatRoleDO chatRole = roles.get(0);
                    if (chatRole.getSystemMessage() != null && !chatRole.getSystemMessage().isEmpty()) {
                        log.info("[buildSystemPrompt] 使用配置角色提示词: roleName={}, roleId={}", 
                                roleName, chatRole.getId());
                        String prompt = chatRole.getSystemMessage() + "\n\n目标语言：" + targetLanguage + "。";
                        log.info("[buildSystemPrompt] 最终提示词:\n{}", prompt);
                        return prompt;
                    } else {
                        log.info("[buildSystemPrompt] 配置角色没有设定提示词: roleName={}", roleName);
                    }
                } else {
                    log.info("[buildSystemPrompt] 未找到匹配的角色: roleName={}", roleName);
                }
            }
        } catch (Exception e) {
            log.error("[buildSystemPrompt] 从 Nacos 配置加载角色提示词失败", e);
        }

        // 3. 使用默认提示词
        log.info("[buildSystemPrompt] 使用默认翻译提示词");
        String defaultPrompt = String.format(
                "你是专业的翻译助手。目标语言：%s。\n" +
                        "- 准确翻译原文内容，保持原意不变。\n" +
                        "- 严禁添加解释、示例或额外说明。\n" +
                        "- 仅返回翻译文本，不输出任何解释性语句、引导语或前缀。\n" +
                        "- 严格保留原文中的数字、日期、比例、缩写、单位、标点符号和格式结构。\n" +
                        "- 专业术语保持一致性，不要随意更改。\n",
                targetLanguage
        );
        log.info("[buildSystemPrompt] 最终提示词:\n{}", defaultPrompt);
        return defaultPrompt;
    }
}