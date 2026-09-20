package cn.iocoder.sva.module.system.service.auth;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * HR系统接口调用服务，通过HTTP Basic认证访问HR系统REST接口
 *
 * @author 闫理想
 * @since 2025/12/20
 */
@Slf4j
@Service
public class KpHrServiceImpl implements IKpHrService {

    /** Basic认证凭证格式：username:password */
    private static final String CREDENTIALS_FORMAT = "%s:%s";
    /** 请求超时时间（毫秒） */
    private static final int REQUEST_TIMEOUT_MS = 60000;

    /** HR系统认证用户名 */
    @Value("${hr.request.username}")
    private String username;

    /** HR系统认证密码 */
    @Value("${hr.request.password}")
    private String password;

    /** HR系统基础请求地址 */
    @Value("${hr.request.url}")
    private String requestUrl;

    /**
     * 调用HR系统接口
     *
     * @param url   接口路径（拼接在基础地址之后）
     * @param param 请求参数，JSON格式发送
     * @return 包含 data（业务数据）和 total（总记录数）的结果Map；请求失败时返回空Map
     */
    @Override
    public Map<String, Object> execute(String url, Map<String, Object> param) {
        // 参数校验
        if (StrUtil.isBlank(url) || param == null) {
            log.warn("HR请求参数校验失败, url={}", url);
            return null;
        }

        String fullUrl = requestUrl + url;
        Map<String, Object> result = new HashMap<>();
        try {
            // 发送POST请求，携带Basic认证头
            HttpResponse response = HttpRequest.post(fullUrl)
                    .header("Content-Type", "application/json")
                    .header("Authorization", buildBasicAuth())
                    .body(JSONUtil.toJsonStr(param))
                    .timeout(REQUEST_TIMEOUT_MS)
                    .execute();

            if (!response.isOk()) {
                log.error("HR请求失败, url={}, 状态码={}", fullUrl, response.getStatus());
                return result;
            }

            // 解析HR系统返回的JSON，提取业务数据和总记录数
            JSONObject json = JSONUtil.parseObj(response.body());
            result.put("data", json.getStr("DATA"));
            result.put("total", json.getStr("TOTALROWCOUNT"));
            response.close();
        } catch (Exception e) {
            log.error("HR请求异常, url={}", fullUrl, e);
        }
        return result;
    }

    /**
     * 构建HTTP Basic认证头
     */
    private String buildBasicAuth() {
        String credentials = String.format(CREDENTIALS_FORMAT, username, password);
        return "Basic " + Base64.encode(credentials);
    }
}

