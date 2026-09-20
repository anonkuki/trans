package cn.iocoder.sva.module.ai.service.translation;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.module.ai.controller.admin.file.vo.file.FileUploadReqVO;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranGlossaryItemPageReqVO;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranGlossaryItemSaveReqVO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranGlossaryItemDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 术语管理 Service 接口
 *
 * @author like
 */
public interface TranGlossaryItemService {

    /**
     * 创建术语管理
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createTranGlossaryItem(@Valid TranGlossaryItemSaveReqVO createReqVO);

    /**
     * 更新术语管理
     *
     * @param updateReqVO 更新信息
     */
    void updateTranGlossaryItem(@Valid TranGlossaryItemSaveReqVO updateReqVO);

    /**
     * 删除术语管理
     *
     * @param id 编号
     */
    void deleteTranGlossaryItem(Long id);

    /**
    * 批量删除术语管理
    *
    * @param ids 编号
    */
    void deleteTranGlossaryItemListByIds(List<Long> ids);

    /**
     * 获得术语管理
     *
     * @param id 编号
     * @return 术语管理
     */
    TranGlossaryItemDO getTranGlossaryItem(Long id);

    /**
     * 获得术语管理分页
     *
     * @param pageReqVO 分页查询
     * @return 术语管理分页
     */
    PageResult<TranGlossaryItemDO> getTranGlossaryItemPage(TranGlossaryItemPageReqVO pageReqVO);

    List<TranGlossaryItemDO> getTranGlossaryItemListByGlossaryId(Long glossaryId);

    String uploadFile(@Valid FileUploadReqVO uploadReqVO);
}