import request from '@/config/axios'

export interface TempFileVO {
  id: number | undefined
  tempName: string
  name: string
  url: string
  type: string
  size: number
  createTime: Date
}

// 查询模板管理分页
export const getTempFilePage = (params: PageParam) => {
  return request.get({ url: '/system/temp-file/page', params })
}

// 查询模板管理详情
export const getTempFile = (id: number) => {
  return request.get({ url: '/system/temp-file/get?id=' + id })
}

// 新增模板管理
export const createTempFile = (data: TempFileVO) => {
  return request.post({ url: '/system/temp-file/create', data })
}

// 修改模板管理
export const updateTempFile = (data: TempFileVO) => {
  return request.put({ url: '/system/temp-file/update', data })
}

// 删除模板管理
export const deleteTempFile = (id: number) => {
  return request.delete({ url: '/system/temp-file/delete?id=' + id })
}

// 批量删除模板管理
export const deleteTempFileList = (ids: number[]) => {
  return request.delete({ url: '/system/temp-file/delete-list', params: { ids: ids.join(',') } })
}

// 导出模板管理
export const exportTempFile = (params) => {
  return request.download({ url: '/system/temp-file/export-excel', params })
}

// 上传文件
export const uploadFile = (file: File, tempName: string) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('tempName', tempName)
  return request.post({ url: '/system/temp-file/upload', data: formData, headers: { 'Content-Type': 'multipart/form-data' } })
}
