package cn.iocoder.sva.module.ai.framework.rpc.config;

import cn.iocoder.sva.module.infra.api.file.FileApi;
import cn.iocoder.sva.module.system.api.user.AdminUserApi;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration(value = "aiRpcConfiguration", proxyBeanMethods = false)
@EnableFeignClients(clients = {FileApi.class, AdminUserApi.class})
public class RpcConfiguration {
}
