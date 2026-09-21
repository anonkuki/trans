package cn.iocoder.sva.module.ai.service.translation;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranFilePageReqVO;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranFileSaveReqVO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranFileDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * AI翻译文件信息 Service 接口
 *
 * @author like
 */
public interface TranFileService {

    /**
     * 创建AI翻译文件信息
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createTranFile(@Valid TranFileSaveReqVO createReqVO);

    /**
     * 创建AI翻译文件信息（直接接收 DO 对象）
     *
     * @param tranFileDO 文件信息
     * @return 编号
     */
    Long createTranFile(TranFileDO tranFileDO);

    /**
     * 更新AI翻译文件信息
     *
     * @param updateReqVO 更新信息
     */
    void updateTranFile(@Valid TranFileSaveReqVO updateReqVO);

    /**
     * 更新AI翻译文件信息（直接接收 DO 对象）
     *
     * @param tranFileDO 文件信息
     */
    void updateTranFile(TranFileDO tranFileDO);

    /**
     * 删除AI翻译文件信息
     *
     * @param id 编号
     */
    void deleteTranFile(Long id);

    /**
    * 批量删除AI翻译文件信息
    *
    * @param ids 编号
    */
    void deleteTranFileListByIds(List<Long> ids);

    /**
     * 删除单个翻译文件记录及其关联的 MinIO 文件
     *
     * @param id 文件记录ID
     * @throws Exception 删除异常
     */
    void deleteTranFileWithMinioFiles(Long id) throws Exception;

    /**
     * 批量删除翻译文件记录及其关联的 MinIO 文件
     *
     * @param ids 文件记录ID列表
     * @throws Exception 删除异常
     */
    void deleteTranFileListWithMinioFiles(List<Long> ids) throws Exception;

    /**
     * 获得AI翻译文件信息
     *
     * @param id 编号
     * @return AI翻译文件信息
     */
    TranFileDO getTranFile(Long id);

    /**
     * 获得当前登录用户可访问的翻译文件；超级管理员可访问全部记录。
     */
    TranFileDO getTranFileForCurrentUser(Long id);

    /**
     * 获得AI翻译文件信息分页
     *
     * @param pageReqVO 分页查询
     * @return AI翻译文件信息分页
     */
    PageResult<TranFileDO> getTranFilePage(TranFilePageReqVO pageReqVO);

}
