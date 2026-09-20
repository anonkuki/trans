import request from '@/config/axios'

// 翻译任务状态
export interface TranslateTask {
  taskId: string
  status: 'pending' | 'running' | 'completed' | 'failed'
  progress: number
  progressMsg: string
  elapsed: number
  realtime_pairs: any[]
  pairs: any[]
  downloads: {
    file?: boolean
    excel?: boolean
    qc?: boolean
  }
  minioUrls: {
    file?: string
    excel?: string
    qc?: string
  }
  cost_cny: number
  qc_issues: number | null
  error?: string
}

// 翻译 API
export const TranApi = {
  // 获取术语库
  getGlossary: async (glossaryId?: number) => {
    const params = glossaryId ? { glossaryId } : {}
    return await request.get({ url: '/ai/tran/glossary', params })
  },

  // 获取可见术语库列表
  getVisibleGlossaryList: async (targetLanguage?: string) => {
    const params = targetLanguage ? { targetLanguage } : {}
    return await request.get({ url: '/ai/tran/glossary/visible-list', params })
  },

  // 获取模型列表
  getModelSimpleList: async (type?: number) => {
    const params = type !== undefined ? { type } : {}
    return await request.get({ url: '/ai/model/simple-list', params })
  },

  // 导出术语库JSON
  exportGlossary: () => {
    window.open('/admin-api/ai/tran/glossary/export', '_blank')
  },

  // 导入术语库(Excel)
  importGlossary: async (file: File) => {
    return await request.upload({ url: '/ai/tran/glossary/import', data: { file } })
  },

  // 保存术语到术语库
  saveGlossary: async (terms: Record<string, string>, targetLanguage: string = 'en', glossaryId?: number) => {
    return await request.post({ url: '/ai/tran/glossary/save', data: terms, params: { targetLanguage, glossaryId } })
  },

  // 提交翻译任务
  submitTranslate: async (data: {
    file: File
    targetLang: string
    useGlossaryReplace: boolean
    strictFormat: boolean
    enableComparison: boolean
    enableQc: boolean
    glossaryId?: number | null
    modelId?: number | null
    roleId?: number | null
    disableCache?: number
  }) => {
    return await request.upload({ url: '/ai/tran/translate', data: {
      file: data.file,
      targetLang: data.targetLang,
      useGlossaryReplace: data.useGlossaryReplace,
      strictFormat: data.strictFormat,
      enableComparison: data.enableComparison,
      enableQc: data.enableQc,
      glossaryId: data.glossaryId,
      modelId: data.modelId,
      roleId: data.roleId,
      disableCache: data.disableCache
    } })
  },

  // 获取翻译任务状态
  getTask: async (taskId: string) => {
    return await request.get({ url: `/ai/tran/task/${taskId}` })
  },

  // 下载翻译结果（使用 MinIO URL）
  downloadFile: async (taskId: string, type: string) => {
    try {
      // 先获取任务信息，包含 MinIO URL
      const taskRes = await request.get({ url: `/ai/tran/task/${taskId}` })
      const taskData = taskRes.data || taskRes
      
      if (!taskData.minioUrls || !taskData.minioUrls[type]) {
        throw new Error(`文件类型 ${type} 的下载链接不存在`)
      }
      
      const minioUrl = taskData.minioUrls[type]
      
      // 使用 fetch 下载文件
      const response = await fetch(minioUrl)
      if (!response.ok) {
        throw new Error(`下载失败: ${response.status}`)
      }
      
      const blob = await response.blob()
      const blobUrl = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = blobUrl
      
      // 设置文件名
      let filename = `download_${type}_${taskId}`
      if (type === 'file') {
        filename = taskData.downloads?.file?.filename || filename
      } else if (type === 'excel') {
        filename = taskData.downloads?.excel?.filename || filename
      } else if (type === 'qc') {
        filename = taskData.downloads?.qc?.filename || filename
      }
      
      a.download = filename
      a.click()
      URL.revokeObjectURL(blobUrl)
      
    } catch (error) {
      console.error('下载文件失败:', error)
      throw error
    }
  }
}