import request from '@/config/axios'

/** 术语库管理信息 */
export interface TranGlossary {
          id: number; // 主键id
          glossaryName?: string; // 术语库名称
          sourceLanguage?: string; // 源语言
          targetLanguage?: string; // 目标语言
          languageDirection?: string; // 语言方向
          itemCount?: number; // 条目数量
          roleId: number; // 所属角色
          userId: number; // 所属人员
          deptId: number; // 所属部门
          isEnabled?: number; // 是否启用 1=启用 0=禁用
  }

// 术语库管理 API
export const TranGlossaryApi = {
  // 查询术语库管理分页
  getTranGlossaryPage: async (params: any) => {
    return await request.get({ url: `/ai/tran-glossary/page`, params })
  },

  // 查询术语库管理详情
  getTranGlossary: async (id: number) => {
    return await request.get({ url: `/ai/tran-glossary/get?id=` + id })
  },

  // 新增术语库管理
  createTranGlossary: async (data: TranGlossary) => {
    return await request.post({ url: `/ai/tran-glossary/create`, data })
  },

  // 修改术语库管理
  updateTranGlossary: async (data: TranGlossary) => {
    return await request.put({ url: `/ai/tran-glossary/update`, data })
  },

  // 删除术语库管理
  deleteTranGlossary: async (id: number) => {
    return await request.delete({ url: `/ai/tran-glossary/delete?id=` + id })
  },

  /** 批量删除术语库管理 */
  deleteTranGlossaryList: async (ids: number[]) => {
    return await request.delete({ url: `/ai/tran-glossary/delete-list?ids=${ids.join(',')}` })
  },

  // 导出术语库管理 Excel
  exportTranGlossary: async (params) => {
    return await request.download({ url: `/ai/tran-glossary/export-excel`, params })
  }
}