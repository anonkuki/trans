package cn.iocoder.sva.module.ai.service.feishu;

import cn.iocoder.sva.module.ai.config.OauthPropertiesConfig;
import cn.iocoder.sva.module.ai.controller.admin.chat.vo.conversation.AiChatConversationCreateMyReqVO;
import cn.iocoder.sva.module.ai.controller.admin.chat.vo.conversation.AiChatConversationUpdateMyReqVO;
import cn.iocoder.sva.module.ai.controller.admin.chat.vo.message.AiChatMessageSendReqVO;
import cn.iocoder.sva.module.ai.controller.admin.chat.vo.message.AiChatMessageSendRespVO;
import cn.iocoder.sva.module.ai.dal.dataobject.chat.AiChatConversationDO;
import cn.iocoder.sva.module.ai.dal.dataobject.model.AiChatRoleDO;
import cn.iocoder.sva.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.sva.module.ai.dal.dataobject.user.AdminUserDO;
import cn.iocoder.sva.module.ai.dal.mysql.chat.AiChatConversationMapper;
import cn.iocoder.sva.module.ai.dal.mysql.user.AdminUserMapper;
import cn.iocoder.sva.module.ai.enums.model.AiModelTypeEnum;
import cn.iocoder.sva.module.ai.service.chat.AiChatConversationService;
import cn.iocoder.sva.module.ai.service.chat.AiChatMessageService;
import cn.iocoder.sva.module.ai.service.model.AiChatRoleService;
import cn.iocoder.sva.module.ai.service.model.AiModelService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class FeishuAiUtil {

    @Resource
    private AiChatConversationService aiChatConversationService;

    @Resource
    private AiChatMessageService aiChatMessageService;

    @Resource
    private AdminUserMapper adminUserMapper;

    @Resource
    private AiModelService aiModelService;

    @Resource
    private AiChatRoleService aiChatRoleService;

    @Resource
    private AiChatConversationMapper chatConversationMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private OauthPropertiesConfig oauthPropertiesConfig;

    /** Redis Key 前缀：飞书用户ID -> 系统用户ID */
    private static final String REDIS_KEY_USER_MAPPING = "feishu:user:mapping:";
    
    /** Redis Key 前缀：飞书用户ID -> 对话ID */
    private static final String REDIS_KEY_CONVERSATION = "feishu:conversation:";
    
    /** 缓存过期时间：72小时 */
    private static final long CACHE_EXPIRE_HOURS = 24 * 3;

    // ===================== 【新增】ThreadLocal 存储当前飞书 userId =====================
    private static final ThreadLocal<String> CURRENT_FEISHU_USER_ID = new ThreadLocal<>();

    /**
     * 获取当前线程的飞书 userId
     * 
     * @return 飞书 userId
     */
    public static String getCurrentFeishuUserId() {
        return CURRENT_FEISHU_USER_ID.get();
    }


    // 对话方法接收语言字符串，返回语言字符串
    public String handleMessageAi(String message, String userId) {
        // ===================== 【新增】设置当前飞书 userId 到 ThreadLocal =====================
        CURRENT_FEISHU_USER_ID.set(userId);
        log.debug("[handleMessageAi] 已设置飞书userId到ThreadLocal: {}", userId);
        
        try {
            Long loginUserId = getOrCreateUserId(userId);
            
            // 从 Redis 获取对话 ID
            Long chatConversationMy = getConversationFromRedis(userId);
            
            if (chatConversationMy == null) {
                // 没有对话记录，创建新对话
                log.info("[handleMessageAi] 用户 {} 创建新对话", loginUserId);
                
                // 获取或创建飞书AI助手角色
                Long roleId = getOrCreateFeishuAiRole();
                log.info("[handleMessageAi] 使用角色ID: {}", roleId);
                
                // 创建对话时指定 roleId
                AiChatConversationCreateMyReqVO createReqVO = new AiChatConversationCreateMyReqVO();
                createReqVO.setRoleId(roleId);
                chatConversationMy = aiChatConversationService.createChatConversationMy(createReqVO, loginUserId);
                log.info("[handleMessageAi] 创建对话成功, conversationId={}", chatConversationMy);

                AiModelDO defaultModel = aiModelService.getRequiredDefaultModel(AiModelTypeEnum.CHAT.getType());
                aiChatConversationService.updateChatConversationMy(
                    new AiChatConversationUpdateMyReqVO()
                        .setId(chatConversationMy)
                        .setTitle(message)
                        .setModelId(defaultModel.getId())
                        .setMaxTokens(1024)
                        .setMaxContexts(10)
                        .setSystemMessage("科兴AI应用平台飞书助手")
                        .setTemperature(0.8),
                    loginUserId);
                log.info("[handleMessageAi] 更新对话配置成功, modelId={}", defaultModel.getId());
                
                // 存入 Redis
                saveConversationToRedis(userId, chatConversationMy);
            } else {
                log.info("[handleMessageAi] 用户 {} 复用已有对话: {}", loginUserId, chatConversationMy);
            }

            log.info("[handleMessageAi] 开始发送消息, conversationId={}, content={}", chatConversationMy, message);
            AiChatMessageSendRespVO aiChatMessageSendRespVO = aiChatMessageService.sendMessage(
                new AiChatMessageSendReqVO()
                    .setConversationId(chatConversationMy)
                    .setContent(message)
                    .setUseContext(true),
                loginUserId);
            log.info("[handleMessageAi] 消息发送成功, 回复内容长度={}", 
                    aiChatMessageSendRespVO.getReceive().getContent().length());
            return aiChatMessageSendRespVO.getReceive().getContent();
            
        } finally {
            // ===================== 【新增】清理 ThreadLocal，防止内存泄漏 =====================
            CURRENT_FEISHU_USER_ID.remove();
            log.debug("[handleMessageAi] 已清理ThreadLocal");
        }
    }

    /**
     * 获取或创建飞书AI助手角色
     * 
     * @return 角色ID
     */
    private Long getOrCreateFeishuAiRole() {
        // 从配置文件获取角色名称
        String roleName = oauthPropertiesConfig.getFeishu().getAiRoleName();
        log.info("[getOrCreateFeishuAiRole] 开始获取飞书AI助手角色, roleName={}", roleName);
        
        try {
            // 1. 尝试查找已存在的飞书AI助手角色
            List<AiChatRoleDO> roles = 
                aiChatRoleService.getChatRoleListByName(roleName);
            
            if (roles != null && !roles.isEmpty()) {
                Long roleId = roles.get(0).getId();
                log.info("[getOrCreateFeishuAiRole] 找到已存在的飞书AI助手角色: roleId={}, roleName={}", 
                        roleId, roles.get(0).getName());
                
                // 打印角色配置的工具ID
                if (roles.get(0).getToolIds() != null && !roles.get(0).getToolIds().isEmpty()) {
                    log.info("[getOrCreateFeishuAiRole] 角色配置的工具ID列表: {}", roles.get(0).getToolIds());
                } else {
                    log.warn("[getOrCreateFeishuAiRole] 角色未配置任何工具ID");
                }
                
                return roleId;
            }
            
            // 2. 如果不存在，给出明确提示
            log.error("[getOrCreateFeishuAiRole] 未找到飞书AI助手角色，请先在数据库中创建");
            log.error("[getOrCreateFeishuAiRole] 角色名称应为: {}", roleName);
            log.error("[getOrCreateFeishuAiRole] 执行以下SQL创建角色:");
            log.error("[getOrCreateFeishuAiRole] INSERT INTO ai_chat_role (user_id, model_id, name, avatar, category, description, system_message, tool_ids) VALUES (...)");
            
            // 临时方案：返回 null，让系统使用默认配置（但这样就不会有工具了）
            return null;
            
        } catch (Exception e) {
            log.error("[getOrCreateFeishuAiRole] 获取飞书AI助手角色失败", e);
            return null;
        }
    }

    /**
     * 获取或创建用户 ID（使用 Redis 缓存）
     */
    private Long getOrCreateUserId(String userId) {
        log.debug("[getOrCreateUserId] 开始获取用户ID, userId={}", userId);
        // 1. 先从 Redis 查询
        String redisKey = REDIS_KEY_USER_MAPPING + userId;
        String cachedUserId = stringRedisTemplate.opsForValue().get(redisKey);
        
        if (cachedUserId != null) {
            log.debug("[getOrCreateUserId] 从Redis获取到用户ID: {}", cachedUserId);
            return Long.parseLong(cachedUserId);
        }
        
        // 2. Redis 中没有，从数据库查询
        log.debug("[getOrCreateUserId] Redis中未找到，从数据库查询");
        AdminUserDO adminUserDO = adminUserMapper.selectByUsername(userId);
        if (adminUserDO == null) {
            log.error("[getOrCreateUserId] 用户不存在: {}", userId);
            throw new RuntimeException("用户不存在: " + userId);
        }
        
        // 3. 存入 Redis
        stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(adminUserDO.getId()), 
                CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        log.debug("[getOrCreateUserId] 用户ID已缓存到Redis: userId={}, systemUserId={}", 
                userId, adminUserDO.getId());
        
        return adminUserDO.getId();
    }

    /**
     * 从 Redis 获取对话 ID
     */
    private Long getConversationFromRedis(String userId) {
        String redisKey = REDIS_KEY_CONVERSATION + userId;
        String conversationId = stringRedisTemplate.opsForValue().get(redisKey);
        
        if (conversationId == null) {
            log.debug("[getConversationFromRedis] Redis中未找到对话ID: userId={}", userId);
            return null;
        }
        
        log.debug("[getConversationFromRedis] 从Redis获取到对话ID: userId={}, conversationId={}", 
                userId, conversationId);
        return Long.parseLong(conversationId);
    }

    /**
     * 保存对话 ID 到 Redis
     */
    private void saveConversationToRedis(String userId, Long conversationId) {
        String redisKey = REDIS_KEY_CONVERSATION + userId;
        stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(conversationId), 
                CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        log.info("[saveConversationToRedis] 已保存对话 ID 到 Redis: userId={}, conversationId={}", 
                userId, conversationId);
    }

}
