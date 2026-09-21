package cn.iocoder.sva.module.ai.service.translation.tran.impl;

import cn.iocoder.sva.framework.common.util.json.JsonUtils;
import cn.iocoder.sva.module.ai.dal.dataobject.model.AiChatRoleDO;
import cn.iocoder.sva.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.sva.module.ai.enums.model.AiModelTypeEnum;
import cn.iocoder.sva.module.ai.service.model.AiChatRoleService;
import cn.iocoder.sva.module.ai.service.model.AiModelService;
import cn.iocoder.sva.module.ai.service.translation.tran.LlmClientService;
import cn.iocoder.sva.module.ai.service.translation.tran.config.TransDocProperties;
import cn.iocoder.sva.module.ai.service.translation.tran.context.AiModelContext;
import cn.iocoder.sva.module.ai.service.translation.tran.context.ChatModelContext;
import cn.iocoder.sva.module.ai.service.translation.tran.context.PromptContext;
import cn.iocoder.sva.module.ai.service.translation.tran.context.RoleContext;
import cn.iocoder.sva.module.ai.service.translation.tran.context.TranslationCacheContext;
import cn.iocoder.sva.module.ai.service.translation.tran.model.UsageStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Service
public class LlmClientServiceImpl implements LlmClientService {

    @Resource
    private AiModelService aiModelService;

    @Resource
    private AiChatRoleService chatRoleService;

    @Resource
    private TransDocProperties transDocProperties;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** Redis Key 前缀 */
    private static final String REDIS_KEY_PREFIX = "translation:llm_cache:";

    /** 缓存过期时间：1天 */
    private static final long CACHE_EXPIRE_DAYS = 1;


    /** 中文字符正则 */
    private static final Pattern CJK_PATTERN = Pattern.compile("[\\u4e00-\\u9fff]");
    /** 连续英文正则 */
    private static final Pattern ALPHA_SEQ6 = Pattern.compile("[A-Za-z]{6,}");

    /**
     * 获取当前使用的 ChatModel
     * <p>
     * 优先从 ThreadLocal 获取，如果没有则使用默认的聊天模型
     *
     * @return ChatModel 实例
     */
    private ChatModel getCurrentChatModel() {
        // 1. 优先从 ThreadLocal 获取（翻译任务中设置）
        ChatModel contextModel = ChatModelContext.get();
        if (contextModel != null) {
            log.info("[LlmClientService] 使用 ThreadLocal 中的 ChatModel: {}", contextModel.getClass().getSimpleName());
            return contextModel;
        }

        // 2. 使用默认的聊天模型
        try {
            AiModelDO defaultModel = aiModelService.getRequiredDefaultModel(AiModelTypeEnum.CHAT.getType());
            ChatModel defaultChatModel = aiModelService.getChatModel(defaultModel.getId());
            log.info("[LlmClientService] 使用默认 ChatModel: {}, modelId={}, modelName={}, platform={}",
                    defaultChatModel.getClass().getSimpleName(),
                    defaultModel.getId(),
                    defaultModel.getName(),
                    defaultModel.getPlatform());
            return defaultChatModel;
        } catch (Exception e) {
            log.error("[LlmClientService] 获取默认 ChatModel 失败", e);
            throw new RuntimeException("无法获取默认的聊天模型，请检查模型配置", e);
        }
    }

    // ===================== 翻译调用 =====================

    @Override
    public TranslateResult translateOnce(String text, String targetLanguage) {
        return doTranslate(text, targetLanguage, false, null);
    }

    @Override
    public TranslateResult translateOnce(String text, String targetLanguage, Long roleId) {
        return doTranslate(text, targetLanguage, false, roleId);
    }

    @Override
    public TranslateResult translateOnceStrict(String text, String targetLanguage) {
        return doTranslate(text, targetLanguage, true, null);
    }

    @Override
    public TranslateResult translateOnceStrict(String text, String targetLanguage, Long roleId) {
        return doTranslate(text, targetLanguage, true, roleId);
    }

    @Override
    public TranslateResult polishPreserveTerms(String text, String targetLanguage) {
        String systemPrompt = buildBasePrompt(targetLanguage, null) +
                "文本中术语已替换完毕，严禁改动术语；仅润色语法与流畅度。";

        return executeCall(systemPrompt, text, 0.2);
    }

    @Override
    public TranslateResult cachedCall(CallKind kind, String text, String targetLanguage) {
        String cacheKey = buildCacheKey(kind, targetLanguage, text);
        String redisKey = REDIS_KEY_PREFIX + cacheKey;

        // 从 ThreadLocal 获取缓存禁用标志
        boolean disableCache = TranslationCacheContext.get();

        // 如果禁用缓存，直接执行翻译
        if (disableCache) {
            log.debug("[缓存已禁用] key={}, 直接执行翻译", cacheKey);
            TranslateResult result;
            switch (kind) {
                case STRICT:
                    result = translateOnceStrict(text, targetLanguage);
                    break;
                case POLISH:
                    result = polishPreserveTerms(text, targetLanguage);
                    break;
                default:
                    result = translateOnce(text, targetLanguage);
            }
            return result;
        }

        // 从 Redis 获取缓存
        String cachedJson = stringRedisTemplate.opsForValue().get(redisKey);
        if (cachedJson != null) {
            try {
                TranslateResult cached = JsonUtils.parseObject(cachedJson, TranslateResult.class);

                // 检查缓存内容是否有效
                if (cached == null || cached.getContent() == null) {
                    log.warn("[Redis缓存无效] key={}, content为null，删除缓存", cacheKey);
                    stringRedisTemplate.delete(redisKey);
                } else if (cached.getContent().isEmpty()) {
                    log.warn("[Redis缓存无效] key={}, content为空字符串，删除缓存", cacheKey);
                    stringRedisTemplate.delete(redisKey);
                } else {
                    log.debug("[Redis缓存命中] key={}, content长度={}, status={}",
                            cacheKey, cached.getContent().length(), cached.getStatus());
                    return cached;
                }
            } catch (Exception e) {
                log.warn("[Redis缓存解析失败] key={}, error={}", cacheKey, e.getMessage());
                stringRedisTemplate.delete(redisKey);
            }
        }

        log.debug("[Redis缓存未命中] key={}", cacheKey);

        // 执行翻译
        TranslateResult result;
        switch (kind) {
            case STRICT:
                result = translateOnceStrict(text, targetLanguage);
                break;
            case POLISH:
                result = polishPreserveTerms(text, targetLanguage);
                break;
            default:
                result = translateOnce(text, targetLanguage);
        }

        // 验证翻译结果是否有效
        if (result == null || result.getContent() == null) {
            log.warn("[Redis缓存跳过] key={}, 翻译结果为null，不存入缓存", cacheKey);
            return result != null ? result : new TranslateResult("", new UsageStats(), "[ERROR] 翻译结果为null");
        }

        // 如果结果是空字符串或包含错误标记，不存入缓存
        if (result.getContent().isEmpty() || result.getStatus().startsWith("[ERROR]")) {
            log.warn("[Redis缓存跳过] key={}, 翻译失败(status={})，不存入缓存", cacheKey, result.getStatus());
            return result;
        }

        // 存入 Redis，设置1天过期
        try {
            String resultJson = JsonUtils.toJsonString(result);
            stringRedisTemplate.opsForValue().set(redisKey, resultJson, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            log.debug("[Redis缓存设置] key={}, content长度={}, expire={}天", cacheKey, result.getContent().length(), CACHE_EXPIRE_DAYS);
        } catch (Exception e) {
            log.warn("[Redis缓存设置失败] key={}, error={}", cacheKey, e.getMessage());
        }

        return result;
    }

    @Override
    public TranslateResult sanitizeOrRetry(String text, String targetLanguage,
                                           String rawContent, UsageStats usage) {
        rawContent = rawContent != null ? rawContent : "";
        usage = usage != null ? usage : new UsageStats();

        // 仅检查是否违反目标语言约束
        if (!violatesTargetLanguage(rawContent, targetLanguage)) {
            return new TranslateResult(rawContent, usage, "OK");
        }

        // 重试：使用严格模式
        log.warn("[sanitizeOrRetry] 翻译输出违反目标语言约束，使用严格模式重试: violatesTargetLanguage=true, content前50字符='{}'",
                rawContent.substring(0, Math.min(50, rawContent.length())));
        TranslateResult retryResult = translateOnceStrict(text, targetLanguage);

        // 合并用量
        UsageStats merged = mergeUsage(usage, retryResult.getUsage());

        // 检查重试结果
        if (!violatesTargetLanguage(retryResult.getContent(), targetLanguage)) {
            return new TranslateResult(retryResult.getContent(), merged, "OK");
        }

        // 仍然失败：返回原文而非空字符串，确保文档中不出现空白
        log.error("[sanitizeOrRetry] 严格模式重试仍然失败，返回原文: '{}'",
                text != null ? text.substring(0, Math.min(50, text.length())) : "(null)");
        return new TranslateResult(text != null ? text : "", merged, "[WARN] 重试失败，保留原文");
    }

    // ===================== 缓存管理 =====================

    @Override
    public int getCacheSize() {
        // Redis 无法直接获取特定前缀的键数量，返回 -1 表示不支持
        log.warn("[getCacheSize] Redis 缓存不支持直接获取大小");
        return -1;
    }

    @Override
    public void clearCache() {
        // 删除所有翻译缓存
        try {
            Set<String> keys = stringRedisTemplate.keys(REDIS_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                Long deletedCount = stringRedisTemplate.delete(keys);
                log.info("[clearCache] 已清空翻译缓存，删除 {} 条记录", deletedCount);
            } else {
                log.info("[clearCache] 翻译缓存为空");
            }
        } catch (Exception e) {
            log.error("[clearCache] 清空翻译缓存失败", e);
        }
    }

    @Override
    public LlmClientService withChatModel(ChatModel newChatModel) {
        // 这个方法在 ThreadLocal 方案下不再需要，保留接口兼容性
        log.warn("[withChatModel] 此方法已废弃，请使用 ChatModelContext.set() 设置 ChatModel");
        return this;
    }

    // ===================== 内部方法 =====================

    /**
     * 执行翻译
     */
    private TranslateResult doTranslate(String text, String targetLanguage, boolean strict, Long roleId) {
        String systemPrompt = buildBasePrompt(targetLanguage, roleId);
        if (strict) {
            systemPrompt += "必须逐字翻译，禁止任何解释，只输出纯文本。";
        }

        double temperature = strict ? 0.0 : 0.2;
        return executeCall(systemPrompt, text, temperature);
    }

    /**
     * 执行 LLM 调用
     */
    private TranslateResult executeCall(String systemPrompt, String text, double temperature) {
        long startTime = System.currentTimeMillis();
        try {
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(systemPrompt));
            messages.add(new UserMessage(text));

            ChatModel currentModel = getCurrentChatModel();

            // 如果是 OpenAI 兼容模式或 DashScope，需要指定模型名称
            OpenAiChatOptions options;
            boolean needsModelParameter = currentModel instanceof OpenAiChatModel
                    || currentModel.getClass().getName().contains("DashScope");

            if (needsModelParameter) {
                // 尝试从 ThreadLocal 或默认模型中获取模型代码
                String modelCode = getModelCodeFromContext();
                if (modelCode != null && !modelCode.isEmpty()) {
                    log.info("[LLM调用] {} 模式，使用模型: {}",
                            currentModel.getClass().getSimpleName(), modelCode);
                    options = OpenAiChatOptions.builder()
                            .temperature(temperature)
                            .model(modelCode)
                            .build();
                } else {
                    log.warn("[LLM调用] {} 模式，但未找到模型代码，使用默认配置",
                            currentModel.getClass().getSimpleName());
                    options = OpenAiChatOptions.builder()
                            .temperature(temperature)
                            .build();
                }
            } else {
                options = OpenAiChatOptions.builder()
                        .temperature(temperature)
                        .build();
            }

            Prompt prompt = new Prompt(messages, options);

            // 【新增】检测是否为 QC 调用，如果是则打印完整提示词
            boolean isQcCall = systemPrompt != null && (
                    systemPrompt.contains("scientific copy editor") ||
                    systemPrompt.contains("质量审校")
            );

            if (isQcCall) {
                log.info("[QC调用检测] 检测到 QC 专用提示词");
                log.info("[QC-系统提示词] \n{}", systemPrompt);
            }

            // 记录调用前的详细信息
            log.info("[LLM调用开始] model={}, temperature={}, text长度={}",
                    currentModel.getClass().getSimpleName(),
                    temperature,
                    text != null ? text.length() : 0);

            ChatResponse response = currentModel.call(prompt);

            // 计算请求耗时
            long elapsedTime = System.currentTimeMillis() - startTime;

            String content = response.getResult().getOutput().getText();
            if (content != null) {
                content = content.strip();
            }

            UsageStats usage = extractUsage(response);

            // 【日志】打印发给AI的完整内容（系统提示词 + 用户原文）和AI返回的完整内容
            log.info("[LLM翻译] 耗时: {}ms\n" +
                    "===== 发给AI的完整内容（System Prompt + User Message）=====\n{}\n{}\n" +
                    "===== AI返回完整内容 =====\n{}\n" +
                    "===== END =====",
                    elapsedTime,
                    systemPrompt != null ? systemPrompt : "(null)",
                    text != null ? text : "(null)",
                    content != null ? content : "(null)");

            return new TranslateResult(content, usage, "OK");

        } catch (Exception e) {
            // 计算异常时的耗时
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.error("[LLM调用失败] error={}, exceptionType={}, 耗时={}ms", e.getMessage(), e.getClass().getName(), elapsedTime, e);
            // 重要：返回空字符串而不是原文，避免将未翻译的原文存入缓存
            return new TranslateResult("", new UsageStats(), "[ERROR] " + e.getMessage());
        }
    }

    /**
     * 从上下文中获取模型代码
     */
    private String getModelCodeFromContext() {
        try {
            // 优先从 AiModelContext 中获取模型代码
            AiModelContext.ModelInfo modelInfo = AiModelContext.get();
            if (modelInfo != null && modelInfo.getModelCode() != null && !modelInfo.getModelCode().isEmpty()) {
                log.info("[getModelCodeFromContext] 从 AiModelContext 获取模型代码: {}", modelInfo.getModelCode());
                return modelInfo.getModelCode();
            }

            // 如果 AiModelContext 中没有，尝试从 ChatModelContext 推断
            // TODO: 后续可以增强此逻辑，从 ChatModel 实例中提取模型信息

            // 最后返回默认值（用于通义千问兼容模式调用 DeepSeek）
            log.warn("[getModelCodeFromContext] 未找到模型代码，使用默认值: deepseek-v3");
            return "deepseek-v3";
        } catch (Exception e) {
            log.warn("[getModelCodeFromContext] 获取模型代码失败，使用默认值", e);
            return "deepseek-v3";
        }
    }

    /**
     * 构建基础提示词
     * <p>
     * 注意：此方法不再查询数据库，提示词由 TranServiceImpl 在任务开始时构建并缓存到 PromptContext
     *
     * @param targetLanguage 目标语言
     * @param roleId         聊天角色ID（已废弃，仅保留参数兼容性）
     * @return 系统提示词
     */
    private String buildBasePrompt(String targetLanguage, Long roleId) {
        // 1. 优先从 PromptContext 获取缓存的提示词（避免重复查询数据库）
        String cachedPrompt = PromptContext.get();
        if (cachedPrompt != null) {
            log.debug("[buildBasePrompt] 使用缓存的提示词");
            return cachedPrompt;
        }

        // 2. 如果缓存未命中，直接使用默认提示词（不再查询数据库）
        log.warn("[buildBasePrompt] PromptContext 缓存未命中，使用默认提示词。这不应该发生，请检查 TranServiceImpl 是否正确设置了缓存。");
        String defaultPrompt = String.format(
                "你是专业的翻译助手。目标语言：%s。\n" +
                        "- 准确翻译原文内容，保持原意不变。\n" +
                        "- 严禁添加解释、示例或额外说明。\n" +
                        "- 仅返回翻译文本，不输出任何解释性语句、引导语或前缀。\n" +
                        "- 严格保留原文中的数字、日期、比例、缩写、单位、标点符号和格式结构。\n" +
                        "- 专业术语保持一致性，不要随意更改。\n",
                targetLanguage
        );
        log.debug("[buildBasePrompt] 最终提示词:\n{}", defaultPrompt);
        return defaultPrompt;
    }

    /**
     * 提取用量信息
     */
    private UsageStats extractUsage(ChatResponse response) {
        UsageStats stats = new UsageStats();
        if (response.getMetadata() != null) {
            stats.setPromptTokens(toInt(response.getMetadata().getUsage().getPromptTokens(), 0));
            stats.setCompletionTokens(toInt(response.getMetadata().getUsage().getCompletionTokens(), 0));
            stats.setTotalTokens(stats.getPromptTokens() + stats.getCompletionTokens());
        }
        return stats;
    }

    /**
     * 合并用量统计
     */
    private UsageStats mergeUsage(UsageStats u1, UsageStats u2) {
        UsageStats merged = new UsageStats();
        merged.setPromptTokens(u1.getPromptTokens() + u2.getPromptTokens());
        merged.setCompletionTokens(u1.getCompletionTokens() + u2.getCompletionTokens());
        merged.setTotalTokens(u1.getTotalTokens() + u2.getTotalTokens());
        merged.setPromptCacheHitTokens(u1.getPromptCacheHitTokens() + u2.getPromptCacheHitTokens());
        merged.setPromptCacheMissTokens(u1.getPromptCacheMissTokens() + u2.getPromptCacheMissTokens());
        return merged;
    }


    /**
     * 检查是否违反目标语言约束
     */
    private boolean violatesTargetLanguage(String content, String targetLanguage) {
        if (content == null) return true;

        if (targetLanguage.toLowerCase().startsWith("english")) {
            // 目标是英文，不应出现中文
            return CJK_PATTERN.matcher(content).find();
        }
        if (targetLanguage.toLowerCase().startsWith("chinese")) {
            // 目标是中文，允许少量英文单位/缩写
            // 优化策略：
            // 1. 如果内容很短（3个及以内单词），即使全是英文也认为是正常的（可能是短语无需翻译）
            // 2. 提取所有连续的英文单词（长度>=2）
            // 3. 计算英文内容占比，如果超过30%才认为违规
            // 4. 或者单个英文单词长度超过20才认为违规（可能是整句未翻译），但引号内的内容除外

            // 先检查是否是短句（3个及以内单词）
            // 按空格分割，过滤空字符串，统计单词数
            String[] words = content.trim().split("\\s+");
            int wordCount = 0;
            boolean hasLongEnglishWord = false;
            for (String word : words) {
                if (!word.isEmpty()) {
                    wordCount++;
                    // 去掉标点符号后检查纯字母部分长度
                    String lettersOnly = word.replaceAll("[^A-Za-z]", "");
                    if (lettersOnly.length() > 4) {
                        hasLongEnglishWord = true;
                    }
                }
            }

            // 如果是3个及以内的单词，且没有超过4个字母的英文单词
            // 认为是缩写/短标记（如 GMP、API、SU），允许不翻译
            // 但如果包含完整英文单词（如 Glossary），则视为未翻译
            if (wordCount <= 3 && !hasLongEnglishWord) {
                log.debug("[violatesTargetLanguage] 短缩写(单词数={})，允许包含英文: {}", wordCount, content);
                return false;
            }

            // 先检查是否有超长的英文单词（可能整句未翻译）
            // 排除全大写缩写（如 WorldBioHazTec）和首字母大写专有名词（如 Moderna, Novartis）
            // 只匹配全小写的超长单词（这才是真正的未翻译英文句子）
            Pattern longEnglishWord = Pattern.compile("[a-z]{30,}");
            java.util.regex.Matcher longMatcher = longEnglishWord.matcher(content);
            while (longMatcher.find()) {
                String matchedText = longMatcher.group();
                int startIndex = longMatcher.start();

                // 检查这个长单词是否在引号内
                boolean isInQuotes = false;
                // 向前查找最近的引号
                int lastQuoteBefore = -1;
                for (int i = startIndex - 1; i >= 0; i--) {
                    char c = content.charAt(i);
                    if (c == '"' || c == '"' || c == '\'') {
                        lastQuoteBefore = i;
                        break;
                    }
                }
                // 向后查找最近的引号
                int nextQuoteAfter = -1;
                for (int i = startIndex + matchedText.length(); i < content.length(); i++) {
                    char c = content.charAt(i);
                    if (c == '"' || c == '"' || c == '\'') {
                        nextQuoteAfter = i;
                        break;
                    }
                }

                // 如果前后都有引号，说明在引号内，允许
                if (lastQuoteBefore != -1 && nextQuoteAfter != -1) {
                    isInQuotes = true;
                    log.debug("[violatesTargetLanguage] 检测到引号内的长文本，允许: {}", matchedText);
                }

                // 如果不在引号内，判定为违规
                if (!isInQuotes) {
                    log.warn("[violatesTargetLanguage] 检测到超长英文单词(>=20字符)且不在引号内，判定为违规: {}", content);
                    return true;
                }
            }

            // 提取所有连续英文片段（长度>=2），排除缩写、专有名词和引号内的内容
            Pattern englishFragments = Pattern.compile("[A-Za-z]{2,}");
            java.util.regex.Matcher matcher = englishFragments.matcher(content);
            int totalEnglishLength = 0;
            while (matcher.find()) {
                String fragment = matcher.group();

                // 跳过全大写缩写（如 GMP, API, USA, CQV）
                if (fragment.equals(fragment.toUpperCase())) {
                    continue;
                }
                // 跳过首字母大写的专有名词（如 Moderna, Novartis, Pfizer, Colombia）
                // 特征：首字母大写且其余含小写，且长度合理（<=15字符）
                if (Character.isUpperCase(fragment.charAt(0)) && fragment.length() <= 15
                        && fragment.substring(1).matches(".*[a-z].*")) {
                    continue;
                }
                int startIndex = matcher.start();

                // 检查这个英文片段是否在引号内
                boolean isInQuotes = false;
                // 向前查找最近的引号
                int lastQuoteBefore = -1;
                for (int i = startIndex - 1; i >= 0; i--) {
                    char c = content.charAt(i);
                    if (c == '"' || c == '"' || c == '\'') {
                        lastQuoteBefore = i;
                        break;
                    }
                }
                // 向后查找最近的引号
                int nextQuoteAfter = -1;
                for (int i = startIndex + fragment.length(); i < content.length(); i++) {
                    char c = content.charAt(i);
                    if (c == '"' || c == '"' || c == '\'') {
                        nextQuoteAfter = i;
                        break;
                    }
                }

                // 如果前后都有引号，说明在引号内，不计入英文占比
                if (lastQuoteBefore != -1 && nextQuoteAfter != -1) {
                    log.debug("[violatesTargetLanguage] 引号内的英文片段，不计入占比: {}", fragment);
                } else {
                    // 不在引号内，计入英文占比
                    totalEnglishLength += fragment.length();
                }
            }

            // 计算英文占比
            double englishRatio = content.isEmpty() ? 0 : (double) totalEnglishLength / content.length();

            // 如果英文占比超过 60%，认为翻译失败
            // 阈值提高是因为中文译文天然较短，公司名/缩写等专有名词占比容易偏高
            if (englishRatio > 0.6) {
                log.warn("[violatesTargetLanguage] 英文占比过高({:.1%})，判定为违规: {}", englishRatio, content);
                return true;
            }

            // 否则认为是正常的（包含专有名词、缩写、引号内文本等）
            return false;
        }
        return false;
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(CallKind kind, String targetLanguage, String text) {
        return kind.name() + ":" + targetLanguage + ":" + text.hashCode();
    }

    /**
     * 安全转换为整数
     */
    private int toInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // ===================== 术语约束翻译模式 =====================

    @Override
    public TranslateResult translateWithGlossaryConstraint(String text, String targetLanguage,
                                                            Map<String, String> glossaryMap) {
        // 构建术语约束翻译的系统提示词
        String systemPrompt = buildConstraintPrompt(targetLanguage, glossaryMap);

        // 【调试日志】打印原文和术语信息
        log.info("[术语约束翻译-输入信息] 原文长度={}, 术语数={}, 目标语言={}, 原文前100字符='{}'",
                text != null ? text.length() : 0,
                glossaryMap != null ? glossaryMap.size() : 0,
                targetLanguage,
                text != null ? text.substring(0, Math.min(100, text.length())) : "null");
        // 【调试日志】打印实际传给大模型的完整系统提示词，便于核对术语对照表是否正确拼入
        log.info("[术语约束翻译-完整提示词] 目标语言={}, 术语数={}, systemPrompt=\n{}",
                targetLanguage, glossaryMap != null ? glossaryMap.size() : 0, systemPrompt);

        // 执行翻译调用（temperature=0.2，保持一定的灵活性）
        return executeCall(systemPrompt, text, 0.2);
    }

    /**
     * 构建术语约束翻译的提示词
     * <p>
     * 提示词结构：
     * 1. 基础提示词（从数据库配置或默认值获取）
     * 2. 术语约束说明
     * 3. 术语对照表（仅当前段落涉及的）
     * 4. 目标语言
     *
     * @param targetLanguage 目标语言
     * @param glossaryMap    术语映射表
     * @return 系统提示词
     */
    private String buildConstraintPrompt(String targetLanguage, Map<String, String> glossaryMap) {
        StringBuilder prompt = new StringBuilder();

        // 1. 先获取基础提示词（从 PromptContext 或默认值）
        String basePrompt = PromptContext.get();
        if (basePrompt != null && !basePrompt.isEmpty()) {
            log.debug("[术语约束翻译] 使用数据库配置的基础提示词");
            prompt.append(basePrompt);
            prompt.append("\n\n");
        } else {
            // 如果缓存未命中，使用默认基础提示词
            log.warn("[术语约束翻译] PromptContext 缓存未命中，使用默认基础提示词");
            prompt.append(String.format(
                    "你是专业的医学/GMP文档翻译专家，精通中英文技术术语。\n" +
                    "【翻译要求】\n" +
                    "- 译文必须正式、严谨，符合GMP文档规范\n" +
                    "- 准确传达原文含义，不得增删或曲解\n" +
                    "- 严格保留原文中的数字、日期、比例、单位、标点符号和格式\n" +
                    "- 仅返回翻译文本，不输出任何解释、说明或引导语\n\n"
            ));
        }

        // 2. 术语约束说明（这是术语约束模式特有的）
        prompt.append("【术语使用要求】\n");
        prompt.append("- 以下术语对照表列出了必须使用的专业术语\n");
        prompt.append("- 翻译时必须在译文中使用对应的英文术语，不得使用其他表达方式\n");
        prompt.append("- 如果原文中出现了术语表中的中文术语，译文必须使用对应的英文术语\n");
        prompt.append("- 术语的一致性至关重要，请严格遵守\n\n");

        // 3. 术语对照表（如果有）
        if (glossaryMap != null && !glossaryMap.isEmpty()) {
            prompt.append("【术语对照表】\n");
            int termCount = 0;
            for (Map.Entry<String, String> entry : glossaryMap.entrySet()) {
                prompt.append(String.format("- %s → %s\n", entry.getKey(), entry.getValue()));
                termCount++;
                // 限制最多显示50个术语，避免提示词过长
                if (termCount >= 50) {
                    prompt.append("... (术语过多，仅显示前50个)\n");
                    break;
                }
            }
            prompt.append("\n");
        } else {
            prompt.append("【术语对照表】\n");
            prompt.append("本次翻译无特定术语约束，请使用标准医学术语进行翻译。\n\n");
        }

        // 4. 目标语言说明
        prompt.append(String.format("【目标语言】%s\n\n", targetLanguage));

        // 5. 开始翻译指令
        prompt.append("请翻译以下文本，严格遵守上述术语约束：\n");

        String finalPrompt = prompt.toString();

        return finalPrompt;
    }
}
