import request from '@/config/axios'

/** AI翻译文件信息信息 */
export interface TranFile {
          id: number; // 主键ID，自增
          fileName?: string; // 文件名
          fileUrl?: string; // 文件路径
          fileStatus?: number; // 文件状态：0-未翻译 1-翻译完成 2-异常文件
          fileType: string; // 文件类型
          context: string; // 文件说明
  }

// AI翻译文件信息 API
export const TranFileApi = {
  // 查询AI翻译文件信息分页
  getTranFilePage: async (params: any) => {
    return await request.get({ url: `/ai/tran-file/page`, params })
  },

  // 查询AI翻译文件信息详情
  getTranFile: async (id: number) => {
    return await request.get({ url: `/ai/tran-file/get?id=` + id })
  },

  // 新增AI翻译文件信息
  createTranFile: async (data: TranFile) => {
    return await request.post({ url: `/ai/tran-file/create`, data })
  },

  // 修改AI翻译文件信息
  updateTranFile: async (data: TranFile) => {
    return await request.put({ url: `/ai/tran-file/update`, data })
  },

  // 删除AI翻译文件信息
  deleteTranFile: async (id: number) => {
    return await request.delete({ url: `/ai/tran-file/delete?id=` + id })
  },

  /** 批量删除AI翻译文件信息 */
  deleteTranFileList: async (ids: number[]) => {
    return await request.delete({ url: `/ai/tran-file/delete-list?ids=${ids.join(',')}` })
  },

  // 导出AI翻译文件信息 Excel
  exportTranFile: async (params) => {
    return await request.download({ url: `/ai/tran-file/export-excel`, params })
  },

  // 下载翻译文件（通过后端接口，避免前端直接请求 MinIO 导致文件名特殊字符被编码）
  downloadFile: async (id: number, type: string = 'file'): Promise<Blob> => {
    return await request.download({ url: `/ai/tran-file/download/${id}?type=${type}` })
  }
}