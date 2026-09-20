package cn.iocoder.sva.module.system.config;

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
         * 飞书clientId
         */
        private String clientId;

        /**
         * 飞书clientSecret
         */
        private String clientSecret;

        private String appAccessTokenUrl;

        private String userInfoUrl;

        private String userAccessTokenUrl;

    }

}