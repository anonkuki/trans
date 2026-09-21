package cn.iocoder.sva.module.ai.service.calendar;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;
import cn.iocoder.sva.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.sva.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.sva.module.ai.config.OauthPropertiesConfig;
import cn.iocoder.sva.module.ai.controller.admin.calendar.vo.CalendarPageReqVO;
import cn.iocoder.sva.module.ai.controller.admin.calendar.vo.CalendarSaveReqVO;
import cn.iocoder.sva.module.ai.dal.dataobject.calendar.CalendarDO;
import cn.iocoder.sva.module.ai.dal.mysql.calendar.CalendarMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static cn.iocoder.sva.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.sva.module.ai.enums.ErrorCodeConstants.CALENDAR_NOT_EXISTS;

/**
 * 飞书日历 Service 实现类
 *
 * @author like
 */
@Slf4j
@Service
@Validated
public class CalendarServiceImpl implements CalendarService {

    @Resource
    private CalendarMapper calendarMapper;

    @Resource
    private OauthPropertiesConfig oauthPropertiesConfig;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RestTemplate restTemplate;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String ACCESS_TOKEN_KEY_PREFIX = "access_token:";
    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh_token:";

    @Override
    public Map<String, String> getFeishuCalendarConfig() {
        Map<String, String> config = new HashMap<>();

        try {
            // 获取飞书日历配置
            OauthPropertiesConfig.Calendar calendarConfig = oauthPropertiesConfig.getCalendar();

            if (calendarConfig != null && calendarConfig.getFeishu() != null) {
                OauthPropertiesConfig.FeishuCalendar feishuCalendar = calendarConfig.getFeishu();

                config.put("clientId", feishuCalendar.getClientId());
//                config.put("clientSecret", feishuCalendar.getClientSecret());
                config.put("getCode", feishuCalendar.getGetCode());
                config.put("callback", feishuCalendar.getCallback());
                config.put("userAccessTokenUrl", feishuCalendar.getUserAccessTokenUrl());
                config.put("getCalendarList", feishuCalendar.getGetCalendarList());
                config.put("getCalendarListItem", feishuCalendar.getGetCalendarListItem());

                log.info("[getFeishuCalendarConfig] 成功获取飞书日历配置");
            } else {
                log.warn("[getFeishuCalendarConfig] 飞书日历配置为空");
            }
        } catch (Exception e) {
            log.error("[getFeishuCalendarConfig] 获取飞书日历配置失败", e);
        }

        return config;
    }

    @Override
    public Map<String, Object> exchangeAccessToken(String code) throws Exception {
        log.info("[exchangeAccessToken] 开始换取 access_token");

        // 获取飞书日历配置
        OauthPropertiesConfig.Calendar calendarConfig = oauthPropertiesConfig.getCalendar();
        if (calendarConfig == null || calendarConfig.getFeishu() == null) {
            throw new Exception("飞书日历配置为空");
        }

        OauthPropertiesConfig.FeishuCalendar feishuCalendar = calendarConfig.getFeishu();
        String userAccessTokenUrl = feishuCalendar.getUserAccessTokenUrl();
        String clientId = feishuCalendar.getClientId();
        String clientSecret = feishuCalendar.getClientSecret();
        String redirectUri = feishuCalendar.getCallback();

        log.info("[exchangeAccessToken] 配置信息: url={}, clientId={}, redirectUri={}",
                userAccessTokenUrl, clientId, redirectUri);

        // 构建请求体（使用表单格式，不是JSON）
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "authorization_code");
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("code", code);
        requestBody.add("redirect_uri", redirectUri);

        // 打印请求参数（隐藏敏感信息）
        log.info("[exchangeAccessToken] 请求参数已构建, grant_type=authorization_code, client_id={}, redirect_uri={}",
                clientId, redirectUri);

        // 设置请求头（使用表单格式）
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        // 发送请求，使用String接收响应避免类型转换问题
        ResponseEntity<String> response = restTemplate.postForEntity(
                userAccessTokenUrl,
                requestEntity,
                String.class
        );

        // 打印HTTP状态码
        log.info("[exchangeAccessToken] HTTP响应状态码: {}", response.getStatusCode());

        // 处理响应
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            String responseBodyStr = response.getBody();
            // 手动解析JSON
            Map<String, Object> responseBody = objectMapper.readValue(responseBodyStr, Map.class);

            // 检查响应码
            if (Integer.valueOf(0).equals(responseBody.get("code"))) {
                log.info("[exchangeAccessToken] 成功获取 access_token");

                // 获取当前登录用户名
                String username = SecurityFrameworkUtils.getLoginUserUsername();
                if (username == null || username.isEmpty()) {
                    throw new Exception("未获取到当前登录用户信息");
                }

                // 提取access_token和refresh_token
                String accessToken = (String) responseBody.get("access_token");
                String refreshToken = (String) responseBody.get("refresh_token");

                // 处理expires_in，可能是Integer或Long类型
                Object expiresInObj = responseBody.get("expires_in");
                Integer expiresIn = expiresInObj instanceof Number ? ((Number) expiresInObj).intValue() : 7200;

                // 处理refresh_token_expires_in，可能不存在
                Object refreshTokenExpiresInObj = responseBody.get("refresh_token_expires_in");
                Integer refreshTokenExpiresIn = refreshTokenExpiresInObj instanceof Number ?
                    ((Number) refreshTokenExpiresInObj).intValue() : 2592000; // 默认30天

                log.info("[exchangeAccessToken] 过期时间: expiresIn={}s, refreshTokenExpiresIn={}s",
                        expiresIn, refreshTokenExpiresIn);

                // 存储到Redis
                String accessTokenKey = ACCESS_TOKEN_KEY_PREFIX + username;
                String refreshTokenKey = REFRESH_TOKEN_KEY_PREFIX + username;

                stringRedisTemplate.opsForValue().set(accessTokenKey, accessToken, expiresIn, TimeUnit.SECONDS);
                stringRedisTemplate.opsForValue().set(refreshTokenKey, refreshToken, refreshTokenExpiresIn, TimeUnit.SECONDS);

                log.info("[exchangeAccessToken] 成功保存token到Redis, username={}, accessTokenKey={}, refreshTokenKey={}, expiresIn={}s, refreshTokenExpiresIn={}s",
                        username, accessTokenKey, refreshTokenKey, expiresIn, refreshTokenExpiresIn);

                return Map.of("authorized", true, "expires_in", expiresIn);
            } else {
                String errorMsg = String.format("获取access_token失败, code=%s, msg=%s",
                        responseBody.get("code"), responseBody.get("msg"));
                log.error("[exchangeAccessToken] {}", errorMsg);
                throw new Exception(errorMsg);
            }
        } else {
            throw new Exception("请求失败，HTTP状态码: " + response.getStatusCode());
        }
    }

    @Override
    public Map<String, Object> getOrRefreshAccessToken() throws Exception {
        // 获取当前登录用户名
        String username = SecurityFrameworkUtils.getLoginUserUsername();
        if (username == null || username.isEmpty()) {
            throw new Exception("未获取到当前登录用户信息");
        }

        String accessTokenKey = ACCESS_TOKEN_KEY_PREFIX + username;
        String refreshTokenKey = REFRESH_TOKEN_KEY_PREFIX + username;

        // 1. 检查Redis中是否有access_token
        String accessToken = stringRedisTemplate.opsForValue().get(accessTokenKey);
        if (accessToken != null && !accessToken.isEmpty()) {
            log.info("[getOrRefreshAccessToken] Redis中存在有效的access_token");
            Map<String, Object> result = new HashMap<>();
            result.put("access_token", accessToken);
            result.put("source", "redis");
            return result;
        }

        // 2. 检查是否有refresh_token
        String refreshToken = stringRedisTemplate.opsForValue().get(refreshTokenKey);
        if (refreshToken != null && !refreshToken.isEmpty()) {
            log.info("[getOrRefreshAccessToken] Redis中存在refresh_token，开始刷新");
            return refreshAccessToken(refreshToken);
        }

        // 3. 都没有，需要重新授权
        log.info("[getOrRefreshAccessToken] Redis中无token，需要重新授权");
        throw new Exception("NEED_REAUTH");
    }

    /**
     * 使用refresh_token刷新access_token
     */
    private Map<String, Object> refreshAccessToken(String refreshToken) throws Exception {
        log.info("[refreshAccessToken] 开始刷新access_token");

        // 获取飞书日历配置
        OauthPropertiesConfig.Calendar calendarConfig = oauthPropertiesConfig.getCalendar();
        if (calendarConfig == null || calendarConfig.getFeishu() == null) {
            throw new Exception("飞书日历配置为空");
        }

        OauthPropertiesConfig.FeishuCalendar feishuCalendar = calendarConfig.getFeishu();
        String userAccessTokenUrl = feishuCalendar.getUserAccessTokenUrl();
        String clientId = feishuCalendar.getClientId();
        String clientSecret = feishuCalendar.getClientSecret();

        log.info("[refreshAccessToken] 配置信息: url={}, clientId={}", userAccessTokenUrl, clientId);

        // 构建请求体（使用表单格式）
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "refresh_token");
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("refresh_token", refreshToken);

        log.info("[refreshAccessToken] 请求参数已构建, grant_type=refresh_token, client_id={}", clientId);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        // 发送请求
        ResponseEntity<String> response = restTemplate.postForEntity(
                userAccessTokenUrl,
                requestEntity,
                String.class
        );

        log.info("[refreshAccessToken] HTTP响应状态码: {}", response.getStatusCode());

        // 处理响应
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            String responseBodyStr = response.getBody();
            // 手动解析JSON
            Map<String, Object> responseBody = objectMapper.readValue(responseBodyStr, Map.class);

            // 检查响应码
            if (Integer.valueOf(0).equals(responseBody.get("code"))) {
                // 获取当前登录用户名
                String username = SecurityFrameworkUtils.getLoginUserUsername();
                if (username == null || username.isEmpty()) {
                    throw new Exception("未获取到当前登录用户信息");
                }

                // 提取新的access_token和refresh_token
                String newAccessToken = (String) responseBody.get("access_token");
                String newRefreshToken = (String) responseBody.get("refresh_token");

                // 处理expires_in
                Object expiresInObj = responseBody.get("expires_in");
                Integer expiresIn = expiresInObj instanceof Number ? ((Number) expiresInObj).intValue() : 7200;

                // 处理refresh_token_expires_in
                Object refreshTokenExpiresInObj = responseBody.get("refresh_token_expires_in");
                Integer refreshTokenExpiresIn = refreshTokenExpiresInObj instanceof Number ?
                    ((Number) refreshTokenExpiresInObj).intValue() : 2592000;

                log.info("[refreshAccessToken] 过期时间: expiresIn={}s, refreshTokenExpiresIn={}s",
                        expiresIn, refreshTokenExpiresIn);

                // 存储到Redis
                String accessTokenKey = ACCESS_TOKEN_KEY_PREFIX + username;
                String refreshTokenKey = REFRESH_TOKEN_KEY_PREFIX + username;

                stringRedisTemplate.opsForValue().set(accessTokenKey, newAccessToken, expiresIn, TimeUnit.SECONDS);
                if (newRefreshToken != null) {
                    stringRedisTemplate.opsForValue().set(refreshTokenKey, newRefreshToken, refreshTokenExpiresIn, TimeUnit.SECONDS);
                }

                log.info("[refreshAccessToken] 成功保存新token到Redis, username={}", username);

                return responseBody;
            } else {
                String errorMsg = String.format("刷新access_token失败, code=%s, msg=%s",
                        responseBody.get("code"), responseBody.get("msg"));
                log.error("[refreshAccessToken] {}", errorMsg);
                throw new Exception(errorMsg);
            }
        } else {
            throw new Exception("请求失败，HTTP状态码: " + response.getStatusCode());
        }
    }

    @Override
    public Map<String, Object> getCalendarList(String accessToken, Integer pageSize, String pageToken, String syncToken) throws Exception {
        log.info("[getCalendarList] 开始查询日历列表, pageSize={}, pageToken={}, syncToken={}",
                pageSize, pageToken, syncToken);

        // 获取飞书日历配置
        OauthPropertiesConfig.Calendar calendarConfig = oauthPropertiesConfig.getCalendar();
        if (calendarConfig == null || calendarConfig.getFeishu() == null) {
            throw new Exception("飞书日历配置为空");
        }

        String getCalendarListUrl = calendarConfig.getFeishu().getGetCalendarList();

        // 构建URL参数
        StringBuilder urlBuilder = new StringBuilder(getCalendarListUrl);
        urlBuilder.append("?page_size=").append(pageSize != null ? pageSize : 1000);
        if (pageToken != null && !pageToken.isEmpty()) {
            urlBuilder.append("&page_token=").append(pageToken);
        }
        if (syncToken != null && !syncToken.isEmpty()) {
            urlBuilder.append("&sync_token=").append(syncToken);
        }

        String fullUrl = urlBuilder.toString();
        log.info("[getCalendarList] 请求URL: {}", fullUrl);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        // 发送请求
        ResponseEntity<String> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.GET,
                requestEntity,
                String.class
        );

        log.info("[getCalendarList] HTTP响应状态码: {}", response.getStatusCode());

        // 处理响应
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            String responseBodyStr = response.getBody();
            // 手动解析JSON
            Map<String, Object> responseBody = objectMapper.readValue(responseBodyStr, Map.class);

            return responseBody;
        } else {
            throw new Exception("请求失败，HTTP状态码: " + response.getStatusCode());
        }
    }

    @Override
    public Map<String, Object> getCalendarEvents(String accessToken, String calendarId, Long startTime, Long endTime,
                                                  Long anchorTime, Integer pageSize, String pageToken, String syncToken) throws Exception {
        log.info("[getCalendarEvents] 开始查询日程事件, calendarId={}, startTime={}, endTime={}, anchorTime={}",
                calendarId, startTime, endTime, anchorTime);

        // 获取飞书日历配置
        OauthPropertiesConfig.Calendar calendarConfig = oauthPropertiesConfig.getCalendar();
        if (calendarConfig == null || calendarConfig.getFeishu() == null) {
            throw new Exception("飞书日历配置为空");
        }

        String getCalendarListItemUrl = calendarConfig.getFeishu().getGetCalendarListItem();

        // 替换URL中的calendar_id占位符
        String baseUrl = getCalendarListItemUrl.replace(":calendar_id", calendarId);

        log.info("[getCalendarEvents] 接收到的参数: calendarId={}, startTime={}, endTime={}, anchorTime={}, pageSize={}, pageToken={}, syncToken={}",
                calendarId, startTime, endTime, anchorTime, pageSize, pageToken, syncToken);

        // 构建URL参数
        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        urlBuilder.append("?");

        boolean firstParam = true;
        if (startTime != null) {
            urlBuilder.append("start_time=").append(startTime);
            firstParam = false;
        }
        if (endTime != null) {
            if (!firstParam) urlBuilder.append("&");
            urlBuilder.append("end_time=").append(endTime);
            firstParam = false;
        }
        // anchor_time用于分页，与start_time可以同时存在
        if (anchorTime != null) {
            // anchor_time是飞书日历分页专用的时间锚点，必须传它才能返回page_token
            if (!firstParam) urlBuilder.append("&");
            urlBuilder.append("anchor_time=").append(anchorTime);
            firstParam = false;
        }
        if (pageSize != null) {
            // 飞书API要求page_size范围为50-1000
            int validPageSize = Math.max(50, Math.min(1000, pageSize));
            if (!firstParam) urlBuilder.append("&");
            urlBuilder.append("page_size=").append(validPageSize);
            firstParam = false;
        } else {
            if (!firstParam) urlBuilder.append("&");
            urlBuilder.append("page_size=500");
            firstParam = false;
        }
        if (pageToken != null && !pageToken.isEmpty()) {
            urlBuilder.append("&page_token=").append(pageToken);
        }
        if (syncToken != null && !syncToken.isEmpty()) {
            urlBuilder.append("&sync_token=").append(syncToken);
        }
        // 默认使用open_id作为user_id_type
        urlBuilder.append("&user_id_type=open_id");

        String fullUrl = urlBuilder.toString();
        log.info("[getCalendarEvents] 请求URL: {}", fullUrl);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        // 发送请求
        ResponseEntity<String> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.GET,
                requestEntity,
                String.class
        );

        log.info("[getCalendarEvents] HTTP响应状态码: {}", response.getStatusCode());

        // 处理响应
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            String responseBodyStr = response.getBody();
            // 手动解析JSON
            Map<String, Object> responseBody = objectMapper.readValue(responseBodyStr, Map.class);

            // 飞书API返回的数据结构是 {code, data: {has_more, items, page_token}, msg}
            // 需要返回data对象，而不是整个responseBody
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
            if (data != null) {
                log.info("[getCalendarEvents] has_more={}, page_token={}",
                        data.get("has_more"), data.get("page_token"));
                return data;  // 返回data对象
            } else {
                log.warn("[getCalendarEvents] data对象为null，返回原始responseBody");
                return responseBody;
            }
        } else {
            throw new Exception("请求失败，HTTP状态码: " + response.getStatusCode());
        }
    }

    @Override
    public List<Map<String, Object>> exportCalendarEvents(String calendarId, Long startTime, Long endTime,
                                                           List<String> eventIds, String status, String sortOrder, Integer pageSize) throws Exception {
        log.info("[exportCalendarEvents] 开始导出日程事件, calendarId={}, startTime={}, endTime={}, status={}, sortOrder={}, pageSize={}",
                calendarId, startTime, endTime, status, sortOrder, pageSize);
        log.info("[exportCalendarEvents] 接收到的eventIds参数: {}", eventIds);

        // 先获取或刷新access_token
        Map<String, Object> tokenResult = getOrRefreshAccessToken();
        String accessToken = (String) tokenResult.get("access_token");

        List<Map<String, Object>> allEvents = new ArrayList<>();

        // 判断查询模式
        boolean isTimeRangeQuery = (startTime != null && endTime != null); // 时间范围查询
        boolean isPaginationQuery = (startTime != null && endTime == null); // 分页查询（仅开始时间）

        if (isTimeRangeQuery) {
            // 时间范围查询：先尝试一次性查询500条
            log.info("[exportCalendarEvents] 模式：时间范围查询，先尝试一次性查询");
            try {
                Map<String, Object> result = getCalendarEvents(accessToken, calendarId, startTime, endTime, null, 500, null, null);

                // 提取items列表（result本身就是data对象）
                if (result != null && result.get("items") instanceof List) {
                    allEvents = (List<Map<String, Object>>) result.get("items");
                    log.info("[exportCalendarEvents] 第一次查询获取到{}条事件", allEvents.size());
                }

                // 如果达到了500条，可能需要分页查询更多数据
                if (allEvents.size() >= 500) {
                    log.info("[exportCalendarEvents] 已达到500条，启动分页查询模式以获取完整数据");
                    allEvents = fetchAllEventsByPagination(accessToken, calendarId, startTime, endTime, 200);
                }
            } catch (Exception e) {
                // 如果是401错误，尝试重新获取token后再试一次
                if (e.getMessage() != null && e.getMessage().contains("99991677")) {
                    log.warn("[exportCalendarEvents] Token过期，尝试重新获取token");
                    tokenResult = getOrRefreshAccessToken();
                    accessToken = (String) tokenResult.get("access_token");

                    Map<String, Object> result = getCalendarEvents(accessToken, calendarId, startTime, endTime, null, 500, null, null);
                    if (result != null && result.get("items") instanceof List) {
                        allEvents = (List<Map<String, Object>>) result.get("items");
                        log.info("[exportCalendarEvents] 重试后获取到{}条事件", allEvents.size());
                    }

                    if (allEvents.size() >= 500) {
                        log.info("[exportCalendarEvents] 已达到500条，启动分页查询模式以获取完整数据");
                        allEvents = fetchAllEventsByPagination(accessToken, calendarId, startTime, endTime, 200);
                    }
                } else {
                    throw e;
                }
            }
        } else if (isPaginationQuery) {
            // 分页查询：递归查询所有页
            log.info("[exportCalendarEvents] 模式：分页查询，递归获取所有数据");
            allEvents = fetchAllEventsByPagination(accessToken, calendarId, startTime, null, 200);
        } else {
            // 没有时间筛选：查询默认数量
            log.info("[exportCalendarEvents] 模式：无时间筛选，查询默认数量");
            int limit = pageSize != null ? pageSize : 500;
            Map<String, Object> result = getCalendarEvents(accessToken, calendarId, null, null, null, limit, null, null);

            if (result != null && result.get("items") instanceof List) {
                allEvents = (List<Map<String, Object>>) result.get("items");
                log.info("[exportCalendarEvents] 获取到{}条事件", allEvents.size());
            }
        }

        // 先按eventIds过滤
        List<Map<String, Object>> filteredEvents;
        if (eventIds != null && !eventIds.isEmpty()) {
            filteredEvents = allEvents.stream()
                    .filter(event -> eventIds.contains(event.get("event_id")))
                    .collect(Collectors.toList());
            log.info("[exportCalendarEvents] 按eventIds过滤后剩余{}条事件", filteredEvents.size());
        } else {
            filteredEvents = allEvents;
        }

        // 再按status过滤
        List<Map<String, Object>> exportEvents;
        if (status != null && !status.isEmpty()) {
            exportEvents = filteredEvents.stream()
                    .filter(event -> status.equals(event.get("status")))
                    .collect(Collectors.toList());
            log.info("[exportCalendarEvents] 按status='{}'过滤后剩余{}条事件", status, exportEvents.size());
        } else {
            exportEvents = filteredEvents;
        }

        // 先按开始时间升序排序（与Controller中的逻辑一致）
        exportEvents.sort((a, b) -> {
            Long timeA = getStartTimeTimestamp(a);
            Long timeB = getStartTimeTimestamp(b);
            return timeA.compareTo(timeB);
        });
        log.info("[exportCalendarEvents] 已按开始时间升序排序");

        // 根据sortOrder排序（如果已经是升序，降序则反转）
        if ("desc".equalsIgnoreCase(sortOrder)) {
            Collections.reverse(exportEvents);
            log.info("[exportCalendarEvents] 按降序排列");
        } else {
            log.info("[exportCalendarEvents] 按升序排列");
        }

        log.info("[exportCalendarEvents] 最终导出{}条日程事件（原始{}条）", exportEvents.size(), allEvents.size());

        return exportEvents;
    }

    /**
     * 通过分页方式获取所有事件（用于导出）
     *
     * @param accessToken 访问令牌
     * @param calendarId 日历ID
     * @param startTime 开始时间戳（作为start_time，从该时间往后查询）
     * @param endTime 结束时间戳（可选，用于限制查询范围）
     * @param pageSize 每页数量
     * @return 所有事件列表
     */
    private List<Map<String, Object>> fetchAllEventsByPagination(String accessToken, String calendarId,
                                                                   Long startTime, Long endTime, int pageSize) throws Exception {
        List<Map<String, Object>> allEvents = new ArrayList<>();
        String pageToken = null;
        int pageCount = 0;

        do {
            pageCount++;
            log.info("[fetchAllEventsByPagination] 第{}次分页查询, pageToken={}, startTime={}", pageCount, pageToken, startTime);

            // 使用anchor_time进行分页查询（必须传anchor_time才能返回page_token）
            // 注意：anchor_time不可与start_time和end_time一起使用
            Map<String, Object> result = getCalendarEvents(
                accessToken, calendarId,
                null,  // 不传start_time（anchor_time与start_time互斥）
                endTime,
                startTime,  // 传anchor_time用于分页，从该时间往后查
                pageSize,
                pageToken,
                null
            );

            // 提取items（result本身就是data对象）
            List<Map<String, Object>> currentItems = new ArrayList<>();
            if (result != null && result.get("items") instanceof List) {
                currentItems = (List<Map<String, Object>>) result.get("items");
                log.info("[fetchAllEventsByPagination] 本次获取到{}条事件", currentItems.size());
            }

            // 更新pageToken和has_more
            String nextPageToken = result != null ? (String) result.get("page_token") : null;
            Boolean hasMore = result != null ? (Boolean) result.get("has_more") : false;

            // 如果没有page_token，即使has_more=true也无法继续分页
            if (nextPageToken == null || nextPageToken.isEmpty()) {
                log.info("[fetchAllEventsByPagination] page_token为空，停止分页");
                pageToken = null;
            } else {
                pageToken = nextPageToken;
            }

            log.info("[fetchAllEventsByPagination] has_more={}, page_token={}", hasMore, pageToken);

            // 将当前页数据添加到总列表
            allEvents.addAll(currentItems);

            // 如果有endTime，过滤掉超出endTime的事件
            if (endTime != null && !currentItems.isEmpty()) {
                long beforeFilterSize = allEvents.size();
                allEvents = allEvents.stream()
                    .filter(event -> {
                        Long eventTime = getStartTimeTimestamp(event);
                        return eventTime <= endTime;
                    })
                    .collect(Collectors.toList());
                long afterFilterSize = allEvents.size();
                if (beforeFilterSize != afterFilterSize) {
                    log.info("[fetchAllEventsByPagination] 过滤超出endTime的事件，从{}条减少到{}条",
                            beforeFilterSize, afterFilterSize);
                }
            }

            // 如果has_more为false或pageToken为空，停止分页
            if (pageToken == null || pageToken.isEmpty()) {
                break;
            }

            // 防止无限循环，最多查询100页
            if (pageCount >= 100) {
                log.warn("[fetchAllEventsByPagination] 已达到最大分页次数100，停止查询");
                break;
            }

        } while (true);

        log.info("[fetchAllEventsByPagination] 分页查询完成，共{}页，总计{}条事件", pageCount, allEvents.size());
        return allEvents;
    }

    /**
     * 获取事件的开始时间戳
     *
     * @param event 事件对象
     * @return 开始时间戳（秒）
     */
    private Long getStartTimeTimestamp(Map<String, Object> event) {
        if (event == null) {
            return 0L;
        }

        Map<String, Object> startTime = (Map<String, Object>) event.get("start_time");
        if (startTime != null && startTime.get("timestamp") != null) {
            try {
                return Long.parseLong(startTime.get("timestamp").toString());
            } catch (NumberFormatException e) {
                log.warn("[getStartTimeTimestamp] 解析时间戳失败: {}", startTime.get("timestamp"));
                return 0L;
            }
        }

        // 如果没有timestamp，尝试使用date字段
        if (startTime != null && startTime.get("date") != null) {
            try {
                String dateStr = startTime.get("date").toString();
                // 解析日期字符串为时间戳
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                java.util.Date date = sdf.parse(dateStr);
                return date.getTime() / 1000; // 转换为秒
            } catch (Exception e) {
                log.warn("[getStartTimeTimestamp] 解析日期失败: {}", startTime.get("date"));
                return 0L;
            }
        }

        return 0L;
    }

    @Override
    public Long createCalendar(CalendarSaveReqVO createReqVO) {
        // 插入
        CalendarDO calendar = BeanUtils.toBean(createReqVO, CalendarDO.class);
        calendarMapper.insert(calendar);

        // 返回
        return calendar.getId();
    }

    @Override
    public void updateCalendar(CalendarSaveReqVO updateReqVO) {
        // 校验存在
        validateCalendarExists(updateReqVO.getId());
        // 更新
        CalendarDO updateObj = BeanUtils.toBean(updateReqVO, CalendarDO.class);
        calendarMapper.updateById(updateObj);
    }

    @Override
    public void deleteCalendar(Long id) {
        // 校验存在
        validateCalendarExists(id);
        // 删除
        calendarMapper.deleteById(id);
    }

    @Override
    public void deleteCalendarListByIds(List<Long> ids) {
        // 删除
        calendarMapper.deleteByIds(ids);
    }


    private void validateCalendarExists(Long id) {
        if (calendarMapper.selectById(id) == null) {
            throw exception(CALENDAR_NOT_EXISTS);
        }
    }

    @Override
    public CalendarDO getCalendar(Long id) {
        return calendarMapper.selectById(id);
    }

    @Override
    public PageResult<CalendarDO> getCalendarPage(CalendarPageReqVO pageReqVO) {
        return calendarMapper.selectPage(pageReqVO);
    }

    @Override
    public void syncCalendarEvents(String username, String calendarId) throws Exception {
        log.info("[syncCalendarEvents] 开始同步日历数据, username={}, calendarId={}", username, calendarId);

        // 获取访问令牌
        Map<String, Object> tokenResult = getOrRefreshAccessToken();
        String accessToken = (String) tokenResult.get("access_token");

        // 查询数据库中该用户+日历是否已有数据
        LambdaQueryWrapperX<CalendarDO> checkWrapper = new LambdaQueryWrapperX<>();
        checkWrapper.eq(CalendarDO::getUsername, username);
        checkWrapper.eq(CalendarDO::getCalendarId, calendarId);
        checkWrapper.last("LIMIT 1");
        CalendarDO existingEvent = calendarMapper.selectOne(checkWrapper);

        Long anchorTime;
        LocalDateTime anchorDateTime;
        if (existingEvent != null) {
            // 已有数据：从当前时间往前推1个月作为锚点
            LocalDateTime now = LocalDateTime.now();
            anchorDateTime = now.minusMonths(1);
            anchorTime = anchorDateTime.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
            log.info("[syncCalendarEvents] 检测到已有数据，从当前时间往前推1个月，锚点时间={}", anchorDateTime);
        } else {
            // 无数据：从2010年1月1日开始全量同步
            anchorTime = 1262304000L; // 2010-01-01 00:00:00 UTC
            anchorDateTime = LocalDateTime.of(2010, 1, 1, 0, 0, 0);
            log.info("[syncCalendarEvents] 无历史数据，进行全量同步，锚点时间=2010-01-01");
        }

        // 用于收集飞书返回的所有eventId
        Set<String> feishuEventIds = new HashSet<>();

        // 递归查询所有数据
        int maxPages = 100;
        int currentPage = 0;
        String pageToken = null;
        int totalSynced = 0;
        int insertCount = 0;
        int updateCount = 0;

        while (currentPage < maxPages) {
            currentPage++;
            log.info("[syncCalendarEvents] 查询第{}页", currentPage);

            // 调用飞书API查询日程
            Map<String, Object> result = getCalendarEvents(
                accessToken, calendarId, null, null, anchorTime, 200, pageToken, null
            );

            // getCalendarEvents返回的就是data对象，直接使用
            if (result == null) {
                log.warn("[syncCalendarEvents] 返回数据为空，结束同步");
                break;
            }

            List<Map<String, Object>> items = null;

            if (result.get("items") instanceof List) {
                items = (List<Map<String, Object>>) result.get("items");
            }

            if (items == null || items.isEmpty()) {
                log.info("[syncCalendarEvents] 本页无数据，结束同步");
                break;
            }

            log.info("[syncCalendarEvents] 本页获取到{}条数据", items.size());

            // 保存或更新数据到数据库，并收集eventId
            for (Map<String, Object> event : items) {
                try {
                    CalendarDO calendarEvent = convertToCalendarDO(event, username, calendarId);
                    String eventId = calendarEvent.getEventId();

                    // 收集飞书返回的eventId
                    if (eventId != null && !eventId.isEmpty()) {
                        feishuEventIds.add(eventId);
                    }

                    // 根据eventId判断是否存在
                    LambdaQueryWrapperX<CalendarDO> existWrapper = new LambdaQueryWrapperX<>();
                    existWrapper.eq(CalendarDO::getUsername, username);
                    existWrapper.eq(CalendarDO::getCalendarId, calendarId);
                    existWrapper.eq(CalendarDO::getEventId, eventId);
                    CalendarDO dbEvent = calendarMapper.selectOne(existWrapper);

                    if (dbEvent != null) {
                        // 存在则更新
                        calendarEvent.setId(dbEvent.getId());
                        calendarMapper.updateById(calendarEvent);
                        updateCount++;
                    } else {
                        // 不存在则插入
                        calendarMapper.insert(calendarEvent);
                        insertCount++;
                    }

                    totalSynced++;
                } catch (Exception e) {
                    log.error("[syncCalendarEvents] 保存日程失败, eventId={}", event.get("event_id"), e);
                }
            }

            // 获取下一页的page_token和has_more
            pageToken = (String) result.get("page_token");
            Boolean hasMore = (Boolean) result.get("has_more");

            log.info("[syncCalendarEvents] 本页处理完成, page_token={}, has_more={}", pageToken, hasMore);

            // 如果has_more明确为false，结束同步
            if (hasMore != null && !hasMore) {
                log.info("[syncCalendarEvents] has_more=false，已是最后一页，结束同步");
                break;
            }

            // 如果没有page_token，尝试再查一次确认是否真的没有数据
            if (pageToken == null || pageToken.isEmpty()) {
                log.warn("[syncCalendarEvents] page_token为空，但has_more不为false，再查一次确认");
                // 继续循环，用空pageToken再查一次
            }
        }

        // 处理删除：找出数据库中已存在但飞书中已删除的日程
        log.info("[syncCalendarEvents] 开始检查需要删除的日程，飞书返回eventId数量={}", feishuEventIds.size());

        // 查询数据库中该用户+日历+锚点时间范围内的所有数据
        LambdaQueryWrapperX<CalendarDO> dbWrapper = new LambdaQueryWrapperX<>();
        dbWrapper.eq(CalendarDO::getUsername, username);
        dbWrapper.eq(CalendarDO::getCalendarId, calendarId);
        dbWrapper.ge(CalendarDO::getStartTime, anchorDateTime);
        List<CalendarDO> dbEvents = calendarMapper.selectList(dbWrapper);

        log.info("[syncCalendarEvents] 数据库中锚点时间范围内共有{}条数据", dbEvents.size());

        int deleteCount = 0;
        for (CalendarDO dbEvent : dbEvents) {
            String dbEventId = dbEvent.getEventId();
            // 如果数据库中的eventId不在飞书返回的列表中，说明已被删除
            if (dbEventId != null && !dbEventId.isEmpty() && !feishuEventIds.contains(dbEventId)) {
                log.info("[syncCalendarEvents] 删除已不存在的日程: eventId={}, summary={}",
                        dbEventId, dbEvent.getSummary());
                calendarMapper.deleteById(dbEvent.getId());
                deleteCount++;
            }
        }

        log.info("[syncCalendarEvents] 同步完成，共同步{}条数据（新增{}条，更新{}条，删除{}条）",
                totalSynced, insertCount, updateCount, deleteCount);
    }

    /**
     * 将飞书API返回的事件数据转换为CalendarDO对象
     */
    private CalendarDO convertToCalendarDO(Map<String, Object> event, String username, String calendarId) {
        CalendarDO calendarDO = new CalendarDO();
        calendarDO.setUsername(username);
        calendarDO.setCalendarId(calendarId);

        // 基本字段
        calendarDO.setEventId((String) event.get("event_id"));
        calendarDO.setOrganizerCalendarId((String) event.get("organizer_calendar_id"));
        calendarDO.setSummary((String) event.get("summary"));
        calendarDO.setDescription((String) event.get("description"));

        // 开始时间
        Map<String, Object> startTime = (Map<String, Object>) event.get("start_time");
        if (startTime != null && startTime.get("timestamp") != null) {
            long timestamp = Long.parseLong(startTime.get("timestamp").toString()) * 1000;
            calendarDO.setStartTime(LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
            ));
        }

        // 结束时间
        Map<String, Object> endTime = (Map<String, Object>) event.get("end_time");
        if (endTime != null && endTime.get("timestamp") != null) {
            long timestamp = Long.parseLong(endTime.get("timestamp").toString()) * 1000;
            calendarDO.setEndTime(LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
            ));
        }

        // 视频会议信息
        Map<String, Object> vchat = (Map<String, Object>) event.get("vchat");
        if (vchat != null) {
            calendarDO.setVcType((String) vchat.get("vc_type"));
            calendarDO.setIconType((String) vchat.get("icon_type"));
            calendarDO.setVcDescription((String) vchat.get("description"));
            calendarDO.setMeetingUrl((String) vchat.get("meeting_url"));
        }

        // 其他字段
        calendarDO.setVisibility((String) event.get("visibility"));
        calendarDO.setAttendeeAbility((String) event.get("attendee_ability"));
        calendarDO.setFreeBusyStatus((String) event.get("free_busy_status"));

        // 地点信息
        Map<String, Object> location = (Map<String, Object>) event.get("location");
        if (location != null) {
            calendarDO.setLocationName((String) location.get("name"));
            calendarDO.setLocationAddress((String) location.get("address"));
            Object lat = location.get("latitude");
            Object lon = location.get("longitude");
            if (lat instanceof Number) {
                calendarDO.setLocationLatitude(((Number) lat).doubleValue());
            }
            if (lon instanceof Number) {
                calendarDO.setLocationLongitude(((Number) lon).doubleValue());
            }
        }

        // 颜色
        Object color = event.get("color");
        if (color instanceof Number) {
            calendarDO.setColor(((Number) color).intValue());
        }

        // 提醒时间
        Object reminders = event.get("reminders");
        if (reminders != null) {
            try {
                calendarDO.setRemindersMinutes(objectMapper.writeValueAsString(reminders));
            } catch (Exception e) {
                log.warn("[convertToCalendarDO] 序列化reminders失败", e);
            }
        }

        calendarDO.setRecurrence((String) event.get("recurrence"));
        calendarDO.setStatus((String) event.get("status"));

        Object isException = event.get("is_exception");
        if (isException instanceof Boolean) {
            calendarDO.setIsException((Boolean) isException ? 1 : 0);
        }

        calendarDO.setRecurringEventId((String) event.get("recurring_event_id"));

        Object createTime = event.get("create_time");
        if (createTime != null) {
            calendarDO.setCalendarCreateTime(createTime.toString());
        }

        // 组织者信息
        Map<String, Object> organizer = (Map<String, Object>) event.get("event_organizer");
        if (organizer != null) {
            calendarDO.setUserId((String) organizer.get("user_id"));
            calendarDO.setDisplayName((String) organizer.get("display_name"));
        }

        calendarDO.setAppLink((String) event.get("app_link"));

        // 附件信息
        Object attachments = event.get("attachments");
        if (attachments != null) {
            try {
                calendarDO.setFileInfo(objectMapper.writeValueAsString(attachments));
            } catch (Exception e) {
                log.warn("[convertToCalendarDO] 序列化attachments失败", e);
            }
        }

        return calendarDO;
    }

    @Override
    public PageResult<CalendarDO> getCalendarEventsFromDb(CalendarPageReqVO pageReqVO) {
        log.info("[getCalendarEventsFromDb] 从数据库查询日程分页数据");
        return calendarMapper.selectPage(pageReqVO);
    }

    @Override
    public List<CalendarDO> exportCalendarEvents(CalendarPageReqVO reqVO) {
        log.info("[exportCalendarEvents] 导出日程数据, username={}, calendarId={}, summary={}, status={}",
                reqVO.getUsername(), reqVO.getCalendarId(), reqVO.getSummary(), reqVO.getStatus());
        log.info("[exportCalendarEvents] startTime={}, endTime={}, eventIds={}",
                reqVO.getStartTime(), reqVO.getEndTime(), reqVO.getEventIds());

        List<CalendarDO> exportList;

        if (reqVO.getEventIds() != null && !reqVO.getEventIds().isEmpty()) {
            // 有勾选：根据eventIds导出
            log.info("[exportCalendarEvents] 根据勾选的eventIds导出，共{}条", reqVO.getEventIds().size());
            LambdaQueryWrapperX<CalendarDO> queryWrapper = new LambdaQueryWrapperX<>();
            queryWrapper.eq(CalendarDO::getUsername, reqVO.getUsername()); // 必须按当前用户过滤
            queryWrapper.in(CalendarDO::getEventId, reqVO.getEventIds());
            queryWrapper.orderByAsc(CalendarDO::getStartTime);
            exportList = calendarMapper.selectList(queryWrapper);
        } else {
            // 无勾选：根据查询条件导出
            log.info("[exportCalendarEvents] 根据查询条件导出");
            // 不分页，查询所有符合条件的数据
            reqVO.setPageNo(1);
            reqVO.setPageSize(Integer.MAX_VALUE);
            PageResult<CalendarDO> pageResult = calendarMapper.selectPage(reqVO);
            exportList = pageResult.getList();
        }

        log.info("[exportCalendarEvents] 查询到{}条数据", exportList.size());
        return exportList;
    }
}
