package cn.iocoder.sva.framework.env.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 环境配置
 *
 * @author 科兴源码
 */
@ConfigurationProperties(prefix = "sva.env")
@Data
public class EnvProperties {

    public static final String TAG_KEY = "sva.env.tag";

    /**
     * 环境标签
     */
    private String tag;

}
