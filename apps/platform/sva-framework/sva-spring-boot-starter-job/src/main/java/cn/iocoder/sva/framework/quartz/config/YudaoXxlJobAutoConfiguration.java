package cn.iocoder.sva.framework.quartz.config;

import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * XXL-Job 自动配置类
 *
 * @author 科兴源码
 */
@AutoConfiguration
@ConditionalOnClass(XxlJobSpringExecutor.class)
@ConditionalOnProperty(prefix = "xxl.job", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({XxlJobProperties.class})
@EnableScheduling // 开启 Spring 自带的定时任务
@Slf4j
public class YudaoXxlJobAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public XxlJobExecutor xxlJobExecutor(XxlJobProperties properties) {
        log.info("========== XXL-Job 配置开始加载 ==========");

        // 1. 打印从配置文件读取的原始配置
        log.info("【从配置文件读取的配置】");

        // 1.1 Admin 配置
        XxlJobProperties.AdminProperties admin = properties.getAdmin();
        log.info("  - admin.addresses: {}", admin.getAddresses());

        // 1.2 Executor 配置
        XxlJobProperties.ExecutorProperties executor = properties.getExecutor();
        log.info("  - executor.app-name: {}", executor.getAppName());
        log.info("  - executor.ip: {}", executor.getIp());
        log.info("  - executor.port: {}", executor.getPort());
        log.info("  - executor.log-path: {}", executor.getLogPath());
        log.info("  - executor.log-retention-days: {}", executor.getLogRetentionDays());

        // 1.3 AccessToken 配置
        String accessToken = properties.getAccessToken();
        if (accessToken != null && !accessToken.isEmpty()) {
            log.info("  - access-token: {} (已配置)", maskToken(accessToken));
        } else {
            log.info("  - access-token: 未配置");
        }

        // 2. 参数校验和警告
        log.info("【配置校验结果】");

        if (executor.getAppName() == null || executor.getAppName().isEmpty()) {
            log.error("❌ executor.app-name 未配置，执行器将无法注册到调度中心！");
        } else {
            log.info("✅ executor.app-name 已配置: {}", executor.getAppName());
        }

        if (executor.getIp() == null || executor.getIp().isEmpty()) {
            log.warn("⚠️ executor.ip 未配置，将使用自动获取的 IP 地址。如果自动获取失败，请手动配置 executor.ip");
        } else {
            log.info("✅ executor.ip 已手动配置: {}", executor.getIp());
        }

        if (executor.getPort() == null || executor.getPort() <= 0) {
            log.error("❌ executor.port 未配置或无效，将使用默认端口 9999");
        } else {
            log.info("✅ executor.port 已配置: {}", executor.getPort());
        }

        if (admin.getAddresses() == null || admin.getAddresses().isEmpty()) {
            log.error("❌ admin.addresses 未配置，执行器无法连接到调度中心！");
        } else {
            log.info("✅ admin.addresses 已配置: {}", admin.getAddresses());
        }

        log.info("========== 开始初始化 XXL-Job 执行器 ==========");
        log.info("【设置到 XxlJobExecutor 的参数】");
        log.info("  - setAppname: {}", executor.getAppName());
        log.info("  - setIp: {}", executor.getIp());
        log.info("  - setPort: {}", executor.getPort());
        log.info("  - setLogPath: {}", executor.getLogPath());
        log.info("  - setLogRetentionDays: {}", executor.getLogRetentionDays());
        log.info("  - setAdminAddresses: {}", admin.getAddresses());
        log.info("  - setAccessToken: {}", accessToken != null && !accessToken.isEmpty() ? "已设置 (长度: " + accessToken.length() + ")" : "未设置");

        // 初始化执行器
        XxlJobExecutor xxlJobExecutor = new XxlJobSpringExecutor();

        try {
            // 设置配置
            if (executor.getIp() != null && !executor.getIp().isEmpty()) {
                xxlJobExecutor.setIp(executor.getIp());
            }
            xxlJobExecutor.setPort(executor.getPort());
            xxlJobExecutor.setAppname(executor.getAppName());
            xxlJobExecutor.setLogPath(executor.getLogPath());
            xxlJobExecutor.setLogRetentionDays(executor.getLogRetentionDays());
            xxlJobExecutor.setAdminAddresses(admin.getAddresses());
            xxlJobExecutor.setAccessToken(properties.getAccessToken());

            log.info("✅ XXL-Job 执行器参数设置完成");
            log.info("========== XXL-Job 配置加载完成 ==========");

        } catch (Exception e) {
            log.error("❌ XXL-Job 执行器配置设置失败", e);
            throw e;
        }

        return xxlJobExecutor;
    }

    /**
     * 脱敏处理 token，只显示前4位和后4位
     */
    private String maskToken(String token) {
        if (token == null || token.isEmpty()) {
            return "";
        }
        if (token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }
}