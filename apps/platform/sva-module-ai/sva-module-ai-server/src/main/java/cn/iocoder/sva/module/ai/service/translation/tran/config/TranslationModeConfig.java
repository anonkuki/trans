package cn.iocoder.sva.module.ai.service.translation.tran.config;

import cn.iocoder.sva.framework.common.pojo.CommonResult;
import cn.iocoder.sva.module.infra.api.config.ConfigApi;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 翻译模式配置工具类
 * <p>
 * 负责从配置中心读取翻译模式开关，支持双模式切换：
 * - 旧模式（术语替换）：将中文术语直接替换为英文后发给大模型翻译
 * - 新模式（术语约束）：发送纯净中文原文 + 术语表，让大模型在理解句意后使用术语
 */
@Slf4j
@Component
public class TranslationModeConfig {

    @Resource
    private ConfigApi configApi;

    /**
     * 配置键名：翻译模式开关
     */
    private static final String TRANSLATION_FLAG_KEY = "translation.flag";

    /**
     * 判断是否使用术语约束翻译模式（新模式）
     * <p>
     * 规则：
     * - 如果 translation.flag 的值为 "0" 或 0，返回 true（使用新模式）
     * - 如果值为其他（"1"、null、未配置等），返回 false（使用旧模式）
     *
     * @return true=使用术语约束翻译模式，false=使用术语替换模式
     */
    public boolean isConstraintMode() {
        try {
            // 从配置中心获取配置值
            CommonResult<String> result = configApi.getConfigValueByKey(TRANSLATION_FLAG_KEY);

            // 如果配置不存在或获取失败，默认使用旧模式
            if (result == null || !result.isSuccess() || result.getData() == null) {
                log.debug("[TranslationModeConfig] 配置项 {} 不存在或获取失败，使用默认模式（术语替换）",
                        TRANSLATION_FLAG_KEY);
                return false;
            }

            String flagValue = result.getData().trim();

            // 判断是否为 "0" 或 0
            boolean isConstraintMode = "0".equals(flagValue);

            log.info("[TranslationModeConfig] 翻译模式: flag={}, mode={}",
                    flagValue, isConstraintMode ? "术语约束翻译" : "术语替换");

            return isConstraintMode;

        } catch (Exception e) {
            log.error("[TranslationModeConfig] 读取配置失败，使用默认模式（术语替换）", e);
            // 异常情况下默认使用旧模式
            return false;
        }
    }

    /**
     * 获取当前翻译模式的描述
     *
     * @return 模式描述
     */
    public String getModeDescription() {
        return isConstraintMode() ? "术语约束翻译模式" : "术语替换模式";
    }
}
