<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="80px"
    >
      <el-row>
        <el-col :span="12">
          <el-form-item label="用户昵称" prop="nickname">
            <el-input v-model="formData.nickname" placeholder="请输入用户昵称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="归属部门" prop="deptId">
            <el-tree-select
              v-model="formData.deptId"
              :data="deptRootNodes"
              :props="defaultProps"
              check-strictly
              node-key="id"
              lazy
              :load="loadDeptNodes"
              placeholder="请选择归属部门"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item label="手机号码" prop="mobile">
            <el-input v-model="formData.mobile" maxlength="11" placeholder="请输入手机号码" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="formData.email" maxlength="50" placeholder="请输入邮箱" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item v-if="formData.id === undefined" label="用户名称" prop="username">
            <el-input v-model="formData.username" placeholder="请输入用户名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item v-if="formData.id === undefined" label="用户密码" prop="password">
            <el-input
              v-model="formData.password"
              placeholder="请输入用户密码"
              show-password
              type="password"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item label="用户性别">
            <el-select v-model="formData.sex" placeholder="请选择">
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.SYSTEM_USER_SEX)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="岗位">
            <el-select-v2
              ref="postSelectRef"
              v-model="formData.postIds"
              multiple
              filterable
              remote
              :remote-method="remoteSearchPost"
              :loading="postLoading"
              placeholder="请输入关键词搜索"
              :options="postOptions"
              :props="{ label: 'name', value: 'id' }"
              popper-class="post-select-popper"
              style="width: 100%"
              @visible-change="handlePostVisibleChange"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item label="备注">
            <el-input v-model="formData.remark" placeholder="请输入内容" type="textarea" />
          </el-form-item>
        </el-col>
      </el-row>
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
import { defaultProps } from '@/utils/tree'
import * as PostApi from '@/api/system/post'
import * as DeptApi from '@/api/system/dept'
import * as UserApi from '@/api/system/user'
import { FormRules } from 'element-plus'

defineOptions({ name: 'SystemUserForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formData = ref({
  nickname: '',
  deptId: '',
  mobile: '',
  email: '',
  id: undefined,
  username: '',
  password: '',
  sex: undefined,
  postIds: [] as number[],
  remark: '',
  status: CommonStatusEnum.ENABLE,
  roleIds: []
})
const formRules = reactive<FormRules>({
  username: [{ required: true, message: '用户名称不能为空', trigger: 'blur' }],
  nickname: [{ required: true, message: '用户昵称不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '用户密码不能为空', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }],
  mobile: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }]
})
const formRef = ref()

// 部门树懒加载
const deptRootNodes = ref<any[]>([])
const loadDeptNodes = async (node: any, resolve: (data: any[]) => void) => {
  const parentId = node.level === 0 ? 0 : node.data.id
  const children = await DeptApi.getDeptChildren(parentId)
  resolve(children)
}

// 构建包含指定部门ID的完整路径节点
const buildDeptPathNodes = async (deptId: number) => {
  if (!deptId) return []
  
  // 获取当前部门信息
  const currentDept = await DeptApi.getDept(deptId)
  if (!currentDept) return []
  
  // 递归构建从根到当前部门的完整路径
  let current = currentDept
  
  // 构建祖先节点路径（从根到父节点）
  const ancestors: any[] = []
  while (current && current.parentId && current.parentId !== 0) {
    const parent = await DeptApi.getDept(current.parentId)
    if (parent) {
      ancestors.unshift({
        id: parent.id,
        name: parent.name,
        parentId: parent.parentId,
        children: []
      })
      current = parent
    } else {
      break
    }
  }
  
  // 添加当前节点
  const currentNode = {
    id: currentDept.id,
    name: currentDept.name,
    parentId: currentDept.parentId,
    children: []
  }
  
  // 组装完整路径
  if (ancestors.length > 0) {
    // 将当前节点添加到最后一个祖先节点的children中
    ancestors[ancestors.length - 1].children = [currentNode]
    return ancestors
  } else {
    return [currentNode]
  }
}

// 岗位无限滚动相关
const postSelectRef = ref()
const postList = ref<PostApi.PostSimpleVO[]>([])
const postLoading = ref(false)
const postLoadingMore = ref(false)
const postHasMore = ref(true)
const postPageNo = ref(1)
const postPageSize = 200
let searchKeyword = ref('') // 当前搜索关键词
let scrollContainer: HTMLElement | null = null

const postOptions = computed(() => postList.value)

// 加载岗位列表（分页追加）
const loadPosts = async (pageNo: number, keyword: string = '', isAppend = true) => {
  if (postLoadingMore.value) return
  postLoadingMore.value = true
  try {
    const params: any = {
      pageNo,
      pageSize: postPageSize,
      status: CommonStatusEnum.ENABLE
    }
    if (keyword) {
      params.name = keyword
    }
    const res = await PostApi.getPostPage(params)
    const newPosts = res.list || []
    if (isAppend) {
      // 去重合并（防止重复加载同一页）
      const existingIds = new Set(postList.value.map((p) => p.id))
      const filteredNew = newPosts.filter((p) => !existingIds.has(p.id))
      postList.value = [...postList.value, ...filteredNew]
    } else {
      postList.value = newPosts
    }
    // 判断是否还有更多
    postHasMore.value = newPosts.length === postPageSize && pageNo * postPageSize < res.total
    return newPosts
  } finally {
    postLoadingMore.value = false
  }
}

// 重置岗位列表（搜索时调用）
const resetPostList = async (keyword: string) => {
  searchKeyword.value = keyword
  postPageNo.value = 1
  postHasMore.value = true
  // 清空当前列表，但保留已选中的岗位（确保回显）
  const selectedIds = new Set(formData.value.postIds)
  const selectedPosts = postList.value.filter((p) => selectedIds.has(p.id))
  postList.value = [...selectedPosts]
  await loadPosts(1, keyword, false) // 重置时不追加，直接替换
  // 如果还有更多，继续加载？这里只加载第一页，滚动时再加载
}

// 加载更多（滚动到底部触发）
const loadMore = async () => {
  if (!postHasMore.value || postLoadingMore.value) return
  const nextPage = postPageNo.value + 1
  await loadPosts(nextPage, searchKeyword.value, true)
  postPageNo.value = nextPage
}

// 滚动事件处理
const handleScroll = (e: Event) => {
  const target = e.target as HTMLElement
  // 判断是否滚动到底部（允许一定误差）
  const isBottom = target.scrollHeight - target.scrollTop - target.clientHeight < 10
  if (isBottom && postHasMore.value && !postLoadingMore.value) {
    loadMore()
  }
}

// 绑定/解绑滚动事件
const bindScrollEvent = () => {
  // 获取下拉面板容器（通过 popper 类名）
  const popper = document.querySelector('.post-select-popper')
  if (popper) {
    scrollContainer = popper.querySelector('.el-select-v2__dropdown') as HTMLElement
    if (scrollContainer) {
      scrollContainer.addEventListener('scroll', handleScroll)
    }
  }
}

const unbindScrollEvent = () => {
  if (scrollContainer) {
    scrollContainer.removeEventListener('scroll', handleScroll)
    scrollContainer = null
  }
}

// 下拉面板显示/隐藏回调
const handlePostVisibleChange = (visible: boolean) => {
  if (visible) {
    // 延迟一点确保 DOM 渲染完成
    setTimeout(() => bindScrollEvent(), 100)
  } else {
    unbindScrollEvent()
  }
}

// 远程搜索（带防抖）
let searchTimer: any = null
const remoteSearchPost = async (query: string) => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(async () => {
    if (!query) {
      // 清空搜索，恢复默认列表
      await resetPostList('')
    } else {
      await resetPostList(query)
    }
  }, 300)
}

// 加载已选中的岗位（用于回显）
const loadSelectedPosts = async (ids: number[]) => {
  if (!ids.length) return
  const selectedPosts = await PostApi.getPostListByIds(ids)
  const existingIds = new Set(postList.value.map((p) => p.id))
  const newPosts = selectedPosts.filter((p) => !existingIds.has(p.id))
  if (newPosts.length) {
    postList.value = [...newPosts, ...postList.value]
  }
}

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  // 重置岗位状态
  postPageNo.value = 1
  postHasMore.value = true
  searchKeyword.value = ''
  postList.value = []
  // 加载默认第一页
  await loadPosts(1, '', false)
  // 修改时，设置数据
  if (id) {
    formLoading.value = true
    try {
      formData.value = await UserApi.getUser(id)
      // 加载已选中的岗位（会合并到 postList）
      if (formData.value.postIds && formData.value.postIds.length) {
        await loadSelectedPosts(formData.value.postIds)
      }
      // 如果有部门ID，确保部门树中包含该部门节点以便正确显示
      if (formData.value.deptId) {
        // 重新初始化部门根节点，确保能正确回显
        deptRootNodes.value = await buildDeptPathNodes(Number(formData.value.deptId))
      }
    } finally {
      formLoading.value = false
    }
  } else {
    // 新建时，初始化空的部门根节点
    deptRootNodes.value = []
  }
}
defineExpose({ open })

/** 提交表单 */
const emit = defineEmits(['success'])
const submitForm = async () => {
  if (!formRef) return
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    const data = formData.value as unknown as UserApi.UserVO
    if (formType.value === 'create') {
      await UserApi.createUser(data)
      message.success(t('common.createSuccess'))
    } else {
      await UserApi.updateUser(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.value = {
    nickname: '',
    deptId: '',
    mobile: '',
    email: '',
    id: undefined,
    username: '',
    password: '',
    sex: undefined,
    postIds: [] as number[],
    remark: '',
    status: CommonStatusEnum.ENABLE,
    roleIds: []
  }
  formRef.value?.resetFields()
}
</script>

<style>
/* 可选：确保下拉面板滚动条可见 */
.post-select-popper .el-select-v2__dropdown {
  max-height: 300px;
  overflow-y: auto;
}
</style>
