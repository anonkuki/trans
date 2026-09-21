<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="术语库名称" prop="glossaryName">
        <el-input v-model="formData.glossaryName" placeholder="请输入术语库名称" />
      </el-form-item>
      <el-form-item label="源语言">
        <el-select v-model="formData.sourceLanguage" placeholder="请选择">
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.AI_TRANSLATE_LANGUAGE)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="目标语言">
        <el-select v-model="formData.targetLanguage" placeholder="请选择">
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.AI_TRANSLATE_LANGUAGE)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="所属角色" prop="roleId" v-hasPermi="['ai:tran-glossary:role']">
        <el-select v-model="formData.roleId" clearable placeholder="请选择所属角色" :loading="roleListLoading">
          <el-option
            v-for="role in roleList"
            :key="role.id"
            :label="role.name"
            :value="role.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="可见角色" prop="roleShow" v-hasPermi="['ai:tran-glossary:role-show']">
        <el-select v-model="formData.roleShow" clearable placeholder="请选择可见角色" :loading="roleListLoading">
          <el-option
            v-for="role in roleList"
            :key="role.id"
            :label="role.name"
            :value="role.id"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { TranGlossaryApi, TranGlossary } from '@/api/ai/translation/glossary'
import { DICT_TYPE, getStrDictOptions, getDictOptions } from '@/utils/dict'
import * as RoleApi from '@/api/system/role'

/** 术语库管理 表单 */
defineOptions({ name: 'TranGlossaryForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref({
  id: "",
  glossaryName: "",
  sourceLanguage: "",
  targetLanguage: "",
  languageDirection: "",
  itemCount: 0,
  roleId: "",
  roleShow: "",
  userId: "",
  deptId: "",
  isEnabled: 1
})
const formRules = reactive({
  glossaryName: [{ required: true, message: '术语库名称不能为空', trigger: 'blur' }],
  sourceLanguage: [{ required: true, message: '源语言不能为空', trigger: 'change' }],
  targetLanguage: [{ required: true, message: '目标语言不能为空', trigger: 'change' }]
})

// 角色列表相关
const roleList = ref<RoleApi.RoleVO[]>([]) // 角色列表数据
const roleListLoading = ref(false) // 角色列表加载状态

const formRef = ref() // 表单 Ref

// 监听源语言和目标语言的变化，自动生成语言方向
watch(
  [() => formData.value.sourceLanguage, () => formData.value.targetLanguage],
  ([source, target]) => {
    if (source && target) {
      const languageOptions = getDictOptions(DICT_TYPE.AI_TRANSLATE_LANGUAGE)
      const sourceLabel = languageOptions.find(opt => opt.value === source)?.label || source
      const targetLabel = languageOptions.find(opt => opt.value === target)?.label || target
      formData.value.languageDirection = `${sourceLabel}->${targetLabel}`
    } else {
      formData.value.languageDirection = ""
    }
  },
  { immediate: true }
)

/** 获取角色列表 */
const getRoleList = async () => {
  roleListLoading.value = true
  try {
    const params = {
      pageNo: 1,
      pageSize: 1000,
      code: '',
      name: '',
      status: undefined
    }
    const data = await RoleApi.getRoleListSelf(params)
    roleList.value = data
  } catch (error) {
    console.error('获取角色列表失败：', error)
    message.error(t('common.getRoleListFailed'))
  } finally {
    roleListLoading.value = false
  }
}

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  await getRoleList()
  // 修改时，设置数据
  if (id) {
    formLoading.value = true
    try {
      formData.value = await TranGlossaryApi.getTranGlossary(id)
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
    const data = formData.value as unknown as TranGlossary
    if (formType.value === 'create') {
      await TranGlossaryApi.createTranGlossary(data)
      message.success(t('common.createSuccess'))
    } else {
      await TranGlossaryApi.updateTranGlossary(data)
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
    id: "",
    glossaryName: "",
    sourceLanguage: "",
    targetLanguage: "",
    languageDirection: "",
    itemCount: 0,
    roleId: "",
    roleShow: "",
    userId: "",
    deptId: "",
    isEnabled: 1
  }
  formRef.value?.resetFields()
}
</script>