import request from '@/config/axios'

export interface ExternalLinkVO {
  id?: number
  name: string
  url: string
  icon: string
  iconUrl: string
  description: string
  category: string
  sort: number
  status: number
  openTarget: number
  clickCount: number
  menuId?: number
  addMenuFlag?: boolean
  createTime?: Date
}

// 查询系统外链分页
export const getExternalLinkPage = async (params: PageParam) => {
  return await request.get({ url: '/system/external-link/page', params })
}

// 查询系统外链详情
export const getExternalLink = async (id: number) => {
  return await request.get({ url: '/system/external-link/get?id=' + id })
}

// 新增系统外链
export const createExternalLink = async (data: ExternalLinkVO) => {
  return await request.post({ url: '/system/external-link/create', data })
}

// 修改系统外链
export const updateExternalLink = async (data: ExternalLinkVO) => {
  return await request.put({ url: '/system/external-link/update', data })
}

// 删除系统外链
export const deleteExternalLink = async (id: number) => {
  return await request.delete({ url: '/system/external-link/delete?id=' + id })
}

// 批量删除系统外链
export const deleteExternalLinkList = async (ids: number[]) => {
  return await request.delete({ url: '/system/external-link/delete-list', params: { ids: ids.join(',') } })
}

// 导出系统外链
export const exportExternalLink = async (params) => {
  return await request.download({ url: '/system/external-link/export-excel', params })
}

// 获取系统外链精简信息列表（只包含被开启的外链，用于角色分配外链的选项）
export const getSimpleExternalLinkList = async (): Promise<ExternalLinkVO[]> => {
  return await request.get({ url: '/system/external-link/simple-list' })
}

// 获得首页展示的外链列表（当前登录用户有权限且开启状态的外链）
export const getHomeExternalLinkList = async (): Promise<ExternalLinkVO[]> => {
  return await request.get({ url: '/system/external-link/home-list' })
}

// 记录外链点击
export const clickExternalLink = async (id: number) => {
  return await request.post({ url: '/system/external-link/click?id=' + id })
}

// 获得角色拥有的外链编号数组
export const getRoleExternalLinkList = async (roleId: number) => {
  return await request.get({ url: '/system/external-link-role/list-role-links?roleId=' + roleId })
}

// 赋予角色外链权限
export const assignRoleExternalLink = async (data: { roleId: number; linkIds: number[] }) => {
  return await request.post({ url: '/system/external-link-role/assign-role-link', data })
}
