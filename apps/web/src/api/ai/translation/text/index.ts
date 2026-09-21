import request from '@/config/axios'

/** 词句翻译请求参数 */
export interface TranTextTranslateReq {
  sourceText: string // 待翻译原文
  targetLang: string // 目标语言（English/Chinese）
  modelId?: number | null // 模型ID
  roleId?: number | null // 场景（聊天角色）ID
  glossaryIds?: number[] | null // 术语库ID列表
}

/** 词句翻译结果 */
export interface TranTextTranslateResp {
  id: number
  sourceText: string
  targetText: string
  targetLang: string
  modelName?: string
  roleName?: string
  createTime?: string
}

/** 词句翻译历史记录 */
export interface TranTextHistory {
  id: number
  sourceText: string
  targetText: string
  targetLang: string
  modelName?: string
  roleName?: string
  username?: string
  createTime?: string
}

// AI词句翻译 API
export const TranTextApi = {
  // 翻译文本
  translate: async (data: TranTextTranslateReq) => {
    return await request.post({ url: '/ai/tran/text/translate', data })
  },

  // 查询翻译历史分页（支持模糊搜索）
  getHistoryPage: async (params: any) => {
    return await request.get({ url: '/ai/tran/text/history/page', params })
  },

  // 删除单条翻译历史
  deleteHistory: async (id: number) => {
    return await request.delete({ url: '/ai/tran/text/history/delete?id=' + id })
  },

  // 清空当前用户的翻译历史
  clearHistory: async () => {
    return await request.delete({ url: '/ai/tran/text/history/clear' })
  }
}
