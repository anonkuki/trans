<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="源术语" prop="sourceLanguage">
        <el-input v-model="formData.sourceLanguage" placeholder="请输入源术语" type="text" />
      </el-form-item>
      <el-form-item label="目标术语" prop="targetLanguage">
        <el-input v-model="formData.targetLanguage" placeholder="请输入目标术语" type="text" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { TranGlossaryItemApi, TranGlossaryItem } from '@/api/ai/translation/glossary/item'

/** 术语项管理 表单 */
defineOptions({ name: 'TranGlossaryItemForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const props = defineProps<{
  glossaryId: number
}>()

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref({
  id: undefined,
  sourceLanguage: undefined,
  targetLanguage: undefined,
  glossaryId: props.glossaryId
})
const formRules = reactive({
  sourceLanguage: [{ required: true, message: '源语言不能为空', trigger: 'blur' }],
  targetLanguage: [{ required: true, message: '目标语言不能为空', trigger: 'blur' }]
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
      formData.value = await TranGlossaryItemApi.getTranGlossaryItem(id)
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
    const data = formData.value as unknown as TranGlossaryItem
    if (formType.value === 'create') {
      await TranGlossaryItemApi.createTranGlossaryItem(data)
      message.success(t('common.createSuccess'))
    } else {
      await TranGlossaryItemApi.updateTranGlossaryItem(data)
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
    sourceLanguage: undefined,
    targetLanguage: undefined,
    glossaryId: props.glossaryId
  }
  formRef.value?.resetFields()
}
</script>