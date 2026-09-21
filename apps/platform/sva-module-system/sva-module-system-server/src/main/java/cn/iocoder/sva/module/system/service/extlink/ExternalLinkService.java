package cn.iocoder.sva.module.system.service.extlink;

import java.util.*;
import jakarta.validation.*;
import cn.iocoder.sva.module.system.controller.admin.extlink.vo.*;
import cn.iocoder.sva.module.system.dal.dataobject.extlink.ExternalLinkDO;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.pojo.PageParam;

/**
 * 系统外链 Service 接口
 *
 * @author like
 */
public interface ExternalLinkService {

    /**
     * 创建系统外链
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createExternalLink(@Valid ExternalLinkSaveReqVO createReqVO);

    /**
     * 更新系统外链
     *
     * @param updateReqVO 更新信息
     */
    void updateExternalLink(@Valid ExternalLinkSaveReqVO updateReqVO);

    /**
     * 删除系统外链
     *
     * @param id 编号
     */
    void deleteExternalLink(Long id);

    /**
    * 批量删除系统外链
    *
    * @param ids 编号
    */
    void deleteExternalLinkListByIds(List<Long> ids);

    /**
     * 获得系统外链
     *
     * @param id 编号
     * @return 系统外链
     */
    ExternalLinkDO getExternalLink(Long id);

    /**
     * 获得系统外链分页
     *
     * @param pageReqVO 分页查询
     * @return 系统外链分页
     */
    PageResult<ExternalLinkDO> getExternalLinkPage(ExternalLinkPageReqVO pageReqVO);

    /**
     * 获得开启状态的系统外链列表，用于【角色分配外链】功能的选项。
     *
     * @return 外链列表。如果没有开启状态的外链，则返回空列表。
     */
    List<ExternalLinkDO> getSimpleExternalLinkList();

    /**
     * 获得当前用户首页展示的外链列表。此处会根据用户的角色关联查询外链，保证用户只能看到有权限且开启状态的外链，并按外链序号从小到大排序。
     *
     * @param userId 用户编号。如果用户编号为空，则返回空列表。
     * @return 首页展示的外链列表。如果用户没有角色或者没有分配外链，则返回空列表。
     */
    List<ExternalLinkDO> getHomeExternalLinkList(Long userId);

    /**
     * 记录外链点击，自增外链的点击次数。如果外链不存在，则抛出异常。
     *
     * @param id 外链编号。如果外链编号为空，则抛出异常。
     */
    void clickExternalLink(Long id);

}