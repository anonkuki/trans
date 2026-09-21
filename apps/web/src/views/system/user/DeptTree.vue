<template>
  <div class="head-container">
    <el-input v-model="deptName" class="mb-20px" clearable placeholder="请输入部门名称">
      <template #prefix>
        <Icon icon="ep:search" />
      </template>
    </el-input>
  </div>
  <div class="head-container">
    <el-tree
      ref="treeRef"
      :data="deptList"
      :expand-on-click-node="true"
      :filter-node-method="filterNode"
      :props="defaultProps"
      default-expand-all
      highlight-current
      node-key="id"
      @node-click="handleNodeClick"
      class="dept-tree-scroll"
    />
  </div>
</template>

<script lang="ts" setup>
import { ElTree } from 'element-plus'
import * as DeptApi from '@/api/system/dept'
import { defaultProps, handleTree } from '@/utils/tree'

defineOptions({ name: 'SystemUserDeptTree' })

const deptName = ref('')
const deptList = ref<Tree[]>([]) // 树形结构
const treeRef = ref<InstanceType<typeof ElTree>>()

/** 获得部门树 */
const getTree = async () => {
  const res = await DeptApi.getSimpleDeptList()
  deptList.value = []
  deptList.value.push(...handleTree(res))
}

/** 基于名字过滤 */
const filterNode = (name: string, data: Tree) => {
  if (!name) return true
  return data.name.includes(name)
}

/** 处理部门被点击 */
let currentNode: any = {}
const handleNodeClick = async (row: { [key: string]: any }, treeNode: any, treeInstance: any) => {
  // 先触发查询
  emits('node-click', row)

  // 然后触发展开/收起（使用nextTick确保在DOM更新后执行）
  await nextTick()
  treeInstance.toggleNodeExpansion(row)
}
const emits = defineEmits(['node-click'])

/** 监听deptName */
watch(deptName, (val) => {
  treeRef.value!.filter(val)
})

/** 初始化 */
onMounted(async () => {
  await getTree()
})
</script>

<style scoped>
.head-container {
  overflow-x: auto;
  overflow-y: auto;
  width: 100%;
}

/* 确保树节点内容不换行且能完整显示 */
:deep(.el-tree) {
  min-width: 100%;
  width: max-content;
}

:deep(.el-tree-node__content) {
  white-space: nowrap;
  width: auto;
  min-width: 100%;
}

:deep(.el-tree-node__label) {
  display: inline-block;
  white-space: nowrap;
}

/* 自定义滚动条样式 */
.head-container::-webkit-scrollbar {
  height: 8px;
  width: 8px;
}

.head-container::-webkit-scrollbar-thumb {
  background-color: #dcdfe6;
  border-radius: 4px;
}

.head-container::-webkit-scrollbar-thumb:hover {
  background-color: #c0c4cc;
}

.head-container::-webkit-scrollbar-track {
  background-color: #f5f7fa;
  border-radius: 4px;
}
</style>
