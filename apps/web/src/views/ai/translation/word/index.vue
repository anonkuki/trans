<template>
  <div class="text-translator">
    <!-- 顶部：翻译配置栏 -->
    <el-card class="config-card" shadow="hover">
      <div class="config-row">
        <!-- 目标语言 -->
        <div class="config-item">
          <span class="item-label">目标语言</span>
          <el-select v-model="targetLang" placeholder="请选择" style="width: 120px">
            <el-option label="English" value="English" />
            <el-option label="中文" value="Chinese" />
          </el-select>
        </div>

        <!-- 选择模型 -->
        <div class="config-item">
          <span class="item-label">选择模型</span>
          <el-select v-model="selectedModelId" placeholder="选择模型" style="width: 160px">
            <el-option v-for="item in modelList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </div>

        <!-- 选择场景 -->
        <div class="config-item">
          <span class="item-label">选择场景</span>
          <el-select v-model="selectedRoleId" placeholder="选择场景" style="width: 160px">
            <el-option v-for="item in roleList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </div>

        <!-- 选择术语 -->
        <div class="config-item">
          <span class="item-label">选择术语</span>
          <el-select
            v-model="selectedGlossaryIds"
            placeholder="选择术语"
            clearable
            multiple
            collapse-tags
            collapse-tags-tooltip
            style="width: 220px"
          >
            <el-option
              v-for="item in glossaryList"
              :key="item.id"
              :label="item.glossaryName"
              :value="item.id"
            />
          </el-select>
        </div>

        <!-- 翻译按钮（靠右） -->
        <div class="config-item translate-btn-wrapper">
          <el-button type="success" size="large" plain :loading="translating" @click="handleTranslate">
            <Icon icon="ep:promotion" class="mr-5px" />
            翻译
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 中部：翻译输入 / 输出区 -->
    <el-card class="translate-card" shadow="hover">
      <div class="translate-panels">
        <!-- 左侧：原文输入 -->
        <div class="panel">
          <div class="panel-header">
            <span class="panel-title">原文</span>
            <el-button link type="primary" :disabled="!sourceText" @click="handleClearSource">
              <Icon icon="ep:delete" class="mr-3px" /> 清空
            </el-button>
          </div>
          <el-input
            v-model="sourceText"
            type="textarea"
            :rows="12"
            resize="none"
            maxlength="5000"
            show-word-limit
            placeholder="请输入要翻译的内容，按 Ctrl + Enter 快速翻译"
            class="panel-textarea"
            @keydown.ctrl.enter="handleTranslate"
          />
        </div>

        <!-- 右侧：译文输出 -->
        <div class="panel">
          <div class="panel-header">
            <span class="panel-title">译文</span>
            <el-button link type="primary" :disabled="!targetText" @click="handleCopy">
              <Icon icon="ep:document-copy" class="mr-3px" /> 复制
            </el-button>
          </div>
          <div v-loading="translating" class="panel-result">
            <span v-if="targetText" class="result-text">{{ targetText }}</span>
            <span v-else class="result-placeholder">翻译结果将显示在这里</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 底部：翻译历史 -->
    <el-card class="history-card" shadow="hover">
      <div class="history-toolbar">
        <span class="section-title">
          <Icon icon="ep:clock" class="mr-5px" /> 翻译历史（最近 1000 条）
        </span>
        <div class="history-search">
          <el-input
            v-model="queryParams.keyword"
            placeholder="搜索原文或译文"
            clearable
            class="!w-260px"
            @keyup.enter="handleQuery"
            @clear="handleQuery"
          >
            <template #prefix>
              <Icon icon="ep:search" />
            </template>
          </el-input>
          <el-button type="primary" plain @click="handleQuery">
            <Icon icon="ep:search" class="mr-5px" /> 搜索
          </el-button>
          <el-button plain @click="resetQuery">
            <Icon icon="ep:refresh" class="mr-5px" /> 重置
          </el-button>
          <el-button type="danger" plain :disabled="total === 0" @click="handleClearHistory">
            <Icon icon="ep:delete" class="mr-5px" /> 清空历史
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        :stripe="true"
        :show-overflow-tooltip="true"
        border
        style="margin-top: 12px"
      >
        <el-table-column label="序号" align="center" type="index" width="60" />
        <el-table-column label="原文" align="left" prop="sourceText" min-width="220" show-overflow-tooltip />
        <el-table-column label="译文" align="left" prop="targetText" min-width="220" show-overflow-tooltip />
        <el-table-column label="目标语言" align="center" prop="targetLang" width="100" />
        <el-table-column label="模型" align="center" prop="modelName" width="130" show-overflow-tooltip />
        <el-table-column label="场景" align="center" prop="roleName" width="120" show-overflow-tooltip />
        <el-table-column
          label="翻译时间"
          align="center"
          prop="createTime"
          :formatter="dateFormatter"
          width="180"
        />
        <el-table-column label="操作" align="center" width="130" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="handleFill(scope.row)">回填</el-button>
            <el-button link type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无翻译历史" :image-size="100" />
        </template>
      </el-table>

      <Pagination
        :total="total"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        @pagination="getHistoryList"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { dateFormatter } from '@/utils/formatTime'
import { TranApi } from '@/api/ai/translation/translation/tranApi'
import { TranTextApi } from '@/api/ai/translation/text'
import { ChatRoleApi } from '@/api/ai/model/chatRole'
import { AiModelTypeEnum } from '@/views/ai/utils/constants'

defineOptions({ name: 'TranText' })

// ==================== 翻译配置 ====================
const targetLang = ref('Chinese')
const modelList = ref([])
const selectedModelId = ref(null)
const roleList = ref([])
const selectedRoleId = ref(null)
const glossaryList = ref([])
const selectedGlossaryIds = ref([])

// ==================== 翻译输入/输出 ====================
const sourceText = ref('')
const targetText = ref('')
const translating = ref(false)

// ==================== 翻译历史 ====================
const loading = ref(false)
const list = ref([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: undefined
})

/** 加载模型列表 */
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

/** 加载场景（翻译助手角色）列表 */
const loadRoleList = async () => {
  try {
    const res = await ChatRoleApi.getChatRoleListByCategory('翻译助手')
    roleList.value = res.data || res || []
    if (roleList.value.length > 0) {
      const sortedList = [...roleList.value].sort((a, b) => (a.sort || 0) - (b.sort || 0))
      selectedRoleId.value = sortedList[0].id
    }
  } catch (e) {
    console.error('加载场景列表失败:', e)
  }
}

/** 加载可见术语库列表 */
const loadGlossaryList = async () => {
  try {
    const langMap = { English: 'en', Chinese: 'cn' }
    const targetLanguage = langMap[targetLang.value] || 'en'
    const res = await TranApi.getVisibleGlossaryList(targetLanguage)
    glossaryList.value = res.data || res || []
    // 切换语言后，剔除不在新列表中的已选术语库
    const validIds = glossaryList.value.map((item) => item.id)
    selectedGlossaryIds.value = selectedGlossaryIds.value.filter((id) => validIds.includes(id))
  } catch (e) {
    ElMessage.error('加载术语库列表失败: ' + (e.message || ''))
  }
}

watch(targetLang, () => {
  loadGlossaryList()
})

/** 执行翻译 */
const handleTranslate = async () => {
  const text = sourceText.value.trim()
  if (!text) {
    ElMessage.warning('请输入要翻译的内容')
    return
  }
  translating.value = true
  targetText.value = ''
  try {
    const res = await TranTextApi.translate({
      sourceText: text,
      targetLang: targetLang.value,
      modelId: selectedModelId.value,
      roleId: selectedRoleId.value,
      glossaryIds: selectedGlossaryIds.value
    })
    const data = res?.data ?? res
    targetText.value = data?.targetText || ''
    if (!targetText.value) {
      ElMessage.warning('未获取到译文，请重试')
    }
    // 翻译成功后刷新历史列表（回到第一页，展示最新记录）
    queryParams.pageNo = 1
    await getHistoryList()
  } catch (e) {
    ElMessage.error('翻译失败: ' + (e.message || '未知错误'))
  } finally {
    translating.value = false
  }
}

/** 清空原文 */
const handleClearSource = () => {
  sourceText.value = ''
  targetText.value = ''
}

/** 复制译文 */
const handleCopy = async () => {
  if (!targetText.value) return
  try {
    await navigator.clipboard.writeText(targetText.value)
    ElMessage.success('已复制到剪贴板')
  } catch (e) {
    // 兜底方案
    const textarea = document.createElement('textarea')
    textarea.value = targetText.value
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('已复制到剪贴板')
  }
}

/** 查询翻译历史列表 */
const getHistoryList = async () => {
  loading.value = true
  try {
    const data = await TranTextApi.getHistoryPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    console.error('加载翻译历史失败:', e)
  } finally {
    loading.value = false
  }
}

/** 搜索 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getHistoryList()
}

/** 重置搜索 */
const resetQuery = () => {
  queryParams.keyword = undefined
  handleQuery()
}

/** 回填历史到输入框 */
const handleFill = (row) => {
  sourceText.value = row.sourceText || ''
  targetText.value = row.targetText || ''
  if (row.targetLang) {
    targetLang.value = row.targetLang
  }
}

/** 删除单条历史 */
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该条翻译历史吗？', '提示', { type: 'warning' })
    await TranTextApi.deleteHistory(id)
    ElMessage.success('删除成功')
    await getHistoryList()
  } catch (e) {
    // 用户取消
  }
}

/** 清空历史 */
const handleClearHistory = async () => {
  try {
    await ElMessageBox.confirm('确认清空当前用户的全部翻译历史吗？', '提示', { type: 'warning' })
    await TranTextApi.clearHistory()
    ElMessage.success('已清空翻译历史')
    queryParams.pageNo = 1
    await getHistoryList()
  } catch (e) {
    // 用户取消
  }
}

onMounted(() => {
  loadModelList()
  loadRoleList()
  loadGlossaryList()
  getHistoryList()
})
</script>

<style scoped>
.text-translator {
  padding: 16px;
}

.config-card,
.translate-card,
.history-card {
  margin-bottom: 16px;
}

.config-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
}

.config-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
  white-space: nowrap;
}

.translate-btn-wrapper {
  margin-left: auto;
}

/* 翻译输入/输出双栏 */
.translate-panels {
  display: flex;
  gap: 16px;
}

.panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.panel-textarea :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  font-size: 15px;
  line-height: 1.6;
  padding: 12px;
}

.panel-result {
  min-height: 280px;
  padding: 12px;
  font-size: 15px;
  line-height: 1.6;
  overflow-y: auto;
}

.result-text {
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
}

.result-placeholder {
  color: #c0c4cc;
}

/* 历史工具栏 */
.history-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.section-title {
  display: flex;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.history-search {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
