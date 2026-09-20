<template>
  <Dialog v-model="dialogVisible" title="组织角色" width="800">
    <el-form ref="formRef" v-loading="formLoading" :model="formData" label-width="80px">
      <el-form-item label="角色名称">
        <el-tag>{{ formData.name }}</el-tag>
      </el-form-item>
      <el-form-item label="角色标识">
        <el-tag>{{ formData.code }}</el-tag>
      </el-form-item>
      <el-form-item label="部门范围" label-width="80px">
        <el-card class="w-full h-400px !overflow-y-scroll" shadow="never">
          <template #header>
            全选/全不选:
            <el-switch
              v-model="treeNodeAll"
              active-text="是"
              inactive-text="否"
              inline-prompt
              @change="handleCheckedTreeNodeAll()"
            />
            全部展开/折叠:
            <el-switch
              v-model="deptExpand"
              active-text="展开"
              inactive-text="折叠"
              inline-prompt
              @change="handleCheckedTreeExpand"
            />
            父子联动(选中父节点，自动选择子节点):
            <el-switch v-model="checkStrictly" active-text="是" inactive-text="否" inline-prompt />
          </template>
          <el-tree
            ref="treeRef"
            :check-strictly="!checkStrictly"
            :data="deptOptions"
            :props="defaultProps"
            default-expand-all
            empty-text="加载中，请稍后"
            node-key="id"
            show-checkbox
          />
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
import { defaultProps, handleTree } from '@/utils/tree'
import { SystemDataScopeEnum } from '@/utils/constants'
import * as DeptApi from '@/api/system/dept'
import * as PermissionApi from '@/api/system/permission'
import type { RoleVO } from '@/api/system/role'

defineOptions({ name: 'SystemRoleOrgPermissionForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const formLoading = ref(false)
const formData = reactive({
  id: undefined as number | undefined,
  name: '',
  code: ''
})
const formRef = ref()
const deptOptions = ref<any[]>([])
const deptExpand = ref(true)
const treeRef = ref()
const treeNodeAll = ref(false)
const checkStrictly = ref(true)

/** 打开弹窗 */
const open = async (row: RoleVO) => {
  dialogVisible.value = true
  resetForm()
  formLoading.value = true
  try {
    // 1. 加载部门树
    deptOptions.value = handleTree(await DeptApi.getSimpleDeptList())
    // 2. 设置角色基本信息
    formData.id = row.id
    formData.name = row.name
    formData.code = row.code
    // 3. 单独请求后端接口，获取该角色已分配的组织部门ID列表
    const orgDeptIds = await PermissionApi.getRoleOrgScope(row.id)
    await nextTick()
    // 4. 回显选中的部门
    if (orgDeptIds && orgDeptIds.length) {
      orgDeptIds.forEach((deptId: number) => {
        treeRef.value?.setChecked(deptId, true, false)
      })
    }
  } finally {
    formLoading.value = false
  }
}
defineExpose({ open })

/** 提交表单 */
const emit = defineEmits(['success'])
const submitForm = async () => {
  formLoading.value = true
  try {
    const data = {
      roleId: formData.id,
      dataScope: SystemDataScopeEnum.DEPT_CUSTOM,
      dataScopeDeptIds: treeRef.value?.getCheckedKeys(false) || []
    }
    await PermissionApi.assignRoleOrgScope(data)
    message.success(t('common.updateSuccess'))
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  treeNodeAll.value = false
  deptExpand.value = true
  checkStrictly.value = true
  formData.id = undefined
  formData.name = ''
  formData.code = ''
  treeRef.value?.setCheckedNodes([])
  formRef.value?.resetFields()
}

/** 全选/全不选 */
const handleCheckedTreeNodeAll = () => {
  treeRef.value?.setCheckedNodes(treeNodeAll.value ? deptOptions.value : [])
}

/** 展开/折叠全部 */
const handleCheckedTreeExpand = () => {
  const nodes = treeRef.value?.store.nodesMap
  if (!nodes) return
  for (const node in nodes) {
    if (nodes[node].expanded === deptExpand.value) continue
    nodes[node].expanded = deptExpand.value
  }
}
</script>