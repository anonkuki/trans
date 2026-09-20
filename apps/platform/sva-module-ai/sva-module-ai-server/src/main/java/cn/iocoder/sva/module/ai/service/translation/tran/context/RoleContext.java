package cn.iocoder.sva.module.ai.service.translation.tran.context;

import lombok.extern.slf4j.Slf4j;

/**
 * 翻译角色线程上下文
 * <p>
 * 用于在翻译任务中动态传递聊天角色ID，避免修改大量方法签名
 * 基于 InheritableThreadLocal 实现线程父子关系的传递
 */
@Slf4j
public class RoleContext {

    private static final InheritableThreadLocal<Long> ROLE_ID_HOLDER = new InheritableThreadLocal<>();

    /**
     * 设置当前线程的角色ID
     *
     * @param roleId 聊天角色ID
     */
    public static void set(Long roleId) {
        ROLE_ID_HOLDER.set(roleId);
        log.debug("[RoleContext] 设置角色ID: {}", roleId);
    }

    /**
     * 获取当前线程的角色ID
     *
     * @return 角色ID，如果未设置则返回 null
     */
    public static Long get() {
        Long roleId = ROLE_ID_HOLDER.get();
        if (roleId != null) {
            log.debug("[RoleContext] 获取角色ID: {}", roleId);
        }
        return roleId;
    }

    /**
     * 清除当前线程的角色ID
     * <p>
     * 重要：必须在任务完成后调用，防止内存泄漏
     */
    public static void clear() {
        Long roleId = ROLE_ID_HOLDER.get();
        ROLE_ID_HOLDER.remove();
        log.debug("[RoleContext] 清除角色ID: {}", roleId);
    }

    /**
     * 判断当前线程是否设置了角色ID
     *
     * @return true 如果已设置
     */
    public static boolean hasRoleId() {
        return ROLE_ID_HOLDER.get() != null;
    }
}
