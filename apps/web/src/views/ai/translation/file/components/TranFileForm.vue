<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="文件名" prop="fileName">
        <el-input v-model="formData.fileName" placeholder="请输入文件名" />
      </el-form-item>
      <el-form-item label="文件路径" prop="fileUrl">
        <el-input v-model="formData.fileUrl" placeholder="请输入文件路径" />
      </el-form-item>
      <el-form-item label="文件状态：0-未翻译 1-翻译完成 2-异常文件" prop="fileStatus">
        <el-radio-group v-model="formData.fileStatus">
          <el-radio value="1">请选择字典生成</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="文件类型" prop="fileType">
        <el-select v-model="formData.fileType" placeholder="请选择文件类型">
          <el-option label="请选择字典生成" value="" />
        </el-select>
      </el-form-item>
      <el-form-item label="文件说明" prop="context">
        <el-input v-model="formData.context" placeholder="请输入文件说明" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { TranFileApi, TranFile } from '@/api/ai/translation/file'

/** AI翻译文件信息 表单 */
defineOptions({ name: 'TranFileForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref({
  id: undefined,
  fileName: undefined,
  fileUrl: undefined,
  fileStatus: undefined,
  fileType: undefined,
  context: undefined
})
const formRules = reactive({
  fileName: [{ required: true, message: '文件名不能为空', trigger: 'blur' }],
  fileUrl: [{ required: true, message: '文件路径不能为空', trigger: 'blur' }],
  fileStatus: [{ required: true, message: '文件状态：0-未翻译 1-翻译完成 2-异常文件不能为空', trigger: 'blur' }]
})
const formRef = ref() // 表单 Ref

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  // 修改时，设置数据
  if (id) {
    formLoading.value = true
    try {
      formData.value = await TranFileApi.getTranFile(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 提交表单 */
const emit = defineEmits(['success']) // 定义 success 事件，用于操作成功后的回调
const submitForm = async () => {
  // 校验表单
  await formRef.value.validate()
  // 提交请求
  formLoading.value = true
  try {
    const data = formData.value as unknown as TranFile
    if (formType.value === 'create') {
      await TranFileApi.createTranFile(data)
      message.success(t('common.createSuccess'))
    } else {
      await TranFileApi.updateTranFile(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    // 发送操作成功的事件
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.value = {
    id: undefined,
    fileName: undefined,
    fileUrl: undefined,
    fileStatus: undefined,
    fileType: undefined,
    context: undefined
  }
  formRef.value?.resetFields()
}
</script>