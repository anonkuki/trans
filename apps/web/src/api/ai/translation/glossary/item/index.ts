import request from '@/config/axios'

/** 术语项管理信息 */
export interface TranGlossaryItem {
          id: number; // 主键id
          sourceLanguage?: string; // 源语言
          targetLanguage?: string; // 目标语言
          glossaryId: number; // 所属术语库id
  }

// 术语项管理 API
export const TranGlossaryItemApi = {
  // 查询术语项管理分页
  getTranGlossaryItemPage: async (params: any) => {
    return await request.get({ url: `/ai/tran-glossary-item/page`, params })
  },

  // 查询术语项管理详情
  getTranGlossaryItem: async (id: number) => {
    return await request.get({ url: `/ai/tran-glossary-item/get?id=` + id })
  },

  // 新增术语项管理
  createTranGlossaryItem: async (data: TranGlossaryItem) => {
    return await request.post({ url: `/ai/tran-glossary-item/create`, data })
  },

  // 修改术语项管理
  updateTranGlossaryItem: async (data: TranGlossaryItem) => {
    return await request.put({ url: `/ai/tran-glossary-item/update`, data })
  },

  // 删除术语项管理
  deleteTranGlossaryItem: async (id: number) => {
    return await request.delete({ url: `/ai/tran-glossary-item/delete?id=` + id })
  },

  /** 批量删除术语项管理 */
  deleteTranGlossaryItemList: async (ids: number[]) => {
    return await request.delete({ url: `/ai/tran-glossary-item/delete-list?ids=${ids.join(',')}` })
  },

  // 导出术语项管理 Excel
  exportTranGlossaryItem: async (params) => {
    return await request.download({ url: `/ai/tran-glossary-item/export-excel`, params })
  },

  // 获取模板文件URL（通过RPC接口）
  getTemplateUrl: async (tempName: string) => {
    return await request.get({ url: `/system/temp-file/get-by-temp-name?tempName=${tempName}` })
  }
}