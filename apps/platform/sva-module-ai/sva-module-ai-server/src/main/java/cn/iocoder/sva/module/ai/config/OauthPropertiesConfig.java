package cn.iocoder.sva.module.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

/**
 * Oauth 配置类
 */
@Data
@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "oauth")
public class OauthPropertiesConfig {

    /**
     * 飞书
     */
    private FeiShu feishu;

    @Data
    public static class FeiShu {
        /**
         * 应用 ID
         */
        private String clientId;

        /**
         * 应用密钥
         */
        private String clientSecret;

        /**
         * 获取 tenant_access_token 的 URL
         */
        private String tenantAccessTokenUrl = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";

        /**
         * 发送消息的 URL
         */
        private String sendMessageUrl = "https://open.feishu.cn/open-apis/im/v1/messages";

        /**
         * 回复消息的 URL
         */
        private String replyMessageUrl = "https://open.feishu.cn/open-apis/im/v1/messages/{message_id}/reply";

        /**
         * 下载文件的 URL
         */
        private String downloadFileUrl = "https://open.feishu.cn/open-apis/im/v1/messages/{message_id}/resources/{file_key}?type=file";

        /**
         * ===================== 【新增】流式卡片相关 API =====================
         */
        
        /**
         * 创建卡片实体的 URL
         */
        private String createCardUrl = "https://open.feishu.cn/open-apis/cardkit/v1/cards";

        /**
         * 更新卡片内容的 URL（流式更新文本）
         */
        private String updateCardContentUrl = "https://open.feishu.cn/open-apis/cardkit/v1/cards/{card_id}/elements/{element_id}/content";

        /**
         * 更新卡片配置的 URL（开启/关闭流式模式）
         */
        private String updateCardSettingsUrl = "https://open.feishu.cn/open-apis/cardkit/v1/cards/{card_id}/settings";

        /**
         * 回复方式：stream（流式卡片）/ text（普通文本），默认 stream
         */
        private String replyMode = "stream";

        /**
         * 飞书AI助手角色名称，默认：飞书AI助手
         */
        private String aiRoleName = "飞书AI助手";
    }

}