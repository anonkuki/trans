package cn.iocoder.sva.framework.apilog.config;

import cn.iocoder.sva.framework.common.biz.infra.logger.ApiAccessLogCommonApi;
import cn.iocoder.sva.framework.common.biz.infra.logger.ApiErrorLogCommonApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * API 日志使用到 Feign 的配置项
 *
 * @author 科兴源码
 */
@AutoConfiguration
@EnableFeignClients(clients = {ApiAccessLogCommonApi.class, ApiErrorLogCommonApi.class}) // 主要是引入相关的 API 服务
public class YudaoApiLogRpcAutoConfiguration {
}
