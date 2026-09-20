package cn.iocoder.sva.module.ai.tool.function;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranGlossaryPageReqVO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranFileDO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranGlossaryDO;
import cn.iocoder.sva.module.ai.dal.mysql.file.ChatbotFileMapper;
import cn.iocoder.sva.module.ai.dal.mysql.translation.TranFileMapper;
import cn.iocoder.sva.module.ai.dal.mysql.translation.TranGlossaryMapper;
import cn.iocoder.sva.module.ai.dal.mysql.user.AdminUserMapper;
import cn.iocoder.sva.module.ai.service.feishu.FeishuAiUtil;
import cn.iocoder.sva.module.ai.service.file.FileService;
import cn.iocoder.sva.module.ai.service.translation.TranFileService;
import cn.iocoder.sva.module.ai.service.translation.TranGlossaryService;
import cn.iocoder.sva.module.ai.service.translation.TranService;
import cn.iocoder.sva.module.ai.service.translation.tran.TaskManagerService;
import cn.iocoder.sva.module.ai.service.translation.tran.model.TaskInfo;
import cn.iocoder.sva.module.ai.tool.function.feishutools.FileInfoExtractor;
import cn.iocoder.sva.module.ai.tool.function.feishutools.FileQueryService;
import cn.iocoder.sva.module.ai.tool.function.feishutools.TranslationExecutor;
import cn.iocoder.sva.module.ai.tool.function.feishutools.UserContextManager;
import cn.iocoder.sva.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 飞书AI助手工具类
 * <p>
 * 提供一系列可以被Spring AI调用的工具方法，用于增强飞书机器人的功能
 */
@Slf4j
@Component
public class FeishuAiAssistantTools {

    @Resource
    private ChatbotFileMapper chatbotFileMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private FeishuAiUtil feishuAiUtil;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private TranService tranService;

    @Resource
    private TranFileService tranFileService;

    @Resource
    private TaskManagerService taskManagerService;

    @Resource
    private AdminUserMapper adminUserMapper;

    @Resource
    private FileService fileService;

    @Resource
    private TranFileMapper tranFileMapper;

    @Resource
    private TranGlossaryMapper tranGlossaryMapper;

    @Resource
    private TranGlossaryService tranGlossaryService;

    // 辅助组件
    private FileInfoExtractor fileInfoExtractor;
    private UserContextManager userContextManager;
    private FileQueryService fileQueryService;
    private TranslationExecutor translationExecutor;

    // ===================== Redis Key 前缀：飞书用户ID -> 对话ID =====================
    private static final String REDIS_KEY_CONVERSATION = "feishu:conversation:";

    // ===================== 【新增】Redis Key 前缀：线程ID -> 飞书userId =====================
    private static final String REDIS_KEY_CURRENT_FEISHU_USER_ID = "feishu:user:current:";

    // ===================== 翻译任务等待配置 =====================
    private static final int MAX_WAIT_TIME_SECONDS = 300; // 最大等待5分钟
    private static final long POLL_INTERVAL_MS = 2000; // 每2秒轮询一次

    // ===================== MinIO 配置 =====================
    private static final Long DEFAULT_MINIO_CONFIG_ID = 1L; // 默认MinIO配置ID，需要根据实际情况调整

    @jakarta.annotation.PostConstruct
    public void init() {
        fileInfoExtractor = new FileInfoExtractor();
        userContextManager = new UserContextManager(stringRedisTemplate, adminUserMapper);
        fileQueryService = new FileQueryService(chatbotFileMapper, tranFileMapper, userContextManager, fileInfoExtractor);
        translationExecutor = new TranslationExecutor(tranService, fileService, fileQueryService, fileInfoExtractor, userContextManager);
    }

    /**
     * 获取当前登录用户的用户名
     * <p>
     * 优先从 Security 上下文获取，如果获取不到则从 Redis 中根据当前线程ID获取飞书userId，
     * 然后查询科兴系统的用户名
     *
     * @return 用户名
     */
    @Tool(name = "get_current_username", description = "获取当前登录用户的用户名")
    public String getCurrentUsername() {
        return userContextManager.getCurrentUsername();
    }

    /**
     * 获取当前会话ID
     * <p>
     * 从 Redis 中获取当前用户的会话ID，Redis Key 格式为：feishu:conversation:{username}
     *
     * @return 当前会话ID，如果不存在则返回null
     */
    @Tool(name = "get_current_chat_id", description = "从Redis中获取当前用户的会话ID")
    public String getCurrentChatId() {
        return userContextManager.getCurrentChatId();
    }

    /**
     * 根据用户名查询用户最近上传的一个文件
     * <p>
     * 会话ID会从Redis中自动获取（key格式：feishu:conversation:{username}），
     * 如果能获取到则作为查询条件，否则只根据用户名查询
     *
     * @param username 用户名（工号，可选，不传则使用当前登录用户）
     * @return 最近上传的文件信息
     */
    @Tool(name = "get_latest_uploaded_file",
            description = "根据用户名查询用户最近上传的一个文件。\n\n【重要】username 参数必须是真实的用户名（工号），不能是描述性文字。\n【使用方法】\n1. 先调用 get_current_username 获取当前用户的真实用户名\n2. 将获取到的用户名作为 username 参数传入\n3. 如果用户说'这个文档'或'帮我翻译'，调用此接口获取最近上传的文件\n\n示例：\n- 错误：username='当前用户'、username='我'、username='用户'\n- 正确：username='zhangsan'（通过 get_current_username 获取的真实用户名）")
    public Map<String, Object> getLatestUploadedFile(String username) {
        return fileQueryService.getLatestUploadedFile(username);
    }

    /**
     * 根据用户名和数量查询用户最近上传的N个文件
     * <p>
     * 会话ID会从Redis中自动获取（key格式：feishu:conversation:{username}），
     * 如果能获取到则作为查询条件，否则只根据用户名查询
     *
     * @param username 用户名（工号，可选，不传则使用当前登录用户）
     * @param count 查询数量
     * @return 最近上传的文件列表
     */
    @Tool(name = "get_recent_uploaded_files",
            description = "根据用户名和数量查询用户最近上传的N个文件。\n\n【重要】username 参数必须是真实的用户名（工号），不能是描述性文字。\n【使用方法】\n1. 先调用 get_current_username 获取当前用户的真实用户名\n2. 将获取到的用户名作为 username 参数传入\n3. count 参数指定要查询的文件数量\n\n示例：\n- 错误：username='当前用户'、username='我'、username='用户'\n- 正确：username='zhangsan'（通过 get_current_username 获取的真实用户名）")
    public Map<String, Object> getRecentUploadedFiles(String username, Integer count) {
        return fileQueryService.getRecentUploadedFiles(username, count);
    }

    /**
     * DOCX文档翻译工具
     * <p>
     * 调用完整的DOCX翻译流程，支持以下选项：
     * - 是否双语对照：生成包含原文和译文的双语对照文档
     * - 是否全文QC：进行全文质量检查，生成质检报告
     * - 是否严格格式：保持原文档的格式不变
     * - 是否使用术语库：使用指定的术语库进行术语替换
     * <p>
     * 该工具会等待翻译完成后，从数据库查询翻译结果文件的MinIO路径并返回。
     *
     * @param filePath 文件路径（MinIO中的文件路径，如 "translation/input/document.docx"）
     * @param targetLanguage 目标语言（必须使用英文全拼，如：English、Chinese、Japanese、Korean、French、German、Spanish）
     * @param enableComparison 是否启用双语对照（true/false，默认 true）
     * @param enableQc 是否启用质量检查（true/false，默认 false）
     * @param strictFormat 是否严格保持格式（true/false，默认 true）
     * @param useGlossary 是否使用术语库（true/false，默认 true）
     * @param glossaryId 术语库ID（可选）
     * @return 翻译结果，包含各种文件的MinIO路径
     */
    @Tool(name = "translate_docx_document",
            description = "DOCX文档翻译工具。\n\n" +
                    "【🔴 极其重要 - 必须严格执行】在使用此工具前，你必须先调用 get_user_glossary_list() 获取术语库ID！\n\n" +
                    "❌ 错误做法：直接调用 translate_docx_document 而不先查询术语库\n" +
                    "✅ 正确做法：\n" +
                    "  1. 先调用 get_user_glossary_list(glossaryName=\"SVA09969的术语库\") 或 get_user_glossary_list()\n" +
                    "  2. 从返回结果中提取 id 字段作为 glossaryId\n" +
                    "  3. 再调用 translate_docx_document，传入获取到的 glossaryId\n\n" +
                    "【步骤详解】\n" +
                    "第一步：调用 get_user_glossary_list() 获取术语库列表\n" +
                    "  - 如果用户说了术语库名称（如'用SVA09969的术语库'），传入 glossaryName=\"SVA09969的术语库\"\n" +
                    "  - 如果用户没说，不传 glossaryName 参数\n\n" +
                    "第二步：从返回结果中提取 glossaryId\n" +
                    "  - 返回格式：{\"success\":true, \"glossaries\":[{\"id\":123, \"name\":\"...\", ...}]}\n" +
                    "  - 选择第一个匹配的术语库，提取其 id 字段\n\n" +
                    "第三步：调用本翻译工具\n" +
                    "  - glossaryId = 第二步提取的 id 值\n" +
                    "  - useGlossary = true（如果找到了术语库）\n\n" +
                    "【参数说明】\n" +
                    "- filePath: 文件路径（从 get_latest_uploaded_file 获取）\n" +
                    "- targetLanguage: 目标语言（English/Chinese/Japanese等）\n" +
                    "- enableComparison: 双语对照（默认true）\n" +
                    "- enableQc: 质量检查（默认false）\n" +
                    "- strictFormat: 严格格式（默认true）\n" +
                    "- useGlossary: 是否使用术语库（找到术语库则为true，否则为false）\n" +
                    "- glossaryId: 术语库ID（从 get_user_glossary_list 返回结果中获取的 id）\n\n" +
                    "【回复格式要求】\n" +
                    "翻译完成后，必须按照以下格式回复用户：\n\n" +
                    "您的文件\"{文件名}\"已经翻译完成。以下是相关文件的下载链接：\n" +
                    "1. **翻译后的文件**：\n" +
                    "   {链接文本}\n" +
                    "2. **双语对照文件**：\n" +
                    "   {链接文本}\n" +
                    "3. **对照表文件**：\n" +
                    "   {链接文本}")
    public Map<String, Object> translateDocxDocument(String filePath, String targetLanguage,
                                                     Boolean enableComparison, Boolean enableQc,
                                                     Boolean strictFormat, Boolean useGlossary,
                                                     Long glossaryId) {
        log.info("========== [translate_docx_document] 工具被调用 ==========");
        log.info("[translate_docx_document] 接收到的参数:");
        log.info("  - filePath: {}", filePath);
        log.info("  - targetLanguage: {}", targetLanguage);
        log.info("  - useGlossary: {}", useGlossary);
        log.info("  - glossaryId: {}", glossaryId);
        
        if (useGlossary != null && useGlossary && glossaryId == null) {
            log.error("========== [translate_docx_document] ⚠️⚠️⚠️ 严重错误 ⚠️⚠️⚠️ ==========");
            log.error("[translate_docx_document] useGlossary=true 但 glossaryId=null！");
            log.error("[translate_docx_document] 这说明 AI 没有正确调用 get_user_glossary_list 工具！");
            log.error("[translate_docx_document] 请检查 AI 的角色配置和提示词！");
            log.error("==============================================================");
        }
        
        return translationExecutor.executeTranslation(filePath, targetLanguage, enableComparison,
                enableQc, strictFormat, useGlossary, glossaryId, "DOCX");
    }

    /**
     * PDF文档翻译工具
     * <p>
     * 调用完整的PDF翻译流程，支持以下选项：
     * - 是否双语对照：生成包含原文和译文的双语对照文档
     * - 是否全文QC：进行全文质量检查，生成质检报告
     * - 是否严格格式：保持原文档的格式不变
     * - 是否使用术语库：使用指定的术语库进行术语替换
     * <p>
     * 该工具会等待翻译完成后，从数据库查询翻译结果文件的MinIO路径并返回。
     *
     * @param filePath 文件路径（MinIO中的文件路径，如 "translation/input/document.pdf"）
     * @param targetLanguage 目标语言（必须使用英文全拼，如：English、Chinese、Japanese、Korean、French、German、Spanish）
     * @param enableComparison 是否启用双语对照（true/false，默认 true）
     * @param enableQc 是否启用质量检查（true/false，默认 false）
     * @param strictFormat 是否严格保持格式（true/false，默认 true）
     * @param useGlossary 是否使用术语库（true/false，默认 true）
     * @param glossaryId 术语库ID（可选）
     * @return 翻译结果，包含各种文件的MinIO路径
     */
    @Tool(name = "translate_pdf_document",
            description = "PDF文档翻译工具。\n\n" +
                    "【重要】在使用此工具前，必须先确定要使用的术语库ID（glossaryId）：\n\n" +
                    "第一步：调用 get_user_glossary_list() 获取当前用户可用的术语库列表\n" +
                    "  - 如果用户明确说了术语库名称（如'用我的医学术语库'），则传入 glossaryName 参数进行搜索\n" +
                    "  - 如果用户没说，则不传 glossaryName 参数，获取所有可用术语库\n\n" +
                    "第二步：从返回的术语库列表中选择合适的术语库：\n" +
                    "  - 优先选择名为 '{当前用户名}的术语库' 且 languageDirection 包含目标语言的术语库\n" +
                    "  - 例如：翻译成 English，就找 languageDirection 包含 '->English' 或 targetLanguage='English' 的术语库\n" +
                    "  - 如果找到多个匹配的，选择 itemCount 最多的那个\n" +
                    "  - 如果没找到任何匹配的术语库，设置 useGlossary=false\n\n" +
                    "第三步：调用本翻译工具，传入选定的 glossaryId\n\n" +
                    "【参数说明】\n" +
                    "- filePath: 文件路径（从 get_latest_uploaded_file 获取）\n" +
                    "- targetLanguage: 目标语言（必须用英文全拼：English、Chinese、Japanese等）\n" +
                    "- enableComparison: 双语对照（默认true）\n" +
                    "- enableQc: 质量检查（默认false）\n" +
                    "- strictFormat: 严格格式（默认true）\n" +
                    "- useGlossary: 是否使用术语库（默认true，但如果没有合适的术语库则设为false）\n" +
                    "- glossaryId: 术语库ID（从第一步获取）\n\n" +
                    "【回复格式要求】\n" +
                    "翻译完成后，必须按照以下格式回复用户（将 {文件名} 替换为实际文件名，{链接文本} 替换为实际的下载链接）：\n\n" +
                    "您的文件\"{文件名}\"已经翻译完成。以下是相关文件的下载链接：\n" +
                    "1. **翻译后的文件**：\n" +
                    "   {链接文本}\n" +
                    "2. **双语对照文件**：\n" +
                    "   {链接文本}\n" +
                    "3. **对照表文件**：\n" +
                    "   {链接文本}\n\n" +
                    "注意：只展示上述三项内容，不要展示其他信息（如任务ID、费用、处理时间等）。")
    public Map<String, Object> translatePdfDocument(String filePath, String targetLanguage,
                                                    Boolean enableComparison, Boolean enableQc,
                                                    Boolean strictFormat, Boolean useGlossary,
                                                    Long glossaryId) {
        return translationExecutor.executeTranslation(filePath, targetLanguage, enableComparison,
                enableQc, strictFormat, useGlossary, glossaryId, "PDF");
    }

    /**
     * Excel文档翻译工具
     * <p>
     * 调用完整的Excel翻译流程，支持以下选项：
     * - 是否全文QC：进行全文质量检查，生成质检报告
     * - 是否使用术语库：使用指定的术语库进行术语替换
     * <p>
     * 该工具会等待翻译完成后，从数据库查询翻译结果文件的MinIO路径并返回。
     *
     * @param filePath 文件路径（MinIO中的文件路径，如 "translation/input/document.xlsx"）
     * @param targetLanguage 目标语言（必须使用英文全拼，如：English、Chinese、Japanese、Korean、French、German、Spanish）
     * @param enableQc 是否启用质量检查（true/false，默认 false）
     * @param useGlossary 是否使用术语库（true/false，默认 true）
     * @param glossaryId 术语库ID（可选）
     * @return 翻译结果，包含各种文件的MinIO路径
     */
    @Tool(name = "translate_excel_document",
            description = "Excel文档翻译工具。\n\n" +
                    "【重要】在使用此工具前，必须先确定要使用的术语库ID（glossaryId）：\n\n" +
                    "第一步：调用 get_user_glossary_list() 获取当前用户可用的术语库列表\n" +
                    "  - 如果用户明确说了术语库名称（如'用我的医学术语库'），则传入 glossaryName 参数进行搜索\n" +
                    "  - 如果用户没说，则不传 glossaryName 参数，获取所有可用术语库\n\n" +
                    "第二步：从返回的术语库列表中选择合适的术语库：\n" +
                    "  - 优先选择名为 '{当前用户名}的术语库' 且 languageDirection 包含目标语言的术语库\n" +
                    "  - 例如：翻译成 English，就找 languageDirection 包含 '->English' 或 targetLanguage='English' 的术语库\n" +
                    "  - 如果找到多个匹配的，选择 itemCount 最多的那个\n" +
                    "  - 如果没找到任何匹配的术语库，设置 useGlossary=false\n\n" +
                    "第三步：调用本翻译工具，传入选定的 glossaryId\n\n" +
                    "【参数说明】\n" +
                    "- filePath: 文件路径（从 get_latest_uploaded_file 获取）\n" +
                    "- targetLanguage: 目标语言（必须用英文全拼：English、Chinese、Japanese等）\n" +
                    "- enableQc: 质量检查（默认false）\n" +
                    "- useGlossary: 是否使用术语库（默认true，但如果没有合适的术语库则设为false）\n" +
                    "- glossaryId: 术语库ID（从第一步获取）\n\n" +
                    "【回复格式要求】\n" +
                    "翻译完成后，必须按照以下格式回复用户（将 {文件名} 替换为实际文件名，{链接文本} 替换为实际的下载链接）：\n\n" +
                    "您的文件\"{文件名}\"已经翻译完成。以下是相关文件的下载链接：\n" +
                    "1. **翻译后的文件**：\n" +
                    "   {链接文本}\n" +
                    "2. **对照表文件**：\n" +
                    "   {链接文本}\n\n" +
                    "注意：Excel不支持双语对照，所以只展示上述两项内容，不要展示其他信息（如任务ID、费用、处理时间等）。")
    public Map<String, Object> translateExcelDocument(String filePath, String targetLanguage,
                                                      Boolean enableQc, Boolean useGlossary,
                                                      Long glossaryId) {
        return translationExecutor.executeExcelTranslation(filePath, targetLanguage, enableQc, useGlossary, glossaryId);
    }

    /**
     * 查询当前用户有权查询的术语库列表
     * <p>
     * 根据术语库名称模糊查询（可选），不传则查询所有有权访问的术语库。
     * 权限规则：
     * - 超级管理员：可以查看所有术语库
     * - 普通用户：只能查看自己创建的或所属角色的术语库
     * 重要约束：回答时候仅可以说明术语库名称、语言方向和术语数量三项，其他的都不能展示
     *
     * @param glossaryName 术语库名称（可选，支持模糊查询）
     * @return 术语库列表
     */
    @Tool(name = "get_user_glossary_list",
            description = "查询当前用户有权访问的术语库列表，可按名称模糊搜索。\n\n" +
                    "【使用方法】\n" +
                    "1. 如果用户明确指定了术语库名称（如'用我的医学术语库'），传入 glossaryName 参数进行搜索\n" +
                    "2. 如果用户未指定，不传 glossaryName 参数，获取所有可用术语库\n\n" +
                    "【返回信息】\n" +
                    "每个术语库包含：id（术语库ID）、name（名称）、languageDirection（语言方向，如'Chinese->English'）、targetLanguage（目标语言）、itemCount（术语数量）\n\n" +
                    "【如何选择术语库】\n" +
                    "1. 优先选择名为 '{当前用户名}的术语库' 的术语库\n" +
                    "2. 确保 languageDirection 包含目标语言或 targetLanguage 等于目标语言\n" +
                    "   例如：翻译成 English，找 languageDirection 包含 '->English' 或 targetLanguage='English'\n" +
                    "3. 如果有多个匹配的，选择 itemCount 最多的\n" +
                    "4. 如果没有任何匹配的，不要使用术语库\n\n" +
                    "【重要约束】回答时仅可以说明术语库名称、语言方向和术语数量三项，其他的都不能展示")
    public Map<String, Object> getUserGlossaryList(String glossaryName) {
        log.info("[getUserGlossaryList] 工具被调用, glossaryName={}", glossaryName);
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 获取当前用户信息
            String username = userContextManager.getEffectiveUsername();

            if (StrUtil.isBlank(username)) {
                log.warn("[getUserGlossaryList] 无法获取当前用户信息");
                result.put("success", false);
                result.put("message", "无法获取当前用户信息");
                return result;
            }

            log.info("[getUserGlossaryList] 获取到当前用户: username={}", username);

            // 2. 根据用户名查询用户ID，用于权限控制
            Long userId = userContextManager.getUserIdByUsername(username);
            if (userId == null) {
                log.warn("[getUserGlossaryList] 无法查询到用户ID, username={}", username);
                result.put("success", false);
                result.put("message", "用户不存在: " + username);
                return result;
            }

            log.info("[getUserGlossaryList] 查询到用户ID: userId={}", userId);

            // 3. 构建分页请求对象，默认第一页100条
            TranGlossaryPageReqVO pageReqVO = new TranGlossaryPageReqVO();
            pageReqVO.setPageNo(1);
            pageReqVO.setPageSize(100);

            // 设置术语库名称模糊查询条件
            if (StrUtil.isNotBlank(glossaryName)) {
                pageReqVO.setGlossaryName(glossaryName);
            }

            // 4. 调用Service的分页查询方法（传入userId和username进行权限控制）
            log.info("[getUserGlossaryList] 开始查询术语库列表, userId={}, username={}, glossaryName={}, pageNo=1, pageSize=100", 
                    userId, username, glossaryName);

            PageResult<TranGlossaryDO> pageResult = tranGlossaryService.getTranGlossaryPage(pageReqVO, userId, username);

            List<TranGlossaryDO> glossaryList = pageResult.getList();

            if (glossaryList != null && !glossaryList.isEmpty()) {
                List<Map<String, Object>> glossaryInfoList = new ArrayList<>();
                for (TranGlossaryDO glossary : glossaryList) {
                    Map<String, Object> glossaryInfo = new HashMap<>();
                    glossaryInfo.put("id", glossary.getId());
                    glossaryInfo.put("name", glossary.getGlossaryName());
                    glossaryInfo.put("sourceLanguage", glossary.getSourceLanguage());
                    glossaryInfo.put("targetLanguage", glossary.getTargetLanguage());
                    glossaryInfo.put("languageDirection", glossary.getLanguageDirection());
                    glossaryInfo.put("itemCount", glossary.getItemCount());
                    glossaryInfo.put("isEnabled", glossary.getIsEnabled());
                    
                    // ===================== 【新增】添加匹配度评分，帮助AI决策 =====================
                    int matchScore = calculateMatchScore(glossary, username, null);
                    glossaryInfo.put("matchScore", matchScore);
                    
                    glossaryInfoList.add(glossaryInfo);
                }

                // 按匹配度排序，帮助AI快速找到最合适的术语库
                glossaryInfoList.sort((a, b) -> ((Number) b.get("matchScore")).intValue() - ((Number) a.get("matchScore")).intValue());

                log.info("[getUserGlossaryList] 查询成功, 找到{}个术语库（总数：{}）",
                        glossaryInfoList.size(), pageResult.getTotal());
                result.put("success", true);
                result.put("glossaries", glossaryInfoList);
                result.put("count", glossaryInfoList.size());
                result.put("total", pageResult.getTotal());
                result.put("message", "找到" + glossaryInfoList.size() + "个术语库，已按匹配度排序");
            } else {
                log.info("[getUserGlossaryList] 未找到术语库");
                result.put("success", true);
                result.put("glossaries", new ArrayList<>());
                result.put("count", 0);
                result.put("total", 0);
                result.put("message", "未找到术语库");
            }
        } catch (Exception e) {
            log.error("[getUserGlossaryList] 查询术语库列表失败", e);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
        }

        log.info("[getUserGlossaryList] 工具执行完成, success={}", result.get("success"));
        return result;
    }

    /**
     * ===================== 【新增】计算术语库匹配度评分 =====================
     * 
     * @param glossary 术语库对象
     * @param username 当前用户名
     * @param targetLanguage 目标语言（可选）
     * @return 匹配度评分（越高越匹配）
     */
    private int calculateMatchScore(TranGlossaryDO glossary, String username, String targetLanguage) {
        int score = 0;
        
        // 1. 如果是用户自己的术语库，加50分
        String expectedName = username + "的术语库";
        if (expectedName.equals(glossary.getGlossaryName())) {
            score += 50;
        }
        
        // 2. 如果术语库已启用，加10分
        if (Boolean.TRUE.equals(glossary.getIsEnabled())) {
            score += 10;
        }
        
        // 3. 术语数量越多，分数越高（每100个术语加1分，最多20分）
        if (glossary.getItemCount() != null) {
            score += Math.min(glossary.getItemCount() / 100, 20);
        }
        
        return score;
    }

    // ===================== 等待翻译任务完成 =====================

    /**
     * 等待翻译任务完成并获取结果文件路径
     * <p>
     * 轮询查询任务状态，直到任务完成或超时，然后从数据库查询文件记录获取MinIO路径。
     *
     * @param taskId 任务ID
     * @return 翻译结果文件路径信息
     */
    private Map<String, Object> waitForTranslationCompletion(String taskId) {
        log.info("[waitForTranslationCompletion] 开始等待翻译任务完成, taskId={}", taskId);
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // 轮询等待任务完成
            while (System.currentTimeMillis() - startTime < MAX_WAIT_TIME_SECONDS * 1000L) {
                TaskInfo taskInfo = taskManagerService.getTask(taskId);

                if (taskInfo == null) {
                    log.warn("[waitForTranslationCompletion] 任务不存在: {}", taskId);
                    result.put("success", false);
                    result.put("message", "任务不存在: " + taskId);
                    return result;
                }

                String status = taskInfo.getStatus();
                log.debug("[waitForTranslationCompletion] 任务状态: taskId={}, status={}, progress={}",
                        taskId, status, taskInfo.getProgress());

                if ("completed".equals(status)) {
                    log.info("[waitForTranslationCompletion] 任务已完成, 开始查询文件记录");
                    // 任务完成，从数据库查询文件记录
                    Long fileId = taskInfo.getFileId();
                    if (fileId != null) {
                        log.info("[waitForTranslationCompletion] 查询文件记录, fileId={}", fileId);
                        TranFileDO fileDO = tranFileService.getTranFile(fileId);
                        if (fileDO != null) {
                            log.info("[waitForTranslationCompletion] 翻译完成, 成功获取文件记录");
                            result.put("success", true);
                            result.put("message", "翻译完成");
                            result.put("files", fileInfoExtractor.extractFileUrls(fileDO));
                            return result;
                        } else {
                            log.warn("[waitForTranslationCompletion] 文件记录不存在, fileId={}", fileId);
                        }
                    }

                    result.put("success", false);
                    result.put("message", "任务完成但未找到文件记录");
                    return result;

                } else if ("failed".equals(status)) {
                    log.error("[waitForTranslationCompletion] 任务失败: {}", taskInfo.getError());
                    result.put("success", false);
                    result.put("message", "翻译失败: " + taskInfo.getError());
                    return result;
                }

                // 继续等待
                Thread.sleep(POLL_INTERVAL_MS);
            }

            // 超时
            log.warn("[waitForTranslationCompletion] 翻译超时, taskId={}, 超时时间={}秒",
                    taskId, MAX_WAIT_TIME_SECONDS);
            result.put("success", false);
            result.put("message", "翻译超时（超过" + MAX_WAIT_TIME_SECONDS + "秒）");
            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[waitForTranslationCompletion] 等待被中断", e);
            result.put("success", false);
            result.put("message", "等待被中断");
            return result;
        } catch (Exception e) {
            log.error("[waitForTranslationCompletion] 等待翻译完成失败", e);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
            return result;
        }
    }
}