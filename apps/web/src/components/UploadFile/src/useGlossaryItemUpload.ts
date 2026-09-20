import * as TranGlossaryItemApi from '@/api/ai/translation/glossary/item/tranGlossaryItem'
import {
  UploadRawFile,
  UploadRequestOptions,
  UploadProgressEvent
} from 'element-plus/es/components/upload/src/upload'
import axios, { AxiosProgressEvent } from 'axios'

/**
 * 获得上传 URL
 */
export const getUploadUrl = (): string => {
  return import.meta.env.VITE_BASE_URL + import.meta.env.VITE_API_URL + '/ai/tran-glossary-item/upload'
}

export const useGlossaryItemUpload = (getParentId: () => number | undefined) => {
  // 后端上传地址
  const uploadUrl = getUploadUrl()
  // 是否使用前端直连上传
  const isClientUpload = UPLOAD_TYPE.CLIENT === import.meta.env.VITE_UPLOAD_TYPE
  // 重写ElUpload上传方法
  const httpRequest = async (options: UploadRequestOptions) => {
    // 文件上传进度监听
    const uploadProgressHandler = (evt: AxiosProgressEvent) => {
      const upEvt: UploadProgressEvent = Object.assign(evt.event)
      upEvt.percent = evt.progress ? evt.progress * 100 : 0
      options.onProgress(upEvt) // 触发 el-upload 的 on-progress
    }

    const parentId = getParentId()

    // 模式一：前端上传
    if (isClientUpload) {
      // 1.1 生成文件名称
      const fileName = options.file.name || options.filename
      // 1.2 获取文件预签名地址
      const presignedInfo = await TranGlossaryItemApi.getFilePresignedUrl(fileName, 'glossary-item')
      // 1.3 上传文件（不能使用 ElUpload 的 ajaxUpload 方法的原因：其使用的是 FormData 上传，Minio 不支持）
      return axios
        .put(presignedInfo.uploadUrl, options.file, {
          headers: {
            'Content-Type': options.file.type || 'application/octet-stream'
          },
          onUploadProgress: uploadProgressHandler
        })
        .then(() => {
          // 1.4. 记录文件信息到后端（异步）
          createFile(presignedInfo, options.file, fileName, parentId)
          // 通知成功，数据格式保持与后端上传的返回结果一致
          return { data: presignedInfo.url }
        })
    } else {
      // 模式二：后端上传
      // 重写 el-upload httpRequest 文件上传成功会走成功的钩子，失败走失败的钩子
      return new Promise((resolve, reject) => {
        TranGlossaryItemApi.uploadGlossaryItem({ file: options.file, parentId }, uploadProgressHandler)
          .then((res) => {
            if (res.code === 0) {
              resolve(res)
            } else {
              reject(res)
            }
          })
          .catch((res) => {
            reject(res)
          })
      })
    }
  }

  return {
    uploadUrl,
    httpRequest
  }
}

/**
 * 创建文件信息
 * @param vo 文件预签名信息
 * @param file 文件
 * @param fileName
 * @param parentId 父目录编号
 */
function createFile(vo: any, file: UploadRawFile, fileName: string, parentId?: number) {
  const fileVo = {
    configId: vo.configId,
    url: vo.url,
    path: vo.path,
    name: fileName,
    type: file.type || 'application/octet-stream',
    size: file.size
  }
  TranGlossaryItemApi.createFile(fileVo, parentId)
  return fileVo
}

/**
 * 上传类型
 */
enum UPLOAD_TYPE {
  // 客户端直接上传（只支持S3服务）
  CLIENT = 'client',
  // 客户端发送到后端上传
  SERVER = 'server'
}