<template>
  <Dialog v-model="dialogVisible" title="外链权限">
    <el-form ref="formRef" v-loading="formLoading" :model="formData" label-width="80px">
      <el-form-item label="角色名称">
        <el-tag>{{ formData.name }}</el-tag>
      </el-form-item>
      <el-form-item label="角色标识">
        <el-tag>{{ formData.code }}</el-tag>
      </el-form-item>
      <el-form-item label="外链权限">
        <el-card class="w-full h-400px !overflow-y-scroll" shadow="never">
          <template #header>
            全选/全不选:
            <el-switch
              v-model="linkAll"
              active-text="是"
              inactive-text="否"
              inline-prompt
              @change="handleCheckedLinkAll"
            />
          </template>
          <el-checkbox-group v-model="formData.linkIds">
            <template v-if="linkOptions.length">
              <div
                v-for="link in linkOptions"
                :key="link.id"
                class="mb-10px flex items-center"
              >
                <el-checkbox :value="link.id">
                  <span class="flex items-center">
                    <el-image
                      v-if="link.iconUrl"
                      :src="link.iconUrl"
                      class="mr-5px h-16px w-16px align-middle"
                    />
                    <Icon v-else-if="link.icon" :icon="link.icon" class="mr-5px" />
                    {{ link.name }}
                    <el-tag v-if="link.category" class="ml-5px" size="small" type="info">
                      {{ link.category }}
                    </el-tag>
                  </span>
                </el-checkbox>
              </div>
            </template>
            <el-empty v-else description="暂无开启状态的外链" :image-size="60" />
          </el-checkbox-group>
        </el-card>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import * as RoleApi from '@/api/system/role'
import * as ExternalLinkApi from '@/api/system/extlink'

defineOptions({ name: 'SystemRoleAssignExternalLinkForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formData = reactive({
  id: undefined,
  name: '',
  code: '',
  linkIds: []
})
const formRef = ref() // 表单 Ref
const linkOptions = ref<ExternalLinkApi.ExternalLinkVO[]>([]) // 外链列表
const linkAll = ref(false) // 全选/全不选

/** 打开弹窗 */
const open = async (row: RoleApi.RoleVO) => {
  dialogVisible.value = true
  resetForm()
  // 加载外链列表。注意，必须放在前面，不然下面回显没数据选项
  linkOptions.value = await ExternalLinkApi.getSimpleExternalLinkList()
  // 设置数据
  formData.id = row.id
  formData.name = row.name
  formData.code = row.code
  formLoading.value = true
  try {
    formData.linkIds = await ExternalLinkApi.getRoleExternalLinkList(row.id)
    // 全选状态回显
    linkAll.value =
      linkOptions.value.length > 0 && formData.linkIds.length === linkOptions.value.length
  } finally {
    formLoading.value = false
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
    const data = {
      roleId: formData.id,
      linkIds: [...formData.linkIds]
    }
    await ExternalLinkApi.assignRoleExternalLink(data)
    message.success(t('common.updateSuccess'))
    dialogVisible.value = false
    // 发送操作成功的事件
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  // 重置选项
  linkAll.value = false
  // 重置表单
  formData.id = undefined
  formData.name = ''
  formData.code = ''
  formData.linkIds = []
  formRef.value?.resetFields()
}

/** 全选/全不选 */
const handleCheckedLinkAll = () => {
  formData.linkIds = linkAll.value ? linkOptions.value.map((link) => link.id) : []
}
</script>
