package cn.iocoder.sva.module.system.service.auth;


import java.util.Map;

/**
 * HR系统接口调用服务接口
 *
 * @author 闫理想
 * @since 2025/12/20
 */
public interface IKpHrService {

    /**
     * 调用HR系统接口
     *
     * @param url   接口路径（拼接在基础地址之后）
     * @param param 请求参数，JSON格式发送
     * @return 包含 data（业务数据）和 total（总记录数）的结果Map；请求失败时返回空Map
     */
    Map<String, Object> execute(String url, Map<String, Object> param);
}