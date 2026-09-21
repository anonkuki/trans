package cn.iocoder.sva.module.ai.service.calendar;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.module.ai.controller.admin.calendar.vo.CalendarPageReqVO;
import cn.iocoder.sva.module.ai.controller.admin.calendar.vo.CalendarSaveReqVO;
import cn.iocoder.sva.module.ai.dal.dataobject.calendar.CalendarDO;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

/**
 * 飞书日历 Service 接口
 *
 * @author like
 */
public interface CalendarService {

    /**
     * 获取飞书日历配置信息
     *
     * @return 包含 clientId、clientSecret、getCode 的 Map
     */
    Map<String, String> getFeishuCalendarConfig();

    /**
     * 使用授权码换取访问令牌
     *
     * @param code 授权码
     * @return 包含 access_token 等信息的 Map
     * @throws Exception 请求异常
     */
    Map<String, Object> exchangeAccessToken(String code) throws Exception;

    /**
     * 获取或刷新访问令牌
     *
     * @return 包含 access_token 等信息的 Map
     * @throws Exception 请求异常
     */
    Map<String, Object> getOrRefreshAccessToken() throws Exception;

    /**
     * 查询日历列表
     *
     * @param accessToken 访问令牌
     * @param pageSize 每页数量
     * @param pageToken 分页token
     * @param syncToken 同步token
     * @return 日历列表数据
     * @throws Exception 请求异常
     */
    Map<String, Object> getCalendarList(String accessToken, Integer pageSize, String pageToken, String syncToken) throws Exception;

    /**
     * 查询日程事件列表
     *
     * @param accessToken 访问令牌
     * @param calendarId 日历ID
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @param pageSize 每页数量
     * @param pageToken 分页token
     * @param syncToken 同步token
     * @return 日程事件列表数据
     * @throws Exception 请求异常
     */
    Map<String, Object> getCalendarEvents(String accessToken, String calendarId, Long startTime, Long endTime,
                                          Long anchorTime, Integer pageSize, String pageToken, String syncToken) throws Exception;

    /**
     * 导出日程事件列表
     *
     * @param calendarId 日历ID
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @param eventIds 选中的事件ID列表（为空则导出全部）
     * @param status 状态筛选（为空则导出全部）
     * @param sortOrder 排序状态：asc=升序, desc=降序
     * @param pageSize 分页大小
     * @return 日程事件列表（用于导出）
     * @throws Exception 请求异常
     */
    List<Map<String, Object>> exportCalendarEvents(String calendarId, Long startTime, Long endTime,
                                                    List<String> eventIds, String status, String sortOrder, Integer pageSize) throws Exception;


    /**
     * 创建日程导出
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createCalendar(@Valid CalendarSaveReqVO createReqVO);

    /**
     * 更新日程导出
     *
     * @param updateReqVO 更新信息
     */
    void updateCalendar(@Valid CalendarSaveReqVO updateReqVO);

    /**
     * 删除日程导出
     *
     * @param id 编号
     */
    void deleteCalendar(Long id);

    /**
     * 批量删除日程导出
     *
     * @param ids 编号
     */
    void deleteCalendarListByIds(List<Long> ids);

    /**
     * 获得日程导出
     *
     * @param id 编号
     * @return 日程导出
     */
    CalendarDO getCalendar(Long id);

    /**
     * 获得日程导出分页
     *
     * @param pageReqVO 分页查询
     * @return 日程导出分页
     */
    PageResult<CalendarDO> getCalendarPage(CalendarPageReqVO pageReqVO);

    /**
     * 同步日历数据到数据库（全量或增量）
     *
     * @param username 用户名
     * @param calendarId 日历ID
     * @throws Exception 请求异常
     */
    void syncCalendarEvents(String username, String calendarId) throws Exception;

    /**
     * 从数据库查询日程分页数据（新版）
     *
     * @param pageReqVO 分页查询条件
     * @return 日程分页数据
     */
    PageResult<CalendarDO> getCalendarEventsFromDb(CalendarPageReqVO pageReqVO);

    /**
     * 根据条件或勾选导出日程数据
     *
     * @param reqVO 查询条件，包含eventIds（勾选）或其他查询条件
     * @return 日程列表
     */
    List<CalendarDO> exportCalendarEvents(CalendarPageReqVO reqVO);
}
