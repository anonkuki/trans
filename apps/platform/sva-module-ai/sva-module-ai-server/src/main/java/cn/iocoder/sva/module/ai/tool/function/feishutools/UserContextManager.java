package cn.iocoder.sva.module.ai.tool.function.feishutools;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.sva.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.sva.module.ai.dal.dataobject.user.AdminUserDO;
import cn.iocoder.sva.module.ai.dal.mysql.user.AdminUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 用户上下文管理器
 * 负责获取当前用户信息、会话ID等
 */
@Slf4j
public class UserContextManager {

    private static final String REDIS_KEY_CONVERSATION = "feishu:conversation:";
    private static final String REDIS_KEY_CURRENT_FEISHU_USER_ID = "feishu:user:current:";

    private final StringRedisTemplate stringRedisTemplate;
    private final AdminUserMapper adminUserMapper;

    public UserContextManager(StringRedisTemplate stringRedisTemplate, AdminUserMapper adminUserMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.adminUserMapper = adminUserMapper;
    }

    /**
     * 获取当前登录用户的用户名
     */
    public String getCurrentUsername() {
        try {
            // 先尝试从Security上下文获取
            String username = SecurityFrameworkUtils.getLoginUserUsername();
            if (username != null) {
                log.info("[getCurrentUsername] 从Security上下文获取到用户名: {}", username);
                return username;
            }

            // 飞书机器人场景：从 Redis 获取当前线程的飞书userId
            String feishuUserId = getCurrentFeishuUserIdFromRedis();
            if (StrUtil.isBlank(feishuUserId)) {
                log.warn("[getCurrentUsername] 无法获取飞书userId");
                return "未知用户";
            }

            log.info("[getCurrentUsername] 从Redis获取到飞书userId: {}", feishuUserId);
            return feishuUserId;

        } catch (Exception e) {
            log.error("[getCurrentUsername] 获取当前用户名失败", e);
            return "获取用户名失败";
        }
    }

    /**
     * 获取当前有效的用户名（优先Security，其次Redis）
     */
    public String getEffectiveUsername() {
        String username = SecurityFrameworkUtils.getLoginUserUsername();
        if (StrUtil.isBlank(username)) {
            username = getCurrentFeishuUserIdFromRedis();
        }
        return username;
    }

    /**
     * 获取当前会话ID
     */
    public String getCurrentChatId() {
        try {
            String username = getEffectiveUsername();
            if (StrUtil.isBlank(username)) {
                log.warn("[getCurrentChatId] 无法获取当前用户名，无法查询会话ID");
                return null;
            }

            String redisKey = REDIS_KEY_CONVERSATION + username;
            String conversationId = stringRedisTemplate.opsForValue().get(redisKey);

            if (StrUtil.isNotBlank(conversationId)) {
                log.info("[getCurrentChatId] 成功获取会话ID: username={}, conversationId={}", username, conversationId);
                return conversationId;
            } else {
                log.warn("[getCurrentChatId] Redis中未找到会话ID: username={}", username);
                return null;
            }
        } catch (Exception e) {
            log.error("[getCurrentChatId] 从Redis获取会话ID失败", e);
            return null;
        }
    }

    /**
     * 获取用户会话ID（指定用户名）
     */
    public String getUserChatId(String username) {
        if (StrUtil.isBlank(username)) {
            return null;
        }
        try {
            String redisKey = REDIS_KEY_CONVERSATION + username;
            return stringRedisTemplate.opsForValue().get(redisKey);
        } catch (Exception e) {
            log.error("[getUserChatId] 获取会话ID失败, username={}", username, e);
            return null;
        }
    }

    /**
     * 从 Redis 获取当前线程的飞书userId
     */
    private String getCurrentFeishuUserIdFromRedis() {
        try {
            String threadId = String.valueOf(Thread.currentThread().getId());
            String redisKey = REDIS_KEY_CURRENT_FEISHU_USER_ID + threadId;
            String feishuUserId = stringRedisTemplate.opsForValue().get(redisKey);

            if (StrUtil.isNotBlank(feishuUserId)) {
                log.debug("[getCurrentFeishuUserIdFromRedis] 获取到飞书userId: threadId={}, feishuUserId={}",
                        threadId, feishuUserId);
                return feishuUserId;
            }

            log.debug("[getCurrentFeishuUserIdFromRedis] Redis中未找到飞书userId: threadId={}", threadId);
            return null;
        } catch (Exception e) {
            log.error("[getCurrentFeishuUserIdFromRedis] 获取失败", e);
            return null;
        }
    }

    /**
     * 根据用户名查询用户ID
     */
    public Long getUserIdByUsername(String username) {
        try {
            if (StrUtil.isBlank(username)) {
                username = getEffectiveUsername();
            }

            if (StrUtil.isBlank(username)) {
                log.warn("[getUserIdByUsername] 无法获取用户名");
                return null;
            }

            AdminUserDO userDO = adminUserMapper.selectByUsername(username);

            if (userDO != null) {
                log.debug("[getUserIdByUsername] 查询成功, username={}, userId={}", username, userDO.getId());
                return userDO.getId();
            } else {
                log.warn("[getUserIdByUsername] 未找到用户, username={}", username);
                return null;
            }
        } catch (Exception e) {
            log.error("[getUserIdByUsername] 查询用户ID失败, username={}", username, e);
            return null;
        }
    }
}