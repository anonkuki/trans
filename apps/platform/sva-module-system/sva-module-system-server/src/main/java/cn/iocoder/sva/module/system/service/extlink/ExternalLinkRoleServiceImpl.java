package cn.iocoder.sva.module.system.service.extlink;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import static java.util.Collections.singleton;
import cn.iocoder.sva.module.system.controller.admin.extlink.vo.*;
import cn.iocoder.sva.module.system.dal.dataobject.extlink.ExternalLinkDO;
import cn.iocoder.sva.module.system.dal.dataobject.extlink.ExternalLinkRoleDO;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.pojo.PageParam;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;

import cn.iocoder.sva.module.system.dal.mysql.extlink.ExternalLinkMapper;
import cn.iocoder.sva.module.system.dal.mysql.extlink.ExternalLinkRoleMapper;
import cn.iocoder.sva.module.system.service.permission.RoleService;

import static cn.iocoder.sva.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.sva.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.sva.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.sva.framework.common.util.collection.CollectionUtils.diffList;
import static cn.iocoder.sva.module.system.enums.ErrorCodeConstants.*;

/**
 * 外链和角色关联 Service 实现类
 *
 * @author like
 */
@Service
@Validated
public class ExternalLinkRoleServiceImpl implements ExternalLinkRoleService {

    @Resource
    private ExternalLinkRoleMapper externalLinkRoleMapper;

    @Resource
    private ExternalLinkMapper externalLinkMapper;
    @Resource
    private RoleService roleService;

    @Override
    public Long createExternalLinkRole(ExternalLinkRoleSaveReqVO createReqVO) {
        // 插入
        ExternalLinkRoleDO externalLinkRole = BeanUtils.toBean(createReqVO, ExternalLinkRoleDO.class);
        externalLinkRoleMapper.insert(externalLinkRole);

        // 返回
        return externalLinkRole.getId();
    }

    @Override
    public void updateExternalLinkRole(ExternalLinkRoleSaveReqVO updateReqVO) {
        // 校验存在
        validateExternalLinkRoleExists(updateReqVO.getId());
        // 更新
        ExternalLinkRoleDO updateObj = BeanUtils.toBean(updateReqVO, ExternalLinkRoleDO.class);
        externalLinkRoleMapper.updateById(updateObj);
    }

    @Override
    public void deleteExternalLinkRole(Long id) {
        // 校验存在
        validateExternalLinkRoleExists(id);
        // 删除
        externalLinkRoleMapper.deleteById(id);
    }

    @Override
        public void deleteExternalLinkRoleListByIds(List<Long> ids) {
        // 删除
        externalLinkRoleMapper.deleteByIds(ids);
        }


    private void validateExternalLinkRoleExists(Long id) {
        if (externalLinkRoleMapper.selectById(id) == null) {
            throw exception(EXTERNAL_LINK_ROLE_NOT_EXISTS);
        }
    }

    @Override
    public ExternalLinkRoleDO getExternalLinkRole(Long id) {
        return externalLinkRoleMapper.selectById(id);
    }

    @Override
    public PageResult<ExternalLinkRoleDO> getExternalLinkRolePage(ExternalLinkRolePageReqVO pageReqVO) {
        return externalLinkRoleMapper.selectPage(pageReqVO);
    }

    // ========== 角色-外链的相关方法  ==========

    @Override
    public Set<Long> getLinkIdsByRoleId(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptySet();
        }

        // 如果是管理员的情况下，获取全部外链编号。此处与菜单权限的处理逻辑保持一致，保证超管可以看到全部外链。
        if (roleService.hasAnySuperAdmin(roleIds)) {
            return convertSet(externalLinkMapper.selectList(), ExternalLinkDO::getId);
        }
        // 如果是非管理员的情况下，获得拥有的外链编号。
        return convertSet(externalLinkRoleMapper.selectListByRoleIds(roleIds), ExternalLinkRoleDO::getLinkId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoleExternalLink(Long roleId, Set<Long> linkIds) {
        // 获得角色拥有外链编号。此处与菜单权限的分配逻辑保持一致，采用全量提交、后端计算增删差异的方式。
        Set<Long> dbLinkIds = getLinkIdsByRoleId(singleton(roleId));
        // 计算新增和删除的外链编号。
        Set<Long> linkIdList = CollUtil.emptyIfNull(linkIds);
        Collection<Long> createLinkIds = CollUtil.subtract(linkIdList, dbLinkIds);
        Collection<Long> deleteLinkIds = CollUtil.subtract(dbLinkIds, linkIdList);
        // 执行新增和删除。对于已经授权的外链，不用做任何处理。
        if (CollUtil.isNotEmpty(createLinkIds)) {
            externalLinkRoleMapper.insertBatch(convertList(createLinkIds, linkId -> {
                ExternalLinkRoleDO entity = new ExternalLinkRoleDO();
                entity.setRoleId(roleId);
                entity.setLinkId(linkId);
                return entity;
            }));
        }
        if (CollUtil.isNotEmpty(deleteLinkIds)) {
            externalLinkRoleMapper.physicalDeleteByRoleIdAndLinkIds(roleId, deleteLinkIds);
        }
    }

    @Override
    public void processRoleDeleted(Long roleId) {
        externalLinkRoleMapper.physicalDeleteByRoleId(roleId);
    }

    @Override
    public void processLinkDeleted(Long linkId) {
        externalLinkRoleMapper.physicalDeleteByLinkId(linkId);
    }

    @Override
    public Set<Long> getAllLinkMenuIds() {
        // 提取全部外链关联的菜单编号，过滤未添加到菜单的外链。
        return externalLinkMapper.selectList().stream()
                .map(ExternalLinkDO::getMenuId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Long> getLinkMenuIdsByRoleIds(Collection<Long> roleIds) {
        // 获得角色有权限的外链编号，超管返回全部。
        Set<Long> linkIds = getLinkIdsByRoleId(roleIds);
        if (CollUtil.isEmpty(linkIds)) {
            return Collections.emptySet();
        }
        // 提取这些外链关联的菜单编号，过滤未添加到菜单的外链。
        return externalLinkMapper.selectBatchIds(linkIds).stream()
                .map(ExternalLinkDO::getMenuId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

}