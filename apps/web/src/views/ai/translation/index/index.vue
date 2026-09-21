<template>
  <div class="translator-container">
    <!-- 顶部：翻译配置栏 -->
    <el-card class="config-card" shadow="hover">
      <div class="config-row">
        <!-- 文件上传 -->
        <div class="config-item file-upload-wrapper">
          <el-upload
            ref="uploadRef"
            action="#"
            :before-upload="beforeUpload"
            :auto-upload="false"
            accept=".docx,.xlsx,.xlsm,.pdf"
            :file-list="fileList"
            @change="handleFileChange"
            :show-file-list="false"
          >
            <el-button type="primary" plain>
              <Icon icon="ep:upload" class="mr-5px" />
              选择文件
            </el-button>
          </el-upload>
          <div v-if="selectedFile" class="file-name">{{ selectedFile.name }}</div>
          <div class="file-hint">仅支持docx、xlsx、xlsm和pdf，文件大小100MB以内</div>
        </div>

        <!-- 目标语言 -->
        <div class="config-item">
          <span class="item-label">目标语言</span>
          <el-select v-model="targetLang" placeholder="请选择" style="width: 100px;">
            <el-option label="English" value="English" />
            <el-option label="中文" value="Chinese" />
          </el-select>
        </div>

        <!-- 选择模型 -->
        <div class="config-item">
          <span class="item-label">选择模型</span>
          <el-select v-model="selectedModelId" placeholder="选择模型" style="width: 140px;">
            <el-option
              v-for="item in modelList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </div>

        <!-- 选择角色 -->
        <div class="config-item">
          <span class="item-label">选择场景</span>
          <el-select v-model="selectedRoleId" placeholder="选择场景" style="width: 140px;">
            <el-option
              v-for="item in roleList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </div>

        <!-- 选择术语库多选框 -->
        <div class="config-item">
          <span class="item-label">选择术语</span>
          <el-select v-model="selectedGlossaryIdsForTranslate" placeholder="选择术语" clearable multiple collapse-tags style="width: 200px;">
            <el-option
              v-for="item in glossaryList"
              :key="item.id"
              :label="item.glossaryName"
              :value="item.id"
            />
          </el-select>
        </div>

        <!-- 开始翻译按钮 (靠右) -->
        <div class="config-item start-btn-wrapper">
          <el-button
            type="success"
            size="large"
            @click="startTranslate"
            :loading="isTranslating"
            plain
          >
            <Icon icon="ep:promotion" class="mr-5px" />
            开始翻译
          </el-button>
        </div>
      </div>

      <!-- 功能复选框 (2×2 网格) -->
      <div class="checkboxes-wrapper">
        <div class="checkboxes-grid">
          <el-checkbox v-model="useGlossary">
            启用术语
            <el-tooltip content="启用术语库优先替换" placement="top">
              <el-icon><InfoFilled /></el-icon>
            </el-tooltip>
          </el-checkbox>
          <el-checkbox v-model="strictFormat">
            严格格式
            <el-tooltip content="针对word文档, 严格保留原有格式" placement="top">
              <el-icon><InfoFilled /></el-icon>
            </el-tooltip>
          </el-checkbox>
          <el-checkbox v-if="isWordDocument" v-model="enableComparison">
            双语对照
            <el-tooltip content="翻译后，生成中英文对照的文档" placement="top">
              <el-icon><InfoFilled /></el-icon>
            </el-tooltip>
          </el-checkbox>
          <!-- 全文QC 复选框已隐藏（保留 enableQc 变量，默认 false，不影响其它引用逻辑） -->
          <el-checkbox v-model="enableQc" v-show="false">
            全文QC
            <el-tooltip content="启用大模型执行全文QC并生成报告" placement="top">
              <el-icon><InfoFilled /></el-icon>
            </el-tooltip>
          </el-checkbox>
          <el-checkbox v-if="enableComparison" v-model="translationFirst">
            译文前置
            <el-tooltip content="双语版文件中将译文放在原文前面" placement="top">
              <el-icon><InfoFilled /></el-icon>
            </el-tooltip>
          </el-checkbox>
          <el-checkbox v-model="disableCache" v-show="false" />
        </div>
      </div>
    </el-card>

    <!-- 底部：结果展示区 -->
    <el-card class="result-card" shadow="hover" style="margin-top: 16px;">
      <!-- PDF转换中状态 -->
      <div v-if="isPdfConverting" class="task-running">
        <div class="timer">
          <el-icon><Clock /></el-icon>
          运行时间: {{ elapsed.toFixed(1) }} 秒
          <span style="margin-left: 16px; color: #409eff; font-weight: bold;">
            PDF解析中...
            <el-icon class="is-loading" :size="16" style="margin-left: 8px;"><Loading /></el-icon>
          </span>
        </div>
        <el-progress
          :percentage="0"
          :text-inside="true"
          :stroke-width="20"
          color="#409eff"
          :style="{ borderRadius: '10px' }"
        />
      </div>

      <!-- 翻译中状态 -->
      <div v-else-if="taskId" class="task-running">
        <div class="timer">
          <el-icon><Clock /></el-icon>
          运行时间: {{ elapsed.toFixed(1) }} 秒
          <span v-if="progress >= 100 && taskStatus === 'running'" class="loading-icon" style="margin-left: 16px;">
            {{ taskEnableQc ? '正在进行全文QC' : '翻译完成，正在生成文件' }}
            <el-icon class="is-loading" :size="16"><Loading /></el-icon>
          </span>
          <span v-if="taskStatus === 'completed'" style="margin-left: 16px; color: #67c23a; font-weight: bold;">
            翻译完成
          </span>
        </div>
        <el-progress
          :percentage="progress"
          :text-inside="true"
          :stroke-width="20"
          color="#409eff"
          :style="{ borderRadius: '10px' }"
        >
          <template #default>
            {{ progressMsg }}
          </template>
        </el-progress>

        <div class="section-title">
          <el-icon><Monitor /></el-icon>
          实时中英文对照
        </div>
        <el-table :data="realtimePairs" border stripe max-height="300" style="margin-top: 10px;">
          <el-table-column prop="source" label="原文" />
          <el-table-column prop="target" label="译文" />
          <el-table-column prop="status" label="状态" />
        </el-table>
      </div>

      <!-- 翻译完成结果 -->
      <div v-if="taskStatus === 'completed'" class="task-completed">
        <!-- <el-row :gutter="20" class="stats-row">
          <el-col :span="12">
            <el-statistic
              title="估算费用"
              :precision="2"
              prefix="¥"
              :value="costCny"
              color="#409eff"
            />
          </el-col>
          <el-col :span="12" v-if="qcIssues !== null">
            <el-statistic
              title="QC发现数量"
              :value="qcIssues"
              color="#f56c6c"
            />
          </el-col>
        </el-row> -->
        <!-- <el-row :gutter="20" class="stats-row" v-if="qcIssues !== null">
          <el-col :span="12">
            <el-statistic
              title="QC发现数量"
              :value="qcIssues"
              color="#f56c6c"
            />
          </el-col>
        </el-row> -->

        <div class="download-buttons" style="margin-top: 20px;">
          <el-button
            v-if="minioUrls.file"
            type="primary"
            @click="downloadFile('file')"
            plain
          >
            <Icon icon="ep:download" class="mr-5px" />
            下载翻译结果
          </el-button>
          <el-button
            v-if="minioUrls.contrast"
            type="primary"
            @click="downloadFile('contrast')"
            plain
          >
            <Icon icon="ep:document" class="mr-5px" />
            下载双语对照版
          </el-button>
          <el-button
            v-if="minioUrls.excel"
            type="primary"
            @click="downloadFile('excel')"
            plain
          >
            <Icon icon="ep:document" class="mr-5px" />
            下载中英文对照表(Excel)
          </el-button>
          <el-button
            v-if="minioUrls.qc"
            type="primary"
            @click="downloadFile('qc')"
            plain
          >
            <Icon icon="ep:document" class="mr-5px" />
            下载QC报告(txt)
          </el-button>
        </div>

        <div class="section-title" style="margin-top: 30px;">
          <el-icon><Edit /></el-icon>
          翻译结果审校(按出现频次排序)
        </div>
        <div class="filter-row">
          <el-checkbox v-model="hideLongText">隐藏超过60字符的原文</el-checkbox>
        </div>
        <el-table :data="filteredEditTable" border stripe max-height="400" style="margin-top: 10px;">
          <el-table-column prop="source" label="原文" />
          <el-table-column prop="target" label="译文">
            <template #default="{ row }">
              <el-input v-model="row.target" size="small" />
            </template>
          </el-table-column>
          <el-table-column prop="freq" label="频次" width="80" />
          <el-table-column prop="addToGlossary" label="加入术语库" width="120">
            <template #default="{ row }">
              <el-checkbox v-model="row.addToGlossary" />
            </template>
          </el-table-column>
        </el-table>
        <el-button
          type="primary"
          @click="openGlossarySelectDialog"
          style="margin-top: 10px;"
          plain
        >
          <Icon icon="ep:check" class="mr-5px" />
          保存到术语库
        </el-button>
      </div>

      <!-- 错误状态 -->
      <div v-if="taskStatus === 'failed'">
        <el-alert :title="errorMsg" type="error" show-icon />
      </div>

      <!-- 初始空状态 -->
      <div v-if="!taskId" class="empty-state">
        <el-empty description="请上传文件并开始翻译" :image-size="120" />
      </div>
    </el-card>

    <!-- 术语库选择弹窗 -->
    <el-dialog
      v-model="glossarySelectDialogVisible"
      title="选择保存到的术语库"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-table
        :data="availableGlossaryList"
        border
        stripe
        max-height="400"
        @selection-change="handleGlossarySelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="glossaryName" label="术语库名称" />
        <el-table-column prop="languageDirection" label="语言方向" width="120" />
        <el-table-column prop="itemCount" label="术语数量" width="100" />
        <el-table-column prop="username" label="创建人" width="120" />
      </el-table>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="glossarySelectDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmSaveToGlossary">确定保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { TranApi } from '@/api/ai/translation/translation/tranApi'
import { ChatRoleApi } from '@/api/ai/model/chatRole'
import { useUserStore } from '@/store/modules/user'
import { CACHE_KEY, useCache } from '@/hooks/web/useCache'
import { AiModelTypeEnum } from '@/views/ai/utils/constants'
import {
  InfoFilled, Clock, Monitor, Edit, Loading
} from '@element-plus/icons-vue'

// ==================== 临时直接请求配置(已注释) ====================
// Spring Boot 后端地址
// const API_BASE = 'http://localhost:48088/admin-api/ai/tran'

// 临时 fetch 封装(已注释)
// const api = {
//   async get(url) {
//     const res = await fetch(url, {
//       method: 'GET',
//       headers: { 'Content-Type': 'application/json' }
//     })
//     if (!res.ok) throw new Error(`HTTP ${res.status}`)
//     return { data: await res.json() }
//   },
//
//   async post(url, data) {
//     const res = await fetch(url, {
//       method: 'POST',
//       headers: { 'Content-Type': 'application/json' },
//       body: JSON.stringify(data)
//     })
//     if (!res.ok) throw new Error(`HTTP ${res.status}`)
//     return { data: await res.json() }
//   },
//
//   async upload(url, formData) {
//     const res = await fetch(url, {
//       method: 'POST',
//       body: formData
//     })
//     if (!res.ok) throw new Error(`HTTP ${res.status}`)
//     return { data: await res.json() }
//   }
// }
// ==========================================================

// 表单数据
const uploadRef = ref(null)
const fileList = ref([])
const selectedFile = ref(null)
const targetLang = ref('English')
const useGlossary = ref(true)
const strictFormat = ref(true)
const enableComparison = ref(true)
const enableQc = ref(false)
const translationFirst = ref(false)
const disableCache = ref(true)

const glossaryItems = ref([])
const glossaryQuery = ref('')
const glossaryList = ref([])
const selectedGlossaryIdsForTranslate = ref([])  // 翻译时选择的术语库ID数组（多选）
const modelList = ref([])
const selectedModelId = ref(null)
const roleList = ref([])
const selectedRoleId = ref(null)

// 术语库选择弹窗相关
const glossarySelectDialogVisible = ref(false)
const selectedGlossaryIds = ref([])
const availableGlossaryList = ref([])

const taskId = ref(null)
const isTranslating = ref(false)
const taskStatus = ref(null)
const taskEnableQc = ref(false)
const progress = ref(0)
const progressMsg = ref('')
const elapsed = ref(0)
const realtimePairs = ref([])
const pairs = ref([])
const downloads = ref({})
const minioUrls = ref({})
const errorMsg = ref('')
const costCny = ref(0)
const qcIssues = ref(null)
const isPdfConverting = ref(false) // PDF转换中状态

const hideLongText = ref(true)
const editTableData = ref([])

const filteredGlossary = computed(() => {
  if (!glossaryQuery.value) return glossaryItems.value
  const q = glossaryQuery.value.toLowerCase()
  return glossaryItems.value.filter(item =>
    item.source.toLowerCase().includes(q) ||
    item.target.toLowerCase().includes(q)
  )
})

const isWordDocument = computed(() => {
  if (!selectedFile.value) return true
  const ext = selectedFile.value.name.split('.').pop()?.toLowerCase()
  return ext === 'docx' || ext === 'pdf'
})

const filteredEditTable = computed(() => {
  let data = [...editTableData.value]
  if (hideLongText.value) {
    data = data.filter(item => item.source.length <= 60)
  }
  return data
})

const handleFileChange = (file, fileListArr) => {
  if (!file || !file.raw) return

  // const maxSize = 100 * 1024  // 100KB（测试用）
  const maxSize = 100 * 1024 * 1024  // 100MB
  const allowedTypes = ['.docx', '.xlsx', '.xlsm', '.pdf']
  const fileName = file.name.toLowerCase()
  const isAllowed = allowedTypes.some(type => fileName.endsWith(type))

  // 先检查文件格式
  if (!isAllowed) {
    ElMessage.error('仅支持docx、xlsx、xlsm和pdf格式的文件')
    // 清空文件列表
    selectedFile.value = null
    fileList.value = []
    return
  }

  // 再检查文件大小
  if (file.size > maxSize) {
    ElMessage.error(`文件大小不能超过100MB，当前文件大小：${(file.size / 1024 / 1024).toFixed(2)}MB`)
    // 清空文件列表
    selectedFile.value = null
    fileList.value = []
    return
  }

  // 验证通过，去除文件名中的空格
  const originalFile = file.raw
  const originalName = originalFile.name
  const nameWithoutSpaces = originalName.replace(/\s+/g, '')

  // 如果文件名包含空格，创建新的File对象
  if (originalName !== nameWithoutSpaces) {
    const newFile = new File([originalFile], nameWithoutSpaces, {
      type: originalFile.type,
      lastModified: originalFile.lastModified
    })
    selectedFile.value = newFile
    // 更新fileList中的文件名显示
    fileList.value = [{ ...file, name: nameWithoutSpaces }]
  } else {
    // 文件名没有空格，直接使用原文件
    selectedFile.value = originalFile
    fileList.value = [file]
  }
}

const beforeUpload = (file) => {
  // 由于 auto-upload=false，before-upload 可能不会触发
  // 验证逻辑已移至 handleFileChange
  return false  // 始终阻止自动上传
}

// 使用 TranApi 加载术语库（暂时注释）
// const loadGlossary = async () => {
//   if (!selectedGlossaryId.value) {
//     glossaryItems.value = []
//     return
//   }
//   try {
//     const res = await TranApi.getGlossary(selectedGlossaryId.value)
//     glossaryItems.value = res.items || res.data?.items || []
//   } catch (e) {
//     console.error('加载术语库失败:', e)
//   }
// }

// watch(selectedGlossaryId, () => {
//   loadGlossary()
// })

// 加载可见术语库列表
const loadGlossaryList = async () => {
  try {
    const langMap = { 'English': 'en', 'Chinese': 'cn' }
    const targetLanguage = langMap[targetLang.value] || 'en'
    const res = await TranApi.getVisibleGlossaryList(targetLanguage)
    glossaryList.value = res.data || res || []
    // 注释掉自动选中逻辑，让用户手动选择术语库
    // const { wsCache } = useCache()
    // const userInfo = wsCache.get(CACHE_KEY.USER)
    // const currentUsername = userInfo?.user?.username
    // if (currentUsername && glossaryList.value.length > 0) {
    //   const matchedGlossary = glossaryList.value.find(item => item.username === currentUsername)
    //   if (matchedGlossary) {
    //     selectedGlossaryId.value = matchedGlossary.id
    //   }
    // }
  } catch (e) {
    ElMessage.error('加载术语库列表失败: ' + e.message)
  }
}

const loadModelList = async () => {
  try {
    const res = await TranApi.getModelSimpleList(AiModelTypeEnum.CHAT)
    modelList.value = res.data || res || []
    if (modelList.value.length > 0) {
      selectedModelId.value = modelList.value[0].id
    }
  } catch (e) {
    console.error('加载模型列表失败:', e)
  }
}

const loadRoleList = async () => {
  try {
    const res = await ChatRoleApi.getChatRoleListByCategory('翻译助手')
    roleList.value = res.data || res || []
    if (roleList.value.length > 0) {
      const sortedList = [...roleList.value].sort((a, b) => (a.sort || 0) - (b.sort || 0))
      selectedRoleId.value = sortedList[0].id
    }
  } catch (e) {
    console.error('加载角色列表失败:', e)
  }
}

watch(targetLang, () => {
  loadGlossaryList()
})

// 取消双语对照时，重置译文前置选项
watch(enableComparison, (val) => {
  if (!val) {
    translationFirst.value = false
  }
})

// 非 Word/PDF 文档时，隐藏双语对照并重置相关选项
watch(isWordDocument, (val) => {
  if (!val) {
    enableComparison.value = false
    translationFirst.value = false
  }
})

const startTranslate = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先上传文件！')
    return
  }

  isTranslating.value = true
  taskId.value = null
  taskStatus.value = null
  taskEnableQc.value = false
  progress.value = 0
  progressMsg.value = ''
  elapsed.value = 0
  realtimePairs.value = []
  pairs.value = []
  downloads.value = {}
  errorMsg.value = ''
  costCny.value = 0
  qcIssues.value = null

  // 检查是否为PDF文件，如果是则显示转换状态
  const fileName = selectedFile.value.name.toLowerCase()
  const isPdf = fileName.endsWith('.pdf')

  if (isPdf) {
    isPdfConverting.value = true
  }

  try {
    const res = await TranApi.submitTranslate({
      file: selectedFile.value,
      targetLang: targetLang.value,
      useGlossaryReplace: useGlossary.value,
      strictFormat: strictFormat.value,
      enableComparison: enableComparison.value,
      enableQc: enableQc.value,
      translationFirst: translationFirst.value,
      glossaryIds: selectedGlossaryIdsForTranslate.value,  // 传递术语库ID数组
      modelId: selectedModelId.value,
      roleId: selectedRoleId.value,
      disableCache: disableCache.value ? 1 : 0
    })
    taskId.value = res.taskId || res.data?.taskId
    taskStatus.value = 'pending'

    // 注意：PDF转换状态不在这里关闭，而是在轮询获取到进度后关闭

    startPolling()
  } catch (e) {
    // 出错时也要关闭转换状态
    if (isPdf) {
      isPdfConverting.value = false
    }
    ElMessage.error('提交任务失败: ' + e.message)
    isTranslating.value = false
  }
}

let pollingTimer = null
let pollingStartTime = 0
const startPolling = () => {
  if (pollingTimer) clearTimeout(pollingTimer)
  pollingStartTime = Date.now()

  // 使用递归setTimeout实现渐进式轮询
  const pollWithProgressiveInterval = async () => {
    if (!taskId.value) {
      return
    }

    try {
      const task = await TranApi.getTask(taskId.value)
      const taskData = task.data || task

      progress.value = taskData.progress
      progressMsg.value = taskData.progressMsg || taskData.progress_msg || ''
      elapsed.value = taskData.elapsed
      realtimePairs.value = taskData.realtime_pairs || taskData.realtimePairs || []
      taskStatus.value = taskData.status
      taskEnableQc.value = taskData.enable_qc || taskData.enableQc || false

      // 如果获取到了进度（progress > 0），说明PDF转换完成，关闭转换状态
      if (isPdfConverting.value && progress.value > 0) {
        isPdfConverting.value = false
      }

      if (taskData.status === 'completed') {
        isTranslating.value = false
        pairs.value = taskData.pairs
        downloads.value = taskData.downloads
        // 保存 MinIO URL
        minioUrls.value = taskData.minioUrls || {}
        costCny.value = taskData.cost_cny || taskData.costCny || 0
        qcIssues.value = taskData.qc_issues || taskData.qcIssues

        processEditTable()
        return // 结束轮询
      } else if (taskData.status === 'failed') {
        isTranslating.value = false
        errorMsg.value = taskData.error
        ElMessage.error('翻译失败: ' + taskData.error)
        return // 结束轮询
      }

      // 计算已过去的时间（毫秒）
      const elapsedTime = Date.now() - pollingStartTime

      // 渐进式轮询间隔：30秒内2秒一次，之后1秒一次
      let nextInterval
      if (elapsedTime < 30000) {
        // 前30秒：每2秒查询一次
        nextInterval = 2000
      } else {
        // 30秒后：每1秒查询一次
        nextInterval = 1000
      }

      // 设置下一次轮询
      pollingTimer = setTimeout(pollWithProgressiveInterval, nextInterval)
    } catch (e) {
      console.error('轮询任务状态失败', e)
      // 出错后也继续轮询，但使用较长的间隔
      pollingTimer = setTimeout(pollWithProgressiveInterval, 5000)
    }
  }

  // 开始第一次轮询
  pollWithProgressiveInterval()
}

const processEditTable = () => {
  if (!pairs.value || pairs.value.length === 0) return
  const df = pairs.value

  const freqMap = {}
  df.forEach(item => {
    const src = item.source
    freqMap[src] = (freqMap[src] || 0) + 1
  })

  const uniqueItems = []
  const seen = new Set()
  df.forEach(item => {
    if (!seen.has(item.source)) {
      seen.add(item.source)
      uniqueItems.push({
        source: item.source,
        target: item.target,
        status: item.status,
        freq: freqMap[item.source],
        addToGlossary: false
      })
    }
  })

  uniqueItems.sort((a, b) => b.freq - a.freq)
  editTableData.value = uniqueItems
}

const downloadFile = async (type) => {
  if (!taskId.value) return
  try {
    const blob = await TranApi.downloadFile(taskId.value, type)

    // 根据文件类型和原始文件名生成下载文件名
    let filename
    if (type === 'file' && selectedFile.value) {
      const originalName = selectedFile.value.name
      const dotIndex = originalName.lastIndexOf('.')
      const originalExt = dotIndex > 0 ? originalName.substring(dotIndex).toLowerCase() : ''
      const targetExt = originalExt === '.pdf' ? '.docx' : originalExt
      filename = dotIndex > 0
        ? originalName.substring(0, dotIndex) + '_译文' + targetExt
        : originalName + '_译文'
    } else if (type === 'contrast' && selectedFile.value) {
      const originalName = selectedFile.value.name
      const dotIndex = originalName.lastIndexOf('.')
      const originalExt = dotIndex > 0 ? originalName.substring(dotIndex).toLowerCase() : ''
      const targetExt = originalExt === '.pdf' ? '.docx' : originalExt
      const targetName = dotIndex > 0 ? originalName.substring(0, dotIndex) : originalName
      filename = targetName + '_双语对照' + targetExt
    } else if (type === 'excel' && selectedFile.value) {
      const originalName = selectedFile.value.name
      const dotIndex = originalName.lastIndexOf('.')
      filename = originalName.substring(0, dotIndex) + '_双语对照表.xlsx'
    } else if (type === 'qc' && selectedFile.value) {
      const originalName = selectedFile.value.name
      const dotIndex = originalName.lastIndexOf('.')
      filename = originalName.substring(0, dotIndex) + '_QC报告.txt'
    } else {
      filename = `download_${type}_${taskId.value}`
    }

    // 创建临时链接触发下载
    const blobUrl = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = blobUrl
    a.download = filename
    a.click()
    URL.revokeObjectURL(blobUrl)
  } catch (error) {
    console.error('下载失败:', error)
    ElMessage.error('下载文件失败: ' + (error.message || '未知错误'))
  }
}

// 打开术语库选择弹窗
const openGlossarySelectDialog = async () => {
  // 检查是否有勾选的术语
  const hasSelectedTerms = editTableData.value.some(row => row.addToGlossary)
  if (!hasSelectedTerms) {
    ElMessage.info('请先勾选需要保存的术语')
    return
  }

  try {
    // 加载当前目标语言下的所有可编辑术语库（只包含用户有权限修改的）
    const langMap = { 'English': 'en', 'Chinese': 'cn' }
    const targetLanguage = langMap[targetLang.value] || 'en'
    const res = await TranApi.getEditableGlossaryList(targetLanguage)
    availableGlossaryList.value = res.data || res || []

    if (availableGlossaryList.value.length === 0) {
      ElMessage.warning('当前没有可编辑的术语库')
      return
    }

    // 重置选中状态
    selectedGlossaryIds.value = []
    glossarySelectDialogVisible.value = true
  } catch (e) {
    ElMessage.error('加载术语库列表失败: ' + e.message)
  }
}

// 处理术语库表格选择变化
const handleGlossarySelectionChange = (selection) => {
  selectedGlossaryIds.value = selection.map(item => item.id)
}

// 确认保存到术语库
const confirmSaveToGlossary = async () => {
  // 检查是否勾选了术语库
  if (selectedGlossaryIds.value.length === 0) {
    ElMessage.warning('请勾选保存到的术语库')
    return
  }

  // 收集要保存的术语
  const newTerms = {}
  const savedKeys = []
  editTableData.value.forEach(row => {
    if (row.addToGlossary) {
      newTerms[row.source] = row.target
      savedKeys.push(row.source)
    }
  })

  if (Object.keys(newTerms).length === 0) {
    ElMessage.info('没有勾选需要加入的术语')
    return
  }

  try {
    const langMap = { 'English': 'en', 'Chinese': 'cn' }
    const targetLanguage = langMap[targetLang.value] || 'en'

    // 遍历所有选中的术语库，分别保存
    let totalSaved = 0
    for (const glossaryId of selectedGlossaryIds.value) {
      const res = await TranApi.saveGlossary(newTerms, targetLanguage, glossaryId)
      const resData = res.data || res
      if (resData.success) {
        totalSaved += resData.count || 0
      }
    }

    ElMessage.success(`已新增 ${totalSaved} 条术语到 ${selectedGlossaryIds.value.length} 个术语库`)

    // 关闭弹窗
    glossarySelectDialogVisible.value = false

    // 从编辑表格中移除已保存的术语
    editTableData.value = editTableData.value.filter(row => !savedKeys.includes(row.source))
  } catch (e) {
    ElMessage.error('保存失败: ' + e.message)
  }
}

const saveTerms = async () => {
  // 检查是否选择了术语库
  if (!selectedGlossaryId.value) {
    ElMessage.warning('请先选择术语库')
    return
  }

  const newTerms = {}
  const savedKeys = []
  editTableData.value.forEach(row => {
    if (row.addToGlossary) {
      newTerms[row.source] = row.target
      savedKeys.push(row.source)
    }
  })

  if (Object.keys(newTerms).length === 0) {
    ElMessage.info('没有勾选需要加入的术语')
    return
  }

  try {
    const langMap = { 'English': 'en', 'Chinese': 'cn' }
    const targetLanguage = langMap[targetLang.value] || 'en'
    const res = await TranApi.saveGlossary(newTerms, targetLanguage, selectedGlossaryId.value)
    const resData = res.data || res
    if (resData.success) {
      ElMessage.success(`已新增 ${resData.count} 条术语到术语库`)
      editTableData.value = editTableData.value.filter(row => !savedKeys.includes(row.source))
    } else {
      ElMessage.warning(resData.message)
    }
  } catch (e) {
    ElMessage.error('保存失败: ' + e.message)
  }
}

onMounted(() => {
  loadGlossaryList()
  loadModelList()
  loadRoleList()
})

onUnmounted(() => {
  if (pollingTimer) clearTimeout(pollingTimer)
})
</script>

<style scoped>
.translator-container {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.config-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.config-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-upload-wrapper {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.file-hint {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 4px;
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

.item-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

/* 复选框 2×2 网格布局 */
.checkboxes-grid {
  display: flex;
  gap: 20px;
}

.checkboxes-wrapper {
  margin-top: 16px;
}

/* 开始翻译按钮自动靠右 */
.start-btn-wrapper {
  margin-left: auto;
}

.file-name {
  font-size: 12px;
  color: #909399;
  max-width: 110px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 计时器样式 */
.timer {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  color: #409eff;
  margin-bottom: 16px;
}

/* 加载图标样式 */
.loading-icon {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #409eff;
}

/* 分节标题 */
.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-top: 20px;
  margin-bottom: 10px;
}

/* 统计行 */
.stats-row {
  margin-bottom: 20px;
}

/* 过滤行 */
.filter-row {
  display: flex;
  gap: 20px;
  margin-bottom: 10px;
}

/* 空状态 */
.empty-state {
  padding: 40px 0;
}
</style>
