import request from '@/config/axios'

// 飞书日历 API
export const CalendarApi = {
  // 获取飞书日历配置
  getFeishuCalendarConfig: async () => {
    return await request.get({ url: `/ai/calendar/feishu-config` })
  },

  // 获取或刷新访问令牌
  getOrRefreshToken: async () => {
    return await request.get({ url: `/ai/calendar/token` })
  },

  // 查询日历列表
  getCalendarList: async (params?: {
    pageSize?: number
    pageToken?: string
    syncToken?: string
  }) => {
    return await request.get({
      url: `/ai/calendar/list`,
      params
    })
  },

  // 查询日程事件列表
  getCalendarEvents: async (params: {
    calendarId: string
    startTime?: number
    endTime?: number
    anchorTime?: number  // 分页锚点时间
    pageSize?: number
    pageToken?: string
    syncToken?: string
  }) => {
    return await request.get({
      url: `/ai/calendar/events`,
      params
    })
  },

  // 导出日程事件Excel（实时查询版本 - 暂停使用）
  exportCalendarEvents: async (params: {
    calendarId: string
    startTime?: number
    endTime?: number
    eventIds?: string[] // 选中的事件ID列表
    status?: string // 状态筛选
    sortOrder?: 'asc' | 'desc' // 排序状态：'asc'=升序, 'desc'=降序
    pageSize?: number // 分页大小
  }) => {
    return await request.download({
      url: `/ai/calendar/export-excel`,
      method: 'post',
      data: params
    })
  },

  // 同步日历数据到数据库
  syncCalendarEvents: async (calendarId: string) => {
    return await request.post({
      url: `/ai/calendar/sync`,
      params: { calendarId }
    })
  },

  // 从数据库查询日程分页数据（新版）
  getCalendarEventsPage: async (params: any) => {
    return await request.get({
      url: `/ai/calendar/events/page`,
      params
    })
  },

  // 导出日程事件Excel（新版-基于数据库）
  exportCalendarEventsNew: async (params: any) => {
    return await request.download({
      url: `/ai/calendar/export-excel-new`,
      method: 'post',
      data: params
    })
  }
}
