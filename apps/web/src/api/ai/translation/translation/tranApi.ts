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

  // 获取可编辑术语库列表
  getEditableGlossaryList: async (targetLanguage?: string) => {
    const params = targetLanguage ? { targetLanguage } : {}
    return await request.get({ url: '/ai/tran/glossary/editable-list', params })
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
    translationFirst: boolean
    glossaryIds?: number[] | null  // 改为数组，支持多选
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
      translationFirst: data.translationFirst,
      glossaryIds: data.glossaryIds,  // 传递术语库ID数组
      modelId: data.modelId,
      roleId: data.roleId,
      disableCache: data.disableCache
    } })
  },

  // 获取翻译任务状态
  getTask: async (taskId: string) => {
    return await request.get({ url: `/ai/tran/task/${taskId}` })
  },

  // 下载翻译结果（通过后端接口，避免前端直接请求 MinIO 导致文件名中的特殊字符被编码）
  downloadFile: async (taskId: string, type: string): Promise<Blob> => {
    // 通过后端下载接口，后端生成预签名 URL 并 302 重定向到 MinIO
    // axios 自动跟随重定向，最终拿到文件 blob
    return await request.download({ url: `/ai/tran/download/${taskId}/${type}` })
  }
}