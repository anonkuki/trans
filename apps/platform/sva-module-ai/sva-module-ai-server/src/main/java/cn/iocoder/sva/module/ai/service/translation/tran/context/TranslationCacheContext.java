package cn.iocoder.sva.module.ai.service.translation.tran.context;

import lombok.extern.slf4j.Slf4j;

/**
 * 翻译缓存控制线程上下文
 * <p>
 * 用于在翻译任务中动态传递缓存禁用标志，避免修改大量方法签名
 * 基于 InheritableThreadLocal 实现线程父子关系的传递
 */
@Slf4j
public class TranslationCacheContext {

    private static final InheritableThreadLocal<Boolean> DISABLE_CACHE_HOLDER = new InheritableThreadLocal<>();

    /**
     * 设置当前线程的缓存禁用标志
     *
     * @param disableCache true=禁用缓存，false=启用缓存
     */
    public static void set(Boolean disableCache) {
        DISABLE_CACHE_HOLDER.set(disableCache);
        log.debug("[TranslationCacheContext] 设置缓存禁用标志: {}", disableCache);
    }

    /**
     * 获取当前线程的缓存禁用标志
     *
     * @return true=禁用缓存，false=启用缓存，如果未设置则返回 false（默认启用缓存）
     */
    public static Boolean get() {
        Boolean disableCache = DISABLE_CACHE_HOLDER.get();
        if (disableCache != null) {
            log.debug("[TranslationCacheContext] 获取缓存禁用标志: {}", disableCache);
        }
        return disableCache != null ? disableCache : false;
    }

    /**
     * 清除当前线程的缓存禁用标志
     * <p>
     * 重要：必须在任务完成后调用，防止内存泄漏
     */
    public static void clear() {
        Boolean disableCache = DISABLE_CACHE_HOLDER.get();
        DISABLE_CACHE_HOLDER.remove();
        log.debug("[TranslationCacheContext] 清除缓存禁用标志: {}", disableCache);
    }

    /**
     * 判断当前线程是否设置了缓存禁用标志
     *
     * @return true 如果已设置
     */
    public static boolean hasDisableCache() {
        return DISABLE_CACHE_HOLDER.get() != null;
    }
}
