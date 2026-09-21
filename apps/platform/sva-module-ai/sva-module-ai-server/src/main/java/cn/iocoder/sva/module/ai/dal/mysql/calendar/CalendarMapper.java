package cn.iocoder.sva.module.ai.dal.mysql.calendar;

import java.util.*;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.sva.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.sva.module.ai.dal.dataobject.calendar.CalendarDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.sva.module.ai.controller.admin.calendar.vo.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日程导出 Mapper
 *
 * @author like
 */
@Mapper
public interface CalendarMapper extends BaseMapperX<CalendarDO> {

    default PageResult<CalendarDO> selectPage(CalendarPageReqVO reqVO) {
        LambdaQueryWrapperX<CalendarDO> wrapper = new LambdaQueryWrapperX<CalendarDO>()
                .likeIfPresent(CalendarDO::getUsername, reqVO.getUsername())
                .eqIfPresent(CalendarDO::getCalendarId, reqVO.getCalendarId())
                .likeIfPresent(CalendarDO::getSummary, reqVO.getSummary())
                .eqIfPresent(CalendarDO::getStatus, reqVO.getStatus())
                .orderByDesc(CalendarDO::getStartTime);

        // 手动解析startTime和endTime字符串为LocalDateTime
        if (reqVO.getStartTime() != null && !reqVO.getStartTime().isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime start = LocalDateTime.parse(reqVO.getStartTime(), formatter);
                wrapper.ge(CalendarDO::getStartTime, start);
            } catch (Exception e) {
                // 解析失败，忽略该条件
            }
        }

        if (reqVO.getEndTime() != null && !reqVO.getEndTime().isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime end = LocalDateTime.parse(reqVO.getEndTime(), formatter);
                wrapper.le(CalendarDO::getStartTime, end);
            } catch (Exception e) {
                // 解析失败，忽略该条件
            }
        }

        return selectPage(reqVO, wrapper);
    }

}