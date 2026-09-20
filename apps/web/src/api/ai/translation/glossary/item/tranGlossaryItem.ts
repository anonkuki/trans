import request from '@/config/axios'

// 文件预签名地址 Response VO
export interface FilePresignedUrlRespVO {
  // 文件配置编号
  configId: number
  // 文件上传 URL
  uploadUrl: string
  // 文件 URL
  url: string
  // 文件路径
  path: string
}

// 获取文件预签名地址
export const getFilePresignedUrl = (name: string, directory?: string) => {
  return request.get<FilePresignedUrlRespVO>({
    url: '/infra/file/presigned-url',
    params: { name, directory }
  })
}

// 创建文件
export const createFile = (data: any, parentId?: number) => {
  return request.post({ url: '/infra/file/create', data })
}

// 上传文件
export const uploadGlossaryItem = (data: any, onUploadProgress?: Function) => {
  return request.upload({ url: '/ai/tran-glossary-item/upload', data, onUploadProgress })
}