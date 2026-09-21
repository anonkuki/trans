package cn.iocoder.sva.framework.common.util.http;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.map.TableMap;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HTTP 工具类
 *
 * @author 科兴源码
 */
public class HttpUtils {

    /**
     * 编码 URL 参数
     *
     * @param value 参数
     * @return 编码后的参数
     */
    public static String encodeUtf8(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 解码 URL 参数（query parameter）
     * 注意：此方法会将 + 解码为空格，适用于 query parameter，不适用于 URL path
     *
     * @see #decodeUrlPath(String)
     * @param value 参数
     * @return 解码后的参数
     */
    public static String decodeUtf8(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    /**
     * 解码 URL 路径
     * 与 {@link #decodeUtf8(String)} 不同，此方法不会将 + 解码为空格，保持 + 为字面字符
     * 适用于 URL path 部分的解码
     * <p>
     * 此方法能安全处理以下特殊字符，不会因 URLDecoder 抛出 IllegalArgumentException：
     * <ul>
     *   <li>英文百分号 %（U+0025）：如 "文件100%.docx"、"6%汉译英.docx"</li>
     *   <li>中文全角百分号 ％（U+FF05）：统一转换为英文 % 后按同样逻辑处理</li>
     *   <li>不完整的编码序列：如路径末尾的 "%"、"%2" 等</li>
     *   <li>多个连续百分号：如 "100%%完成"</li>
     * </ul>
     *
     * @param path URL 路径
     * @return 解码后的路径
     */
    public static String decodeUrlPath(String path) {
        // 1. 将全角百分号 ％（U+FF05）统一转为半角 %（U+0025），后续统一处理
        String encoded = path.replace("\uFF05", "%");
        // 2. 将 + 替换为 %2B，避免被 URLDecoder 解码为空格
        encoded = encoded.replace("+", "%2B");
        // 3. 逐字符扫描：将不是合法 URL 编码序列的裸 % 替换为占位符
        //    合法序列：% 后紧跟两位 ASCII 十六进制数字（如 %20、%E4、%a9）
        //    裸百分号：% 在字符串末尾、后面不足2个字符、或后面不是两位十六进制数字
        String placeholder = "\u0000PERCENT\u0000";
        StringBuilder sb = new StringBuilder(encoded.length());
        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            if (c == '%') {
                if (i + 2 < encoded.length() && isAsciiHex(encoded.charAt(i + 1)) && isAsciiHex(encoded.charAt(i + 2))) {
                    // 合法的 URL 编码序列（如 %20、%E4），保留交给 URLDecoder 解码
                    sb.append(c);
                } else {
                    // 裸百分号，用占位符保护，避免 URLDecoder 抛出异常
                    sb.append(placeholder);
                }
            } else {
                sb.append(c);
            }
        }
        // 4. 解码合法的 URL 编码序列
        String decoded = URLDecoder.decode(sb.toString(), StandardCharsets.UTF_8);
        // 5. 将占位符还原为百分号
        return decoded.replace(placeholder, "%");
    }

    /**
     * 判断字符是否为 ASCII 十六进制数字（0-9, a-f, A-F）
     * 仅匹配 ASCII 范围内的字符，不使用正则表达式，确保跨平台行为一致
     */
    private static boolean isAsciiHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    @SuppressWarnings("unchecked")
    public static String replaceUrlQuery(String url, String key, String value) {
        UrlBuilder builder = UrlBuilder.of(url, Charset.defaultCharset());
        // 先移除
        TableMap<CharSequence, CharSequence> query = (TableMap<CharSequence, CharSequence>)
                ReflectUtil.getFieldValue(builder.getQuery(), "query");
        query.remove(key);
        // 后添加
        builder.addQuery(key, value);
        return builder.build();
    }

    public static String removeUrlQuery(String url) {
        if (!StrUtil.contains(url, '?')) {
            return url;
        }
        UrlBuilder builder = UrlBuilder.of(url, Charset.defaultCharset());
        // 移除 query、fragment
        builder.setQuery(null);
        builder.setFragment(null);
        return builder.build();
    }

    /**
     * 拼接 URL
     *
     * copy from Spring Security OAuth2 的 AuthorizationEndpoint 类的 append 方法
     *
     * @param base 基础 URL
     * @param query 查询参数
     * @param keys query 的 key，对应的原本的 key 的映射。例如说 query 里有个 key 是 xx，实际它的 key 是 extra_xx，则通过 keys 里添加这个映射
     * @param fragment URL 的 fragment，即拼接到 # 中
     * @return 拼接后的 URL
     */
    public static String append(String base, Map<String, ?> query, Map<String, String> keys, boolean fragment) {
        UriComponentsBuilder template = UriComponentsBuilder.newInstance();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(base);
        URI redirectUri;
        try {
            // assume it's encoded to start with (if it came in over the wire)
            redirectUri = builder.build(true).toUri();
        } catch (Exception e) {
            // ... but allow client registrations to contain hard-coded non-encoded values
            redirectUri = builder.build().toUri();
            builder = UriComponentsBuilder.fromUri(redirectUri);
        }
        template.scheme(redirectUri.getScheme()).port(redirectUri.getPort()).host(redirectUri.getHost())
                .userInfo(redirectUri.getUserInfo()).path(redirectUri.getPath());

        if (fragment) {
            StringBuilder values = new StringBuilder();
            if (redirectUri.getFragment() != null) {
                String append = redirectUri.getFragment();
                values.append(append);
            }
            for (String key : query.keySet()) {
                if (values.length() > 0) {
                    values.append("&");
                }
                String name = key;
                if (keys != null && keys.containsKey(key)) {
                    name = keys.get(key);
                }
                values.append(name).append("={").append(key).append("}");
            }
            if (values.length() > 0) {
                template.fragment(values.toString());
            }
            UriComponents encoded = template.build().expand(query).encode();
            builder.fragment(encoded.getFragment());
        } else {
            for (String key : query.keySet()) {
                String name = key;
                if (keys != null && keys.containsKey(key)) {
                    name = keys.get(key);
                }
                template.queryParam(name, "{" + key + "}");
            }
            template.fragment(redirectUri.getFragment());
            UriComponents encoded = template.build().expand(query).encode();
            builder.query(encoded.getQuery());
        }
        return builder.build().toUriString();
    }

    public static String[] obtainBasicAuthorization(HttpServletRequest request) {
        String clientId;
        String clientSecret;
        // 先从 Header 中获取
        String authorization = request.getHeader("Authorization");
        authorization = StrUtil.subAfter(authorization, "Basic ", true);
        if (StringUtils.hasText(authorization)) {
            authorization = Base64.decodeStr(authorization);
            clientId = StrUtil.subBefore(authorization, ":", false);
            clientSecret = StrUtil.subAfter(authorization, ":", false);
            // 再从 Param 中获取
        } else {
            clientId = request.getParameter("client_id");
            clientSecret = request.getParameter("client_secret");
        }

        // 如果两者非空，则返回
        if (StrUtil.isNotEmpty(clientId) && StrUtil.isNotEmpty(clientSecret)) {
            return new String[]{clientId, clientSecret};
        }
        return null;
    }

    /**
     * HTTP post 请求，基于 {@link cn.hutool.http.HttpUtil} 实现
     *
     * 为什么要封装该方法，因为 HttpUtil 默认封装的方法，没有允许传递 headers 参数
     *
     * @param url URL
     * @param headers 请求头
     * @param requestBody 请求体
     * @return 请求结果
     */
    public static String post(String url, Map<String, String> headers, String requestBody) {
        try (HttpResponse response = HttpRequest.post(url)
                .addHeaders(headers)
                .body(requestBody)
                .execute()) {
            return response.body();
        }
    }

    /**
     * HTTP get 请求，基于 {@link cn.hutool.http.HttpUtil} 实现
     *
     * 为什么要封装该方法，因为 HttpUtil 默认封装的方法，没有允许传递 headers 参数
     *
     * @param url URL
     * @param headers 请求头
     * @return 请求结果
     */
    public static String get(String url, Map<String, String> headers) {
        try (HttpResponse response = HttpRequest.get(url)
                .addHeaders(headers)
                .execute()) {
            return response.body();
        }
    }

}
