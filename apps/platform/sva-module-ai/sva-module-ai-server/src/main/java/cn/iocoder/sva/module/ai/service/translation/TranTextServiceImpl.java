package cn.iocoder.sva.module.ai.service.translation;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.sva.framework.common.biz.system.permission.PermissionCommonApi;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;
import cn.iocoder.sva.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranTextHistoryPageReqVO;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranTextTranslateReqVO;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranTextTranslateRespVO;
import cn.iocoder.sva.module.ai.dal.dataobject.model.AiChatRoleDO;
import cn.iocoder.sva.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranGlossaryItemDO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranTextHistoryDO;
import cn.iocoder.sva.module.ai.dal.mysql.translation.TranTextHistoryMapper;
import cn.iocoder.sva.module.ai.service.model.AiChatRoleService;
import cn.iocoder.sva.module.ai.service.model.AiModelService;
import cn.iocoder.sva.module.ai.service.translation.helper.GlossaryHelper;
import cn.iocoder.sva.module.ai.service.translation.tran.LlmClientService;
import cn.iocoder.sva.module.ai.service.translation.tran.context.AiModelContext;
import cn.iocoder.sva.module.ai.service.translation.tran.context.ChatModelContext;
import cn.iocoder.sva.module.ai.service.translation.tran.context.PromptContext;
import cn.iocoder.sva.module.ai.service.translation.tran.context.RoleContext;
import cn.iocoder.sva.module.ai.service.translation.tran.context.TranslationCacheContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.sva.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.sva.module.ai.enums.ErrorCodeConstants.TRAN_TEXT_HISTORY_NOT_EXISTS;
import static cn.iocoder.sva.module.ai.enums.ErrorCodeConstants.TRAN_TEXT_TRANSLATE_FAILED;

/**
 * AI词句翻译 Service 实现类
 * <p>
 * 复用文档翻译已有的大模型调用（{@link LlmClientService}）、术语库（{@link GlossaryHelper}）
 * 与线程上下文机制，对单段文本执行同步翻译，并将结果写入历史表（每个用户仅保留最近 1000 条）。
 *
 * @author like
 */
@Service
@Validated
@Slf4j
public class TranTextServiceImpl implements TranTextService {

    /** 每个用户保留的最大历史记录数 */
    private static final int MAX_HISTORY_PER_USER = 1000;

    @Resource
    private TranTextHistoryMapper tranTextHistoryMapper;

    @Resource
    private LlmClientService llmClient;

    @Resource
    private AiModelService aiModelService;

    @Resource
    private AiChatRoleService chatRoleService;

    @Resource
    private GlossaryHelper glossaryHelper;

    @Resource
    private TranGlossaryItemService tranGlossaryItemService;

    @Resource
    private PermissionCommonApi permissionApi;

    @Override
    public TranTextTranslateRespVO translateText(TranTextTranslateReqVO reqVO) {
        String sourceText = reqVO.getSourceText().trim();
        String targetLang = reqVO.getTargetLang();
        Long modelId = reqVO.getModelId();
        Long roleId = reqVO.getRoleId();
        List<Long> glossaryIds = reqVO.getGlossaryIds();

        // 获取当前登录用户名（工号）
        String username = SecurityFrameworkUtils.getLoginUserUsername();
        if (StrUtil.isBlank(username)) {
            username = "system";
        }

        String modelName = null;
        String roleName = null;
        String targetText = null;

        try {
            // 1. 设置模型上下文（与文档翻译保持一致）
            if (modelId != null) {
                AiModelDO modelDO = aiModelService.getModel(modelId);
                ChatModel chatModel = aiModelService.getChatModel(modelId);
                ChatModelContext.set(chatModel);

                AiModelContext.ModelInfo modelInfo = new AiModelContext.ModelInfo();
                modelInfo.setModelId(modelId);
                modelInfo.setModelName(modelDO.getName());
                modelInfo.setModelCode(modelDO.getModel());
                modelInfo.setChatModel(chatModel);
                AiModelContext.set(modelInfo);

                modelName = modelDO.getName();
                log.info("[translateText] 使用自定义模型, modelId={}, modelName={}, modelCode={}",
                        modelId, modelDO.getName(), modelDO.getModel());
            }

            // 2. 设置场景（角色）上下文
            if (roleId != null) {
                RoleContext.set(roleId);
                AiChatRoleDO chatRole = chatRoleService.getChatRole(roleId);
                if (chatRole != null) {
                    roleName = chatRole.getName();
                }
            }

            // 3. 构建并缓存系统提示词
            String systemPrompt = buildSystemPrompt(targetLang, roleId);
            PromptContext.set(systemPrompt);

            // 4. 词句翻译每次都实时调用，禁用语义缓存，确保模型/场景/术语的选择不被历史缓存覆盖
            TranslationCacheContext.set(true);

            // 5. 构建术语映射表
            Map<String, String> glossaryMap = buildGlossaryMap(glossaryIds);

            // 6. 调用大模型翻译
            targetText = doTranslate(sourceText, targetLang, glossaryMap);
        } finally {
            // 清理所有 ThreadLocal，防止内存泄漏
            TranslationCacheContext.clear();
            ChatModelContext.clear();
            AiModelContext.clear();
            RoleContext.clear();
            PromptContext.clear();
        }

        // 7. 保存翻译历史（仅在翻译成功后）
        TranTextHistoryDO history = TranTextHistoryDO.builder()
                .sourceText(sourceText)
                .targetText(targetText)
                .targetLang(targetLang)
                .modelId(modelId)
                .modelName(modelName)
                .roleId(roleId)
                .roleName(roleName)
                .glossaryIds(joinIds(glossaryIds))
                .username(username)
                .build();
        tranTextHistoryMapper.insert(history);

        // 8. 裁剪历史，仅保留当前用户最近 MAX_HISTORY_PER_USER 条
        trimHistory(username);

        // 9. 组装返回结果
        TranTextTranslateRespVO respVO = BeanUtils.toBean(history, TranTextTranslateRespVO.class);
        respVO.setCreateTime(history.getCreateTime());
        return respVO;
    }

    /**
     * 调用大模型执行文本翻译，并对结果做质量清洗
     *
     * @param sourceText  原文
     * @param targetLang  目标语言
     * @param glossaryMap 术语映射表（可为空）
     * @return 译文
     */
    private String doTranslate(String sourceText, String targetLang, Map<String, String> glossaryMap) {
        // 关键：与文档翻译一致，先按原文筛选出【实际出现的术语】，避免把整个术语库无差别塞进提示词
        Map<String, String> relevantGlossary = extractRelevantTerms(sourceText, glossaryMap);

        LlmClientService.TranslateResult raw;
        if (!relevantGlossary.isEmpty()) {
            // 原文命中术语：发送纯净原文 + 相关术语表
            log.info("[doTranslate] 启用【术语约束翻译】，术语库总数={}, 原文相关术语数={}, 目标语言={}",
                    glossaryMap != null ? glossaryMap.size() : 0, relevantGlossary.size(), targetLang);
            raw = llmClient.translateWithGlossaryConstraint(sourceText, targetLang, relevantGlossary);
        } else {
            // 原文未命中任何术语：普通翻译，不附加术语约束
            log.info("[doTranslate] 原文未命中术语库中的任何术语，执行【普通翻译】，术语库总数={}, 目标语言={}",
                    glossaryMap != null ? glossaryMap.size() : 0, targetLang);
            raw = llmClient.translateOnce(sourceText, targetLang);
        }

        if (raw == null || (raw.getStatus() != null && raw.getStatus().startsWith("[ERROR]"))) {
            String msg = raw != null ? raw.getStatus() : "模型无响应";
            log.error("[doTranslate] 翻译失败: {}", msg);
            throw exception(TRAN_TEXT_TRANSLATE_FAILED, msg);
        }

        // 清洗或在违反目标语言约束时重试
        LlmClientService.TranslateResult sanitized =
                llmClient.sanitizeOrRetry(sourceText, targetLang, raw.getContent(), raw.getUsage());

        String targetText = sanitized.getContent();
        if (StrUtil.isBlank(targetText)) {
            log.error("[doTranslate] 模型未返回有效译文, sourceText前50字符='{}'",
                    sourceText.substring(0, Math.min(50, sourceText.length())));
            throw exception(TRAN_TEXT_TRANSLATE_FAILED, "模型未返回有效译文");
        }
        return targetText;
    }

    /**
     * 从术语库中提取原文实际出现的相关术语（与文档翻译 DocxTranslationServiceImpl 保持一致）
     * <p>
     * 仅保留 key（术语原文）作为子串出现在原文中的条目，避免把整个术语库无差别塞进提示词，
     * 导致大模型被大量不相干术语干扰。
     *
     * @param text     原文
     * @param glossary 术语库映射（原文术语 → 译文术语）
     * @return 原文中实际出现的术语映射，未命中时返回空 Map
     */
    private Map<String, String> extractRelevantTerms(String text, Map<String, String> glossary) {
        Map<String, String> relevantTerms = new LinkedHashMap<>();
        if (StrUtil.isBlank(text) || glossary == null || glossary.isEmpty()) {
            return relevantTerms;
        }
        for (Map.Entry<String, String> entry : glossary.entrySet()) {
            String sourceTerm = entry.getKey();
            if (StrUtil.isNotBlank(sourceTerm) && text.contains(sourceTerm)) {
                relevantTerms.put(sourceTerm, entry.getValue());
            }
        }
        log.info("[extractRelevantTerms] 原文长度={}, 术语库总数={}, 命中的相关术语数={}, 相关术语(原文→译文)={}",
                text.length(), glossary.size(), relevantTerms.size(), relevantTerms);
        return relevantTerms;
    }

    /**
     * 根据术语库ID列表构建术语映射表（源语言 -> 目标语言）
     *
     * @param glossaryIds 术语库ID列表
     * @return 术语映射表，无术语时返回空 Map
     */
    private Map<String, String> buildGlossaryMap(List<Long> glossaryIds) {
        if (CollUtil.isEmpty(glossaryIds)) {
            log.info("[buildGlossaryMap] 未选择术语库(glossaryIds为空)，跳过术语约束");
            return new LinkedHashMap<>();
        }
        log.info("[buildGlossaryMap] 开始加载术语库, glossaryIds={}", glossaryIds);
        // 合并多个术语库的术语条目（按 glossaryId + 原文去重）
        Map<String, TranGlossaryItemDO> mergedMap = new LinkedHashMap<>();
        for (Long glossaryId : glossaryIds) {
            if (glossaryId == null) {
                continue;
            }
            try {
                List<TranGlossaryItemDO> items =
                        tranGlossaryItemService.getTranGlossaryItemListByGlossaryId(glossaryId);
                int itemCount = items != null ? items.size() : 0;
                log.info("[buildGlossaryMap] 术语库 {} 查询到 {} 条术语", glossaryId, itemCount);
                if (CollUtil.isNotEmpty(items)) {
                    for (TranGlossaryItemDO item : items) {
                        mergedMap.put(glossaryId + "|" + item.getSourceLanguage(), item);
                    }
                }
            } catch (Exception e) {
                log.error("[buildGlossaryMap] 查询术语库 {} 失败", glossaryId, e);
            }
        }
        Map<String, String> glossaryMap = glossaryHelper.buildGlossaryMap(new ArrayList<>(mergedMap.values()));
        // 【调试日志】仅打印术语库加载总数；实际进入提示词的术语由 extractRelevantTerms 按原文筛选后打印
        log.info("[buildGlossaryMap] 合并去重后术语库共 {} 条术语（将按原文筛选后再加入提示词）", glossaryMap.size());
        return glossaryMap;
    }

    /**
     * 裁剪历史记录，仅保留指定用户最近 {@link #MAX_HISTORY_PER_USER} 条
     *
     * @param username 用户名（工号）
     */
    private void trimHistory(String username) {
        try {
            Long count = tranTextHistoryMapper.selectCountByUsername(username);
            if (count == null || count <= MAX_HISTORY_PER_USER) {
                return;
            }
            long excess = count - MAX_HISTORY_PER_USER;
            List<TranTextHistoryDO> oldest = tranTextHistoryMapper.selectOldestByUsername(username, excess);
            if (CollUtil.isNotEmpty(oldest)) {
                List<Long> ids = oldest.stream().map(TranTextHistoryDO::getId).collect(Collectors.toList());
                tranTextHistoryMapper.deleteByIds(ids);
                log.info("[trimHistory] 用户 {} 历史超过 {} 条，已删除最早的 {} 条",
                        username, MAX_HISTORY_PER_USER, ids.size());
            }
        } catch (Exception e) {
            // 裁剪失败不影响主流程
            log.error("[trimHistory] 裁剪用户 {} 的历史记录失败", username, e);
        }
    }

    /**
     * 构建系统提示词（与文档翻译逻辑保持一致）
     *
     * @param targetLanguage 目标语言
     * @param roleId         角色ID（可选）
     * @return 系统提示词
     */
    private String buildSystemPrompt(String targetLanguage, Long roleId) {
        // 1. 如果提供了角色ID，尝试从数据库加载角色的 systemMessage
        if (roleId != null) {
            try {
                AiChatRoleDO chatRole = chatRoleService.getChatRole(roleId);
                if (chatRole != null && StrUtil.isNotEmpty(chatRole.getSystemMessage())) {
                    return chatRole.getSystemMessage() + "\n\n目标语言：" + targetLanguage + "。";
                }
            } catch (Exception e) {
                log.error("[buildSystemPrompt] 加载角色提示词失败: roleId={}", roleId, e);
            }
        }
        // 2. 使用默认提示词
        return String.format(
                "你是专业的翻译助手。目标语言：%s。\n" +
                        "- 准确翻译原文内容，保持原意不变。\n" +
                        "- 严禁添加解释、示例或额外说明。\n" +
                        "- 仅返回翻译文本，不输出任何解释性语句、引导语或前缀。\n" +
                        "- 严格保留原文中的数字、日期、比例、缩写、单位、标点符号和格式结构。\n" +
                        "- 专业术语保持一致性，不要随意更改。\n",
                targetLanguage
        );
    }

    /**
     * 将术语库ID列表拼接为逗号分隔的字符串
     */
    private String joinIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return null;
        }
        return ids.stream().filter(java.util.Objects::nonNull)
                .map(String::valueOf).collect(Collectors.joining(","));
    }

    @Override
    public PageResult<TranTextHistoryDO> getHistoryPage(TranTextHistoryPageReqVO pageReqVO) {
        // 权限隔离：始终以后台登录态为准，忽略前端传入的 username，防止越权查询他人记录
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String loginUsername = SecurityFrameworkUtils.getLoginUserUsername();
        if (!isSuperAdmin(loginUserId)) {
            // 普通用户：强制只能查询自己的记录
            if (StrUtil.isBlank(loginUsername)) {
                // 兜底：拿不到登录用户名时返回空结果，绝不放行全部数据
                log.warn("[getHistoryPage] 未获取到登录用户名，返回空结果, loginUserId={}", loginUserId);
                return PageResult.empty();
            }
            pageReqVO.setUsername(loginUsername);
        }
        // 超级管理员：可传入 username 筛选指定用户，为空则查询全部
        return tranTextHistoryMapper.selectPage(pageReqVO);
    }

    /**
     * 判断指定用户是否为超级管理员（拥有 roleId=1）
     * <p>
     * loginUserId 为空或获取角色失败时，一律按【非超级管理员】处理，保证权限隔离的保守安全。
     *
     * @param loginUserId 登录用户ID
     * @return true=超级管理员，可查看全部记录
     */
    private boolean isSuperAdmin(Long loginUserId) {
        if (loginUserId == null) {
            return false;
        }
        try {
            Set<Long> userRoleIds = permissionApi.getLoginUserAllRoleIds(loginUserId).getData();
            return userRoleIds != null && userRoleIds.contains(1L);
        } catch (Exception e) {
            log.warn("[isSuperAdmin] 获取用户角色失败，按普通用户处理, loginUserId={}", loginUserId, e);
            return false;
        }
    }

    @Override
    public void deleteHistory(Long id) {
        TranTextHistoryDO history = tranTextHistoryMapper.selectById(id);
        if (history == null) {
            throw exception(TRAN_TEXT_HISTORY_NOT_EXISTS);
        }
        // 归属校验：普通用户只能删除自己的记录，防止越权删除他人记录（对外统一表现为“记录不存在”，避免泄露信息）
        String loginUsername = SecurityFrameworkUtils.getLoginUserUsername();
        if (!isSuperAdmin(SecurityFrameworkUtils.getLoginUserId())
                && !StrUtil.equals(history.getUsername(), loginUsername)) {
            log.warn("[deleteHistory] 用户 {} 尝试删除非本人记录 id={}（归属 {}），已拒绝",
                    loginUsername, id, history.getUsername());
            throw exception(TRAN_TEXT_HISTORY_NOT_EXISTS);
        }
        tranTextHistoryMapper.deleteById(id);
    }

    @Override
    public int clearHistory() {
        String username = SecurityFrameworkUtils.getLoginUserUsername();
        if (StrUtil.isBlank(username)) {
            return 0;
        }
        List<TranTextHistoryDO> list = tranTextHistoryMapper.selectList(
                TranTextHistoryDO::getUsername, username);
        if (CollUtil.isEmpty(list)) {
            return 0;
        }
        List<Long> ids = list.stream().map(TranTextHistoryDO::getId).collect(Collectors.toList());
        tranTextHistoryMapper.deleteByIds(ids);
        log.info("[clearHistory] 用户 {} 清空了 {} 条词句翻译历史", username, ids.size());
        return ids.size();
    }

}
