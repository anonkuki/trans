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
      <el-form-item label="文件名" prop="fileName">
        <el-input
          v-model="queryParams.fileName"
          placeholder="请输入文件名"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="创建时间" prop="createTime">
        <el-date-picker
          v-model="queryParams.createTime"
          value-format="YYYY-MM-DD HH:mm:ss"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
          class="!w-220px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['ai:tran-file:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['ai:tran-file:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
        <el-button
            type="danger"
            plain
            :disabled="isEmpty(checkedIds)"
            @click="handleDeleteBatch"
            v-hasPermi="['ai:tran-file:delete']"
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
    <el-table-column label="序号" align="center" type="index" width="60" />
      <el-table-column label="文件名" align="center" prop="fileName">
        <template #default="scope">
          <el-link type="primary" :underline="false" @click="handleDownload(scope.row)">
            {{ scope.row.fileName }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="文件状态" align="center" prop="fileStatus">
        <template #default="scope">
          <el-tag v-if="scope.row.fileStatus === 0" type="info">未翻译</el-tag>
          <el-tag v-else-if="scope.row.fileStatus === 1" type="success">翻译完成</el-tag>
          <el-tag v-else-if="scope.row.fileStatus === 2" type="danger">异常文件</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="文件类型" align="center" prop="fileType" />
      <el-table-column label="用户" align="center" prop="username" />
      <!-- <el-table-column label="文件说明" align="center" prop="context" /> -->
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="180px"
      />
      <el-table-column label="操作" align="center" min-width="200px">
        <template #default="scope">
          <el-button
            v-if="scope.row.sourceFileUrl"
            link
            type="primary"
            @click="downloadByUrl(scope.row.sourceFileUrl, scope.row.fileName, '_原文档')"
          >
            原文件
          </el-button>
          <el-button
            v-if="scope.row.compareFileUrl"
            link
            type="success"
            @click="downloadByUrl(scope.row.compareFileUrl, scope.row.fileName, '_对照表')"
          >
            对照表
          </el-button>
          <el-button
            v-if="scope.row.qcFileUrl"
            link
            type="warning"
            @click="downloadByUrl(scope.row.qcFileUrl, scope.row.fileName, '_QC报告')"
          >
            QC报告
          </el-button>
          <el-button
            v-if="scope.row.contrastFileUrl"
            link
            type="success"
            @click="downloadByUrl(scope.row.contrastFileUrl, scope.row.fileName, '_双语版')"
          >
            双语版
          </el-button>
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
            v-hasPermi="['ai:tran-file:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['ai:tran-file:delete']"
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
  <TranFileForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { isEmpty } from '@/utils/is'
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import { TranFileApi, TranFile } from '@/api/ai/translation/file'
import TranFileForm from '../components/TranFileForm.vue'

/** AI翻译文件信息 列表 */
defineOptions({ name: 'TranFile' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true) // 列表的加载中
const list = ref<TranFile[]>([]) // 列表的数据
const total = ref(0) // 列表的总页数
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  fileName: undefined,
  createTime: []
})
const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await TranFileApi.getTranFilePage(queryParams)
    list.value = data.list
    total.value = data.total
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
  queryFormRef.value.resetFields()
  handleQuery()
}

/** 添加/修改操作 */
const formRef = ref()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

/** 文件下载 */
const handleDownload = (row: TranFile) => {
  if (row.fileUrl) {
    downloadByUrl(row.fileUrl, row.fileName || '未命名文件', '_译文')
  }
}

/** 通过URL下载文件 */
const downloadByUrl = (url: string, fileName: string, suffix?: string) => {
  if (!url) return
  // 清理文件名：去除_strict和时间戳
  let cleanName = fileName
    .replace(/_strict/g, '')
    .replace(/_\d{10,}/g, '')
  // 从URL中提取文件扩展名
  const urlExtMatch = url.match(/\.([^./]+)$/)
  const ext = urlExtMatch ? '.' + urlExtMatch[1] : ''
  const nameWithoutExt = cleanName.replace(/\.[^.]+$/, '')
  // 添加后缀到名称后面、扩展名前面
  const finalName = suffix ? nameWithoutExt + suffix + ext : cleanName
  // 统一使用blob方式强制下载，确保文件名生效
  fetch(url)
    .then(res => res.blob())
    .then(blob => {
      const blobUrl = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = blobUrl
      a.download = finalName
      a.click()
      URL.revokeObjectURL(blobUrl)
    })
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    // 删除的二次确认
    await message.delConfirm()
    // 发起删除
    await TranFileApi.deleteTranFile(id)
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch {}
}

/** 批量删除AI翻译文件信息 */
const handleDeleteBatch = async () => {
  try {
    // 删除的二次确认
    await message.delConfirm()
    await TranFileApi.deleteTranFileList(checkedIds.value);
    checkedIds.value = [];
    message.success(t('common.delSuccess'))
    await getList();
  } catch {}
}

const checkedIds = ref<number[]>([])
const handleRowCheckboxChange = (records: TranFile[]) => {
  checkedIds.value = records.map((item) => item.id!);
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    // 导出的二次确认
    await message.exportConfirm()
    // 发起导出
    exportLoading.value = true
    const data = await TranFileApi.exportTranFile(queryParams)
    download.excel(data, 'AI翻译文件信息.xls')
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