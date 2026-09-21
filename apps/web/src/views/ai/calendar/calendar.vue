<template>
  <ContentWrap>
    <!-- 搜索表单 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="68px"
    >
      <el-form-item label="日历" prop="calendarId">
        <el-select
          v-model="queryParams.calendarId"
          placeholder="请选择日历"
          @change="handleCalendarChange"
          style="width: 180px"
        >
          <el-option
            v-for="calendar in calendarList"
            :key="calendar.calendar_id"
            :label="calendar.summary"
            :value="calendar.calendar_id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="日程标题" prop="summary">
        <el-input
          v-model="queryParams.summary"
          placeholder="请输入日程标题"
          clearable
          style="width: 180px"
        />
      </el-form-item>
      <el-form-item label="时间范围">
        <el-date-picker
          v-model="timeRangeValue"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 360px"
          clearable
          @change="handleTimeRangeChange"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="请选择状态"
          clearable
          style="width: 180px"
        >
          <el-option label="未回应" value="tentative" />
          <el-option label="已确认" value="confirmed" />
          <el-option label="已取消" value="cancelled" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          plain
          @click="handleSync"
          :loading="syncing"
        >
          <Icon icon="ep:refresh" class="mr-5px" /> 同步数据
        </el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出Excel
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 日程事件列表 -->
  <ContentWrap>
    <el-table :data="eventList" border v-loading="loading" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="序号" width="60" align="center">
        <template #default="{ $index }">
          {{ (queryParams.pageNo - 1) * queryParams.pageSize + $index + 1 }}
        </template>
      </el-table-column>
      <el-table-column prop="summary" label="日程标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="开始时间" width="180" align="center">
        <template #default="{ row }">
          {{ formatTime(row.startTime) }}
        </template>
      </el-table-column>
      <el-table-column label="结束时间" width="180" align="center">
        <template #default="{ row }">
          {{ formatTime(row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="vcType" label="会议类型" width="150" show-overflow-tooltip>
        <template #default="{ row }">
          {{ formatVcType(row.vcType) }}
        </template>
      </el-table-column>
      <el-table-column prop="meetingUrl" label="会议链接" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <el-link v-if="row.meetingUrl" :href="row.meetingUrl" target="_blank" type="primary">
            {{ row.meetingUrl }}
          </el-link>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="locationName" label="地点名称" width="150" show-overflow-tooltip />
      <el-table-column prop="locationAddress" label="地点地址" min-width="200" show-overflow-tooltip />
    </el-table>

    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 空状态 -->
    <el-empty v-if="!loading && eventList.length === 0" description="暂无日程数据" />
  </ContentWrap>
</template>

<script setup lang="ts">
import { CalendarApi } from '@/api/ai/calendar'
import request from '@/config/axios'
import { ElMessage } from 'element-plus'
import { Icon } from '@/components/Icon'
import download from '@/utils/download'

/** 日程管理 */
defineOptions({ name: 'Calendar' })

const calendarList = ref<any[]>([])
const eventList = ref<any[]>([])
const selectedCalendarId = ref<string>('')
const loading = ref(false)
const exportLoading = ref(false)
const queryFormRef = ref()
const syncing = ref(false) // 同步加载状态
const timeRangeValue = ref<[string, string] | []>([]) // 时间范围选择器的值

// 选中的事件列表
const selectedEvents = ref<any[]>([])

// 总数
const total = ref(0)

// 查询参数（简单分页，类似字典管理）
const queryParams = reactive({
  username: undefined as string | undefined,
  calendarId: undefined as string | undefined,
  summary: undefined as string | undefined,
  status: undefined as string | undefined,
  startTime: undefined as string | undefined, // 开始时间字符串
  endTime: undefined as string | undefined,   // 结束时间字符串
  pageNo: 1,
  pageSize: 10
})

/** 格式化时间 */
const formatTime = (timeValue: any) => {
  if (!timeValue) return '-'

  // 如果是时间戳(数字),转换为日期字符串
  if (typeof timeValue === 'number') {
    const date = new Date(timeValue)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  // 如果是字符串,直接处理
  if (typeof timeValue === 'string') {
    return timeValue.replace('T', ' ').substring(0, 16)
  }

  return '-'
}

/** 时间范围变化处理 */
const handleTimeRangeChange = (value: [string, string] | []) => {
  if (value && value.length === 2) {
    queryParams.startTime = value[0]
    queryParams.endTime = value[1]
  } else {
    queryParams.startTime = undefined
    queryParams.endTime = undefined
  }
}

/** 格式化日程状态 */
const formatStatus = (status: string) => {
  const statusMap: Record<string, string> = {
    'tentative': '未回应',
    'confirmed': '已确认',
    'cancelled': '已取消'
  }
  return statusMap[status] || status || '-'
}

/** 格式化会议类型 */
const formatVcType = (vcType: string) => {
  const vcTypeMap: Record<string, string> = {
    'vc': '飞书视频会议',
    'third_party': '第三方链接视频会议',
    'no_meeting': '无视频会议',
    'lark_live': '飞书直播',
    'unknown': '未知类型'
  }
  return vcTypeMap[vcType] || vcType || '-'
}

/** 获取状态标签类型 */
const getStatusType = (status: string) => {
  const typeMap: Record<string, any> = {
    'tentative': 'warning',
    'confirmed': 'success',
    'cancelled': 'danger'
  }
  return typeMap[status] || 'info'
}

/** 检查Token并获取日历列表 */
const checkTokenAndLoadCalendar = async () => {
  // 检查URL中是否有code参数（授权回调）
  const urlParams = new URLSearchParams(window.location.search)
  const code = urlParams.get('code')

  if (code) {
    // 有code，说明是授权回调，使用code换取token
    try {
      await request.get({
        url: '/ai/calendar/callback',
        params: { code }
      })

      // 换取token成功后，加载日历列表
      await loadCalendarList()

      // 清除URL中的code参数，避免刷新时重复处理
      window.history.replaceState({}, '', window.location.pathname)
    } catch (error: any) {
      ElMessage.error('授权失败，请重新授权')
      // 授权失败，重新跳转授权页
      redirectToAuth()
    }
    return
  }

  // 没有code，每次进入都强制跳转授权页
  redirectToAuth()
}

/** 加载日历列表 */
const loadCalendarList = async () => {
  try {
    loading.value = true

    // 查询日历列表
    const calendarResult = await CalendarApi.getCalendarList({ pageSize: 50 })

    // 解析日历数据
    if (calendarResult.data && calendarResult.data.calendar_list) {
      calendarList.value = calendarResult.data.calendar_list
      // 默认选中第一个日历
      if (calendarList.value.length > 0) {
        queryParams.calendarId = calendarList.value[0].calendar_id
        selectedCalendarId.value = calendarList.value[0].calendar_id
        // 自动同步第一个日历的数据
        await handleSync()
      }
    }
  } catch (error: any) {
    ElMessage.error('加载日历失败')
  } finally {
    loading.value = false
  }
}

/** 跳转到飞书授权页 */
const redirectToAuth = async () => {
  try {
    const config = await CalendarApi.getFeishuCalendarConfig()

    // 拼接飞书授权地址
    const scope = 'offline_access calendar:calendar:readonly'
    const authUrl = `${config.getCode}?client_id=${config.clientId}&response_type=code&redirect_uri=${encodeURIComponent(config.callback)}&scope=${encodeURIComponent(scope)}`

    // 直接跳转到飞书授权页
    window.location.href = authUrl
  } catch (error) {
    // 忽略错误
  }
}

/** 同步日历数据到数据库 */
const handleSync = async () => {
  if (!queryParams.calendarId) {
    ElMessage.warning('请先选择日历')
    return
  }

  try {
    syncing.value = true
    loading.value = true // 同时设置loading,显示表格加载状态

    // 显示提示消息
    const loadingMsg = ElMessage({
      message: '正在从飞书拉取日程信息，请稍等...',
      type: 'info',
      duration: 0, // 不自动关闭
      showClose: false
    })

    await CalendarApi.syncCalendarEvents(queryParams.calendarId)

    // 同步完成后关闭提示
    loadingMsg.close()
    ElMessage.success('同步成功')

    // 同步完成后重新查询
    await getList()
  } catch (error: any) {
    // 检查是否是429限流错误
    if (error.response?.status === 429 || error.code === 429) {
      ElMessage.warning('达到飞书请求阈值，请稍后重试')
    } else {
      ElMessage.error('同步失败: ' + (error.message || '未知错误'))
    }
  } finally {
    syncing.value = false
    loading.value = false
  }
}

/** 查询列表 */
const getList = async () => {
  if (!queryParams.calendarId) {
    return
  }

  try {
    loading.value = true

    // 调用新的分页查询接口
    const result = await CalendarApi.getCalendarEventsPage(queryParams)

    eventList.value = result.list || []
    total.value = result.total || 0
  } catch (error: any) {
    ElMessage.error('查询失败')
  } finally {
    loading.value = false
  }
}

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  timeRangeValue.value = [] // 清空时间范围选择器
  queryParams.pageNo = 1
  getList()
}

/** 页码变化 */
const handlePageChange = (page: number) => {
  queryParams.pageNo = page
  getList()
}

/** 每页条数变化 */
const handlePageSizeChange = (size: number) => {
  queryParams.pageSize = size
  queryParams.pageNo = 1
  getList()
}

/** 导出Excel */
/** 导出Excel */
const handleExport = async () => {
  if (!queryParams.calendarId) {
    ElMessage.warning('请先选择日历')
    return
  }

  try {
    exportLoading.value = true

    // 构建导出参数
    const exportParams: any = {
      calendarId: queryParams.calendarId,
      summary: queryParams.summary,
      status: queryParams.status,
      startTime: queryParams.startTime,
      endTime: queryParams.endTime
    }

    // 如果有勾选，传递eventIds
    if (selectedEvents.value && selectedEvents.value.length > 0) {
      exportParams.eventIds = selectedEvents.value.map((item: any) => item.eventId)
      console.log('[handleExport] 导出勾选的数据，共', exportParams.eventIds.length, '条')
    } else {
      console.log('[handleExport] 导出查询条件的全部数据')
    }

    // 调用新的导出接口（基于数据库）
    const data = await CalendarApi.exportCalendarEventsNew(exportParams)

    // 使用download.excel下载文件
    download.excel(data, '日程事件.xlsx')

    ElMessage.success('导出成功')
  } catch (error) {
    console.error('[handleExport] 导出失败:', error)
    ElMessage.error('导出失败')
  } finally {
    exportLoading.value = false
  }
}

/** 日历选择变化 */
const handleCalendarChange = async (calendarId: string) => {
  // 重置分页状态
  queryParams.pageNo = 1

  // 同步新选择的日历数据
  await handleSync()
}

/** 表格多选变化 */
const handleSelectionChange = (selection: any[]) => {
  selectedEvents.value = selection
}


/** 初始化 **/
onMounted(async () => {
  await checkTokenAndLoadCalendar()
})
</script>
