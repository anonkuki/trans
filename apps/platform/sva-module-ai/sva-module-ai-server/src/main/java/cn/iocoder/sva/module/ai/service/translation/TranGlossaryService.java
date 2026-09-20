package cn.iocoder.sva.module.ai.service.translation;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranGlossaryPageReqVO;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranGlossarySaveReqVO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranGlossaryDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 术语库管理 Service 接口
 *
 * @author like
 */
public interface TranGlossaryService {

    /**
     * 创建术语库管理
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createTranGlossary(@Valid TranGlossarySaveReqVO createReqVO);

    /**
     * 更新术语库管理
     *
     * @param updateReqVO 更新信息
     */
    void updateTranGlossary(@Valid TranGlossarySaveReqVO updateReqVO);

    /**
     * 删除术语库管理
     *
     * @param id 编号
     */
    void deleteTranGlossary(Long id);

    /**
    * 批量删除术语库管理
    *
    * @param ids 编号
    */
    void deleteTranGlossaryListByIds(List<Long> ids);

    /**
     * 获得术语库管理
     *
     * @param id 编号
     * @return 术语库管理
     */
    TranGlossaryDO getTranGlossary(Long id);

    /**
     * 获得术语库管理分页
     *
     * @param pageReqVO 分页查询
     * @return 术语库管理分页
     */
    PageResult<TranGlossaryDO> getTranGlossaryPage(TranGlossaryPageReqVO pageReqVO);

    /**
     * 获得术语库管理分页（指定用户ID）
     * <p>
     * 用于 AI Tool 等没有 SecurityContext 的场景
     *
     * @param pageReqVO 分页查询
     * @param userId 用户ID
     * @return 术语库管理分页
     */
    PageResult<TranGlossaryDO> getTranGlossaryPage(TranGlossaryPageReqVO pageReqVO, Long userId);

    /**
     * 获得术语库管理分页（指定用户ID和用户名）
     * <p>
     * 用于 AI Tool 等没有 SecurityContext 的场景，支持查询用户自己创建的术语库
     *
     * @param pageReqVO 分页查询
     * @param userId 用户ID
     * @param username 用户名
     * @return 术语库管理分页
     */
    PageResult<TranGlossaryDO> getTranGlossaryPage(TranGlossaryPageReqVO pageReqVO, Long userId, String username);

}