<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="800">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="100px"
    >
      <el-form-item label="外链名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入外链名称" />
      </el-form-item>
      <el-form-item label="外链地址" prop="url">
        <el-input v-model="formData.url" placeholder="请输入外链地址，如 https://www.example.com" />
      </el-form-item>
      <el-form-item label="图标" prop="icon">
        <IconSelect v-model="formData.icon" clearable />
      </el-form-item>
      <el-form-item label="图标图片" prop="iconUrl">
        <UploadImg v-model="formData.iconUrl" :file-size="1" width="60px" height="60px">
          <template #tip> 上传后优先展示图片图标 </template>
        </UploadImg>
      </el-form-item>
      <el-form-item label="描述" prop="description">
        <el-input v-model="formData.description" placeholder="请输入外链描述" type="textarea" />
      </el-form-item>
      <el-form-item label="分类" prop="category">
        <el-input v-model="formData.category" placeholder="请输入分类，用于首页卡片分组展示" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" :min="0" controls-position="right" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="formData.status" clearable placeholder="请选择状态">
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="打开方式" prop="openTarget">
        <el-radio-group v-model="formData.openTarget">
          <el-radio :value="1">新窗口</el-radio>
          <el-radio :value="0">当前窗口</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="添加到菜单" prop="addMenuFlag">
        <el-checkbox v-model="formData.addMenuFlag">同步添加到顶层菜单，在侧边栏展示</el-checkbox>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { CommonStatusEnum } from '@/utils/constants'
import * as ExternalLinkApi from '@/api/system/extlink'

defineOptions({ name: 'SystemExternalLinkForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref({
  id: undefined,
  name: '',
  url: '',
  icon: '',
  iconUrl: '',
  description: '',
  category: '',
  sort: 0,
  status: CommonStatusEnum.ENABLE,
  openTarget: 1,
  addMenuFlag: true // 默认同步添加到菜单。后端根据该标识创建/删除关联的顶层菜单。
})
const formRules = reactive({
  name: [{ required: true, message: '外链名称不能为空', trigger: 'blur' }],
  url: [
    { required: true, message: '外链地址不能为空', trigger: 'blur' },
    {
      pattern: /^https?:\/\/.*/,
      message: '外链地址必须以 http:// 或 https:// 开头',
      trigger: 'blur'
    }
  ],
  sort: [{ required: true, message: '排序不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }],
  openTarget: [{ required: true, message: '打开方式不能为空', trigger: 'change' }]
})
const formRef = ref() // 表单 Ref

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  // 修改时，设置数据。回显时根据是否已关联菜单，回填"添加到菜单"勾选状态。
  if (id) {
    formLoading.value = true
    try {
      const detail = await ExternalLinkApi.getExternalLink(id)
      formData.value = detail
      formData.value.addMenuFlag = !!detail.menuId
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
  if (!formRef) return
  const valid = await formRef.value.validate()
  if (!valid) return
  // 提交请求
  formLoading.value = true
  try {
    const data = formData.value as unknown as ExternalLinkApi.ExternalLinkVO
    if (formType.value === 'create') {
      await ExternalLinkApi.createExternalLink(data)
      message.success(t('common.createSuccess'))
    } else {
      await ExternalLinkApi.updateExternalLink(data)
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
    name: '',
    url: '',
    icon: '',
    iconUrl: '',
    description: '',
    category: '',
    sort: 0,
    status: CommonStatusEnum.ENABLE,
    openTarget: 1,
    addMenuFlag: true
  }
  formRef.value?.resetFields()
}
</script>
