import request from '@/config/axios'

export interface RoleVO {
  id: number
  name: string
  code: string
  sort: number
  status: number
  type: number
  dataScope: number
  dataScopeDeptIds: number[]
  createTime: Date
  creatorRole?: number
}

// 查询角色列表
export const getRolePage = async (params: PageParam) => {
  return await request.get({ url: '/system/role/page', params })
}

export const getRoleListSelf = async (params: PageParam) => {
  return await request.get({ url: '/system/role/listself', params })
}

// 查询角色（精简)列表
export const getSimpleRoleList = async (): Promise<RoleVO[]> => {
  return await request.get({ url: '/system/role/simple-list' })
}

// 查询角色（精简)列表（结合当前角色）
export const getSimpleLoginRoleList = async (): Promise<RoleVO[]> => {
  return await request.get({ url: '/system/role/login-role-select' })
}

// 查询当前登录角色（精简)列表
export const getLoginRoleList = async (): Promise<RoleVO[]> => {
  return await request.get({ url: '/system/role/login-role' })
}

// 查询角色详情
export const getRole = async (id: number) => {
  return await request.get({ url: '/system/role/get?id=' + id })
}

// 新增角色
export const createRole = async (data: RoleVO) => {
  return await request.post({ url: '/system/role/create', data })
}

// 修改角色
export const updateRole = async (data: RoleVO) => {
  return await request.put({ url: '/system/role/update', data })
}

// 删除角色
export const deleteRole = async (id: number) => {
  return await request.delete({ url: '/system/role/delete?id=' + id })
}

// 批量删除角色
export const deleteRoleList = async (ids: number[]) => {
  return await request.delete({ url: '/system/role/delete-list', params: { ids: ids.join(',') } })
}

// 导出角色
export const exportRole = (params: any) => {
  return request.download({
    url: '/system/role/export-excel',
    params
  })
}

// ===============部门角色相关接口================

/** 部门和角色关联信息 */
export interface DeptRole {
          id: number; // 自增编号
          deptId?: number; // 部门ID
          roleId?: number; // 角色ID
  }

// 部门和角色关联 API
export const DeptRoleApi = {
  // 查询部门和角色关联分页
  getDeptRolePage: async (params: any) => {
    return await request.get({ url: `/system/dept-role/page`, params })
  },

  // 查询部门和角色关联详情
  getDeptRole: async (id: number) => {
    return await request.get({ url: `/system/dept-role/get?id=` + id })
  },

  // 新增部门和角色关联
  createDeptRole: async (data: DeptRole) => {
    return await request.post({ url: `/system/dept-role/create`, data })
  },

  // 修改部门和角色关联
  updateDeptRole: async (data: DeptRole) => {
    return await request.put({ url: `/system/dept-role/update`, data })
  },

  // 删除部门和角色关联
  deleteDeptRole: async (id: number) => {
    return await request.delete({ url: `/system/dept-role/delete?id=` + id })
  },

  /** 批量删除部门和角色关联 */
  deleteDeptRoleList: async (ids: number[]) => {
    return await request.delete({ url: `/system/dept-role/delete-list?ids=${ids.join(',')}` })
  },

  // 导出部门和角色关联 Excel
  exportDeptRole: async (params) => {
    return await request.download({ url: `/system/dept-role/export-excel`, params })
  }
}
