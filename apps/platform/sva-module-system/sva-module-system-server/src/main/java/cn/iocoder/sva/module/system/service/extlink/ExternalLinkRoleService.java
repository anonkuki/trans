package cn.iocoder.sva.module.system.service.extlink;

import java.util.*;
import static java.util.Collections.singleton;
import jakarta.validation.*;
import cn.iocoder.sva.module.system.controller.admin.extlink.vo.*;
import cn.iocoder.sva.module.system.dal.dataobject.extlink.ExternalLinkRoleDO;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.pojo.PageParam;

/**
 * 外链和角色关联 Service 接口
 *
 * @author like
 */
public interface ExternalLinkRoleService {

    /**
     * 创建外链和角色关联
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createExternalLinkRole(@Valid ExternalLinkRoleSaveReqVO createReqVO);

    /**
     * 更新外链和角色关联
     *
     * @param updateReqVO 更新信息
     */
    void updateExternalLinkRole(@Valid ExternalLinkRoleSaveReqVO updateReqVO);

    /**
     * 删除外链和角色关联
     *
     * @param id 编号
     */
    void deleteExternalLinkRole(Long id);

    /**
    * 批量删除外链和角色关联
    *
    * @param ids 编号
    */
    void deleteExternalLinkRoleListByIds(List<Long> ids);

    /**
     * 获得外链和角色关联
     *
     * @param id 编号
     * @return 外链和角色关联
     */
    ExternalLinkRoleDO getExternalLinkRole(Long id);

    /**
     * 获得外链和角色关联分页
     *
     * @param pageReqVO 分页查询
     * @return 外链和角色关联分页
     */
    PageResult<ExternalLinkRoleDO> getExternalLinkRolePage(ExternalLinkRolePageReqVO pageReqVO);

    /**
     * 获得角色拥有的外链编号数组，传入单个角色编号的场景使用，例如说，角色分配外链时，获取已经分配的外链编号数组，用于前端回显已选中的外链。
     *
     * @param roleId 角色编号数组，单个角色编号的场景使用。如果角色编号为空，则返回空数组。
     * @return 外链编号数组。如果角色不存在或者没有分配外链，则返回空数组。
     */
    default Set<Long> getLinkIdsByRoleId(Long roleId) {
        return getLinkIdsByRoleId(singleton(roleId));
    }

    /**
     * 获得角色拥有的外链编号数组，传入多个角色编号的场景使用。如果任一一个角色为管理员，则返回全部外链编号。
     *
     * @param roleIds 角色编号数组。如果角色编号为空，则返回空数组。
     * @return 外链编号数组。如果角色不存在或者没有分配外链，则返回空数组。
     */
    Set<Long> getLinkIdsByRoleId(Collection<Long> roleIds);

    /**
     * 赋予角色外链权限。如果角色已经有该外链权限，则不做任何处理。如果角色没有该外链权限，则新增角色外链关联。
     *
     * @param roleId  角色编号
     * @param linkIds 外链编号数组。如果为空，则清空角色外链权限。
     */
    void assignRoleExternalLink(Long roleId, Set<Long> linkIds);

    /**
     * 处理角色删除，级联物理删除角色关联的外链权限。如果角色不存在，则不做任何处理。
     *
     * @param roleId 角色编号
     */
    void processRoleDeleted(Long roleId);

    /**
     * 处理外链删除，级联物理删除外链关联的角色权限。如果外链不存在，则不做任何处理。
     *
     * @param linkId 外链编号
     */
    void processLinkDeleted(Long linkId);

    /**
     * 获得全部外链同步创建的菜单编号集合（不含未添加到菜单的外链），用于从常规菜单权限中排除外链菜单。
     *
     * @return 菜单编号集合。如果没有外链关联菜单，则返回空集合。
     */
    Set<Long> getAllLinkMenuIds();

    /**
     * 获得角色有权限的外链对应的菜单编号集合。如果任一一个角色为管理员，则返回全部外链菜单编号。
     *
     * @param roleIds 角色编号数组。如果角色编号为空，则返回空集合。
     * @return 菜单编号集合。如果角色没有外链权限或外链未关联菜单，则返回空集合。
     */
    Set<Long> getLinkMenuIdsByRoleIds(Collection<Long> roleIds);

}