<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="68px"
    >
      <el-form-item label="源术语" prop="sourceLanguage">
        <el-input
          v-model="queryParams.sourceLanguage"
          placeholder="请输入源术语"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['ai:tran-glossary-item:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
        <el-button
          type="warning"
          plain
          @click="openImportForm"
        >
          <Icon icon="ep:upload" class="mr-5px" /> 批量上传
        </el-button>
        <el-button
          type="info"
          plain
          @click="handleDownloadTemplate"
          :loading="downloadLoading"
        >
          <Icon icon="ep:download" class="mr-5px" /> 下载模板
        </el-button>
        <el-button
            type="danger"
            plain
            :disabled="isEmpty(checkedIds)"
            @click="handleDeleteBatch"
        >
          <Icon icon="ep:delete" class="mr-5px" /> 批量删除
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table
        row-key="id"
        v-loading="loading"
        :data="list"
        :stripe="true"
        :show-overflow-tooltip="true"
        @selection-change="handleRowCheckboxChange"
    >
    <el-table-column type="selection" width="55" />
      <!-- <el-table-column label="主键id" align="center" prop="id" /> -->
      <el-table-column label="源术语" align="center" prop="sourceLanguage" />
      <el-table-column label="目标术语" align="center" prop="targetLanguage" />
      <!-- <el-table-column label="所属术语库id" align="center" prop="glossaryId" /> -->
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="180px"
      />
      <el-table-column label="操作" align="center" min-width="120px">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 表单弹窗：添加/修改 -->
  <TranGlossaryItemForm ref="formRef" @success="getList" :glossary-id="glossaryId" />
  <!-- 批量上传弹窗 -->
  <TranGlossaryItemImportForm ref="importFormRef" @success="getList" />
</template>

<script setup lang="ts">
import { isEmpty } from '@/utils/is'
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import { useRoute } from 'vue-router'
import { TranGlossaryItemApi, TranGlossaryItem } from '@/api/ai/translation/glossary/item'
import TranGlossaryItemForm from './components/TranGlossaryItemForm.vue'
import TranGlossaryItemImportForm from './components/TranGlossaryItemImportForm.vue'
import { processDownloadUrl } from '@/utils/downloadHelper'

/** 术语项管理 列表 */
defineOptions({ name: 'TranGlossaryItem' })

const route = useRoute()
const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化
const downloadLoading = ref(false) // 下载加载状态

// 获取术语库ID
const glossaryId = ref(Number(route.params.id))

const loading = ref(true) // 列表的加载中
const list = ref<TranGlossaryItem[]>([]) // 列表的数据
const total = ref(0) // 列表的总页数
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  sourceLanguage: undefined,
  glossaryId: glossaryId.value,
  createTime: []
})
const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await TranGlossaryItemApi.getTranGlossaryItemPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 下载模板 */
const handleDownloadTemplate = async () => {
  try {
    downloadLoading.value = true
    // 调用RPC接口获取模板文件信息
    const res = await TranGlossaryItemApi.getTemplateUrl('GlossaryTemplate')
    console.log('模板查询结果:', res)

    // request拦截器已经解包了CommonResult，res就是data字段的内容
    if (res && res.url && res.name) {
      // 使用原始文件名下载，去掉时间戳后缀
      const fileName = res.name

      // 通过 fetch 获取文件内容
      const processedUrl = processDownloadUrl(res.url)
      const response = await fetch(processedUrl)
      if (!response.ok) {
        throw new Error('文件下载失败')
      }
      const blob = await response.blob()

      // 创建下载链接
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = fileName // 指定文件名
      document.body.appendChild(link)
      link.click()

      // 清理
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)

      message.success('模板下载成功')
    } else {
      console.warn('未找到模板文件，返回数据:', res)
      message.error('未找到模板文件，请先上传模板')
    }
  } catch (error) {
    console.error('下载模板失败:', error)
    message.error('下载模板失败')
  } finally {
    downloadLoading.value = false
  }
}

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

/** 添加/修改操作 */
const formRef = ref()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

/** 批量上传操作 */
const importFormRef = ref()
const openImportForm = () => {
  importFormRef.value.open(glossaryId.value)
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    // 删除的二次确认
    await message.delConfirm()
    // 发起删除
    await TranGlossaryItemApi.deleteTranGlossaryItem(id)
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch {}
}

/** 批量删除术语项管理 */
const handleDeleteBatch = async () => {
  try {
    // 删除的二次确认
    await message.delConfirm()
    await TranGlossaryItemApi.deleteTranGlossaryItemList(checkedIds.value);
    checkedIds.value = [];
    message.success(t('common.delSuccess'))
    await getList();
  } catch {}
}

const checkedIds = ref<number[]>([])
const handleRowCheckboxChange = (records: TranGlossaryItem[]) => {
  checkedIds.value = records.map((item) => item.id!);
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    // 导出的二次确认
    await message.exportConfirm()
    // 发起导出
    exportLoading.value = true
    const data = await TranGlossaryItemApi.exportTranGlossaryItem(queryParams)
    download.excel(data, '术语项管理.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

/** 初始化 **/
onMounted(() => {
  getList()
})
</script>