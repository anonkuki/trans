package cn.iocoder.sva.module.ai.service.translation;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranTextHistoryPageReqVO;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranTextTranslateReqVO;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranTextTranslateRespVO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranTextHistoryDO;
import jakarta.validation.Valid;

/**
 * AI词句翻译 Service 接口
 * <p>
 * 提供“词句翻译”页面的文本翻译能力，并维护每个用户最近 1000 条翻译历史，支持模糊搜索。
 *
 * @author like
 */
public interface TranTextService {

    /**
     * 翻译文本（同步），并保存翻译历史
     *
     * @param reqVO 翻译请求（原文、目标语言、模型、场景、术语库）
     * @return 翻译结果
     */
    TranTextTranslateRespVO translateText(@Valid TranTextTranslateReqVO reqVO);

    /**
     * 获得词句翻译历史分页（普通用户仅可见自己的记录，超级管理员可见全部）
     *
     * @param pageReqVO 分页查询条件
     * @return 翻译历史分页
     */
    PageResult<TranTextHistoryDO> getHistoryPage(TranTextHistoryPageReqVO pageReqVO);

    /**
     * 删除单条翻译历史
     *
     * @param id 记录ID
     */
    void deleteHistory(Long id);

    /**
     * 清空当前登录用户的翻译历史
     *
     * @return 被清除的记录数
     */
    int clearHistory();

}
