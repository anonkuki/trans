package cn.iocoder.sva.module.ai.controller.admin.calendar;

import cn.iocoder.sva.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.sva.framework.common.pojo.CommonResult;
import cn.iocoder.sva.framework.common.pojo.PageParam;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;
import cn.iocoder.sva.framework.excel.core.util.ExcelUtils;
import cn.iocoder.sva.module.ai.controller.admin.calendar.vo.CalendarEventExportVO;
import cn.iocoder.sva.module.ai.controller.admin.calendar.vo.CalendarPageReqVO;
import cn.iocoder.sva.module.ai.controller.admin.calendar.vo.CalendarRespVO;
import cn.iocoder.sva.module.ai.dal.dataobject.calendar.CalendarDO;
import cn.iocoder.sva.module.ai.service.calendar.CalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.sva.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.sva.framework.common.pojo.CommonResult.error;
import static cn.iocoder.sva.framework.common.pojo.CommonResult.success;

/**
 * 飞书日历 Controller
 *
 * @author like
 */
@Slf4j
@RestController
@RequestMapping("/ai/calendar")
@Tag(name = "飞书日历接口", description = "提供飞书日历相关的接口能力")
public class CalendarController {

    @Resource
    private CalendarService calendarService;

    @GetMapping("/feishu-config")
    @Operation(summary = "获取飞书日历配置", description = "获取飞书日历的 clientId、clientSecret 和 getCode 配置信息")
    public CommonResult<Map<String, String>> getFeishuCalendarConfig() {
        Map<String, String> config = calendarService.getFeishuCalendarConfig();
        return success(config);
    }

    @GetMapping("/callback")
    @Operation(summary = "飞书日历授权回调", description = "接收飞书授权回调的code，换取access_token")
    public CommonResult<Map<String, Object>> feishuCallback(
            @Parameter(description = "授权码", required = true)
            @RequestParam("code") String code) {
        try {
            Map<String, Object> result = calendarService.exchangeAccessToken(code);
            return success(result);
        } catch (Exception e) {
            log.info("获取accessToken错误：{}",e);
            return error(500, "获取access_token失败: " + e.getMessage());
        }
    }

    @GetMapping("/token")
    @Operation(summary = "获取或刷新访问令牌", description = "优先从Redis获取，过期则自动刷新，无refresh_token则返回需要重新授权")
    public CommonResult<Map<String, Object>> getOrRefreshToken() {
        try {
            calendarService.getOrRefreshAccessToken();
            return success(Map.of("authorized", true));
        } catch (Exception e) {
            if ("NEED_REAUTH".equals(e.getMessage())) {
                // 需要重新授权
                Map<String, Object> result = new java.util.HashMap<>();
                result.put("needReauth", true);
                return success(result);
            }
            log.error("获取或刷新token错误：{}", e);
            return error(500, "获取token失败: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    @Operation(summary = "查询日历列表", description = "获取用户的日历列表")
    public CommonResult<Map<String, Object>> getCalendarList(
            @Parameter(description = "每页数量", example = "1000")
            @RequestParam(value = "pageSize", required = false, defaultValue = "1000") Integer pageSize,
            @Parameter(description = "分页token")
            @RequestParam(value = "pageToken", required = false) String pageToken,
            @Parameter(description = "同步token")
            @RequestParam(value = "syncToken", required = false) String syncToken) {
        try {
            // 先获取或刷新access_token
            Map<String, Object> tokenResult = calendarService.getOrRefreshAccessToken();
            String accessToken = (String) tokenResult.get("access_token");

            // 查询日历列表
            Map<String, Object> result = calendarService.getCalendarList(accessToken, pageSize, pageToken, syncToken);
            return success(result);
        } catch (Exception e) {
            if ("NEED_REAUTH".equals(e.getMessage())) {
                // 需要重新授权
                Map<String, Object> result = new java.util.HashMap<>();
                result.put("needReauth", true);
                return success(result);
            }
            log.error("查询日历列表错误：{}", e);
            return error(500, "查询日历列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/events")
    @Operation(summary = "查询日程事件列表", description = "获取指定日历的日程事件列表")
    public CommonResult<Map<String, Object>> getCalendarEvents(
            @Parameter(description = "日历ID", required = true)
            @RequestParam("calendarId") String calendarId,
            @Parameter(description = "开始时间戳")
            @RequestParam(value = "startTime", required = false) Long startTime,
            @Parameter(description = "结束时间戳")
            @RequestParam(value = "endTime", required = false) Long endTime,
            @Parameter(description = "分页锚点时间（秒级时间戳）")
            @RequestParam(value = "anchorTime", required = false) Long anchorTime,
            @Parameter(description = "每页数量", example = "500")
            @RequestParam(value = "pageSize", required = false, defaultValue = "500") Integer pageSize,
            @Parameter(description = "分页token")
            @RequestParam(value = "pageToken", required = false) String pageToken,
            @Parameter(description = "同步token")
            @RequestParam(value = "syncToken", required = false) String syncToken) {
        try {
            // 先获取或刷新access_token
            Map<String, Object> tokenResult = calendarService.getOrRefreshAccessToken();
            String accessToken = (String) tokenResult.get("access_token");

            // 查询日程事件列表
            Map<String, Object> result = calendarService.getCalendarEvents(
                    accessToken, calendarId, startTime, endTime, anchorTime, pageSize, pageToken, syncToken);

            // 对返回的日程事件按开始时间排序
            if (result != null && result.get("data") instanceof Map) {
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                if (data.get("items") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");

                    // 按开始时间升序排序
                    items.sort((a, b) -> {
                        Long timeA = getStartTimeTimestamp(a);
                        Long timeB = getStartTimeTimestamp(b);
                        return timeA.compareTo(timeB);
                    });

                    log.info("[getCalendarEvents] 已按开始时间排序，共{}条事件", items.size());
                }
            }

            return success(result);
        } catch (Exception e) {
            if ("NEED_REAUTH".equals(e.getMessage())) {
                // 需要重新授权
                Map<String, Object> result = new java.util.HashMap<>();
                result.put("needReauth", true);
                return success(result);
            }
            log.error("查询日程事件列表错误：{}", e);
            return error(500, "查询日程事件列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/export-excel-new")
    @Operation(summary = "导出日程事件 Excel（新版-基于数据库）", description = "根据勾选或查询条件从数据库导出日程事件")
//    @PreAuthorize("@ss.hasPermission('ai:calendar:query')")
    public void exportExcelNew(
            jakarta.servlet.http.HttpServletResponse response,
            @RequestBody CalendarPageReqVO reqVO) throws Exception {
        try {
            log.info("[exportExcelNew] 接收到的参数: username={}, calendarId={}, summary={}, status={}, startTime={}, endTime={}, eventIds={}",
                    reqVO.getUsername(), reqVO.getCalendarId(), reqVO.getSummary(), reqVO.getStatus(),
                    reqVO.getStartTime(), reqVO.getEndTime(), reqVO.getEventIds());

            // 获取当前登录用户名
            String username = cn.iocoder.sva.framework.security.core.util.SecurityFrameworkUtils.getLoginUserUsername();

            // 设置username为当前登录用户
            reqVO.setUsername(username);

            log.info("[exportExcelNew] 设置username后: username={}, startTime={}, endTime={}",
                    reqVO.getUsername(), reqVO.getStartTime(), reqVO.getEndTime());

            // 调用Service层方法查询数据
            List<CalendarDO> exportList = calendarService.exportCalendarEvents(reqVO);

            // 转换为VO对象
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            List<CalendarEventExportVO> excelList = new ArrayList<>();
            for (int i = 0; i < exportList.size(); i++) {
                CalendarDO calendarDO = exportList.get(i);
                CalendarEventExportVO vo = new CalendarEventExportVO();
                vo.setIndex(i + 1);
                vo.setSummary(calendarDO.getSummary());
                vo.setDescription(calendarDO.getDescription());

                // 格式化开始时间
                if (calendarDO.getStartTime() != null) {
                    vo.setStartTime(sdf.format(java.sql.Timestamp.valueOf(calendarDO.getStartTime())));
                }

                // 格式化结束时间
                if (calendarDO.getEndTime() != null) {
                    vo.setEndTime(sdf.format(java.sql.Timestamp.valueOf(calendarDO.getEndTime())));
                }

                // 会议类型（中文化）
                String vcType = calendarDO.getVcType();
                if ("vc".equals(vcType)) {
                    vo.setVcType("飞书视频会议");
                } else if ("third_party".equals(vcType)) {
                    vo.setVcType("第三方链接视频会议");
                } else if ("no_meeting".equals(vcType)) {
                    vo.setVcType("无视频会议");
                } else if ("lark_live".equals(vcType)) {
                    vo.setVcType("飞书直播");
                } else if ("unknown".equals(vcType)) {
                    vo.setVcType("未知类型");
                } else {
                    vo.setVcType(vcType);
                }

                // 会议链接
                vo.setMeetingUrl(calendarDO.getMeetingUrl());

                // 地点名称
                vo.setLocationName(calendarDO.getLocationName());

                // 地点地址
                vo.setLocationAddress(calendarDO.getLocationAddress());

                excelList.add(vo);
            }

            // 使用ExcelUtils导出
            ExcelUtils.write(
                    response,
                    "日程事件.xls",
                    "日程列表",
                    CalendarEventExportVO.class,
                    excelList
            );

            log.info("[exportExcelNew] 导出完成，共{}条数据", excelList.size());
        } catch (Exception e) {
            log.error("[exportExcelNew] 导出日程事件错误", e);
            throw e;
        }
    }

    @PostMapping("/sync")
    @Operation(summary = "同步日历数据到数据库", description = "将飞书日历数据同步到本地数据库，支持全量和增量同步")
    public CommonResult<Boolean> syncCalendarEvents(
            @Parameter(description = "日历ID", required = true)
            @RequestParam("calendarId") String calendarId) {
        // 获取当前登录用户名
        String username = cn.iocoder.sva.framework.security.core.util.SecurityFrameworkUtils.getLoginUserUsername();

        try {
            log.info("[syncCalendarEvents] 开始同步, username={}, calendarId={}", username, calendarId);

            // 执行同步
            calendarService.syncCalendarEvents(username, calendarId);

            return success(true);
        } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
            // 捕获429限流错误
            log.warn("[syncCalendarEvents] 达到飞书API请求阈值, username={}, calendarId={}", username, calendarId);
            return error(429, "达到飞书请求阈值，请稍后重试");
        } catch (Exception e) {
            log.error("[syncCalendarEvents] 同步失败", e);
            return error(500, "同步失败: " + e.getMessage());
        }
    }

    @GetMapping("/events/page")
    @Operation(summary = "从数据库查询日程分页数据（新版）", description = "简单的分页查询，类似字典管理页面")
//    @PreAuthorize("@ss.hasPermission('ai:calendar:query')")
    public CommonResult<PageResult<CalendarRespVO>> getCalendarEventsPage(@Valid CalendarPageReqVO pageReqVO) {
        // 自动设置当前登录用户，防止查出其他人的数据
        String username = cn.iocoder.sva.framework.security.core.util.SecurityFrameworkUtils.getLoginUserUsername();
        pageReqVO.setUsername(username);
        log.info("[getCalendarEventsPage] 查询日程分页数据, username={}, calendarId={}", username, pageReqVO.getCalendarId());

        PageResult<CalendarDO> pageResult = calendarService.getCalendarEventsFromDb(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CalendarRespVO.class));
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
                // date格式为 "2018-09-01"
                String dateStr = startTime.get("date").toString();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date date = sdf.parse(dateStr);
                return date.getTime() / 1000; // 转换为秒
            } catch (Exception e) {
                log.warn("[getStartTimeTimestamp] 解析日期失败: {}", startTime.get("date"));
                return 0L;
            }
        }

        return 0L;
    }

    @GetMapping("/get")
    @Operation(summary = "获得日程导出")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
//    @PreAuthorize("@ss.hasPermission('ai:calendar:query')")
    public CommonResult<CalendarRespVO> getCalendar(@RequestParam("id") Long id) {
        CalendarDO calendar = calendarService.getCalendar(id);
        return success(BeanUtils.toBean(calendar, CalendarRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得日程导出分页")
//    @PreAuthorize("@ss.hasPermission('ai:calendar:query')")
    public CommonResult<PageResult<CalendarRespVO>> getCalendarPage(@Valid CalendarPageReqVO pageReqVO) {
        PageResult<CalendarDO> pageResult = calendarService.getCalendarPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CalendarRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出日程导出 Excel")
//    @PreAuthorize("@ss.hasPermission('ai:calendar:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportCalendarExcel(@Valid CalendarPageReqVO pageReqVO,
                                    HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CalendarDO> list = calendarService.getCalendarPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "日程导出.xls", "数据", CalendarRespVO.class,
                BeanUtils.toBean(list, CalendarRespVO.class));
    }

}
