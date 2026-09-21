package cn.iocoder.sva.module.system.service.extlink;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.iocoder.sva.framework.common.enums.CommonStatusEnum;
import cn.iocoder.sva.module.system.controller.admin.extlink.vo.*;
import cn.iocoder.sva.module.system.controller.admin.permission.vo.menu.MenuSaveVO;
import cn.iocoder.sva.module.system.dal.dataobject.extlink.ExternalLinkDO;
import cn.iocoder.sva.module.system.dal.dataobject.extlink.ExternalLinkRoleDO;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.pojo.PageParam;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;

import cn.iocoder.sva.module.system.dal.mysql.extlink.ExternalLinkMapper;
import cn.iocoder.sva.module.system.enums.permission.MenuTypeEnum;
import cn.iocoder.sva.module.system.service.permission.MenuService;
import cn.iocoder.sva.module.system.service.permission.PermissionService;

import static cn.iocoder.sva.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.sva.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.sva.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.sva.framework.common.util.collection.CollectionUtils.diffList;
import static cn.iocoder.sva.module.system.enums.ErrorCodeConstants.*;

/**
 * 系统外链 Service 实现类
 *
 * @author like
 */
@Service
@Validated
public class ExternalLinkServiceImpl implements ExternalLinkService {

    @Resource
    private ExternalLinkMapper externalLinkMapper;

    @Resource
    private ExternalLinkRoleService externalLinkRoleService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private MenuService menuService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createExternalLink(ExternalLinkSaveReqVO createReqVO) {
        // 插入。如果勾选了添加到菜单，先同步创建顶层菜单，再关联保存。
        ExternalLinkDO externalLink = BeanUtils.toBean(createReqVO, ExternalLinkDO.class);
        if (Boolean.TRUE.equals(createReqVO.getAddMenuFlag())) {
            externalLink.setMenuId(menuService.createMenu(buildMenuSaveVO(externalLink)));
        }
        externalLinkMapper.insert(externalLink);

        // 返回
        return externalLink.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateExternalLink(ExternalLinkSaveReqVO updateReqVO) {
        // 校验存在
        ExternalLinkDO oldLink = externalLinkMapper.selectById(updateReqVO.getId());
        if (oldLink == null) {
            throw exception(EXTERNAL_LINK_NOT_EXISTS);
        }
        // 更新
        ExternalLinkDO updateObj = BeanUtils.toBean(updateReqVO, ExternalLinkDO.class);
        // 同步处理菜单关联：勾选时创建/更新菜单，取消勾选时删除关联菜单。
        boolean addMenu = Boolean.TRUE.equals(updateReqVO.getAddMenuFlag());
        if (addMenu) {
            if (oldLink.getMenuId() != null) {
                // 已关联菜单：同步更新菜单的名称、外链地址、图标、排序、状态。
                MenuSaveVO menuSaveVO = buildMenuSaveVO(updateObj);
                menuSaveVO.setId(oldLink.getMenuId());
                menuService.updateMenu(menuSaveVO);
            } else {
                // 未关联菜单：补创建菜单并回写编号。
                updateObj.setMenuId(menuService.createMenu(buildMenuSaveVO(updateObj)));
            }
        } else if (oldLink.getMenuId() != null) {
            // 取消添加到菜单：删除关联的菜单（内部会级联清理角色菜单关联）。
            menuService.deleteMenu(oldLink.getMenuId());
        }
        externalLinkMapper.updateById(updateObj);
        // 取消勾选时清空 menuId。注意：updateById 不更新 null 字段，需用 Wrapper 显式置空。
        if (!addMenu && oldLink.getMenuId() != null) {
            externalLinkMapper.update(null, new LambdaUpdateWrapper<ExternalLinkDO>()
                    .eq(ExternalLinkDO::getId, updateObj.getId())
                    .set(ExternalLinkDO::getMenuId, null));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteExternalLink(Long id) {
        // 校验存在。注意：需在物理删除前查出记录，获取关联的菜单编号。
        ExternalLinkDO externalLink = externalLinkMapper.selectById(id);
        if (externalLink == null) {
            throw exception(EXTERNAL_LINK_NOT_EXISTS);
        }
        // 物理删除（真实删除，不走逻辑删除）
        externalLinkMapper.physicalDeleteById(id);
        // 级联删除外链与角色的关联数据
        externalLinkRoleService.processLinkDeleted(id);
        // 级联删除同步创建的菜单。注意：必须在角色菜单关联被引用前删除，菜单删除内部会清理角色菜单关联。
        if (externalLink.getMenuId() != null) {
            menuService.deleteMenu(externalLink.getMenuId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteExternalLinkListByIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        // 查出待删除的外链，用于后续级联删除同步的菜单。
        List<ExternalLinkDO> links = externalLinkMapper.selectBatchIds(ids);
        // 物理删除（真实删除，不走逻辑删除）
        externalLinkMapper.physicalDeleteByIds(ids);
        // 级联删除外链与角色的关联数据，以及同步创建的菜单。
        ids.forEach(externalLinkRoleService::processLinkDeleted);
        links.forEach(link -> {
            if (link.getMenuId() != null) {
                menuService.deleteMenu(link.getMenuId());
            }
        });
    }


    private void validateExternalLinkExists(Long id) {
        if (externalLinkMapper.selectById(id) == null) {
            throw exception(EXTERNAL_LINK_NOT_EXISTS);
        }
    }

    @Override
    public ExternalLinkDO getExternalLink(Long id) {
        return externalLinkMapper.selectById(id);
    }

    @Override
    public PageResult<ExternalLinkDO> getExternalLinkPage(ExternalLinkPageReqVO pageReqVO) {
        return externalLinkMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ExternalLinkDO> getSimpleExternalLinkList() {
        return externalLinkMapper.selectListByStatus(CommonStatusEnum.ENABLE.getStatus());
    }

    @Override
    public List<ExternalLinkDO> getHomeExternalLinkList(Long userId) {
        // 获得用户拥有的角色编号（包含直接分配和部门继承的角色），通过角色关联间接查询可见资源
        Set<Long> roleIds = permissionService.getLoginUserAllRoleIds(userId);
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        // 获得角色授权的外链编号，超管返回全部外链编号
        Set<Long> linkIds = externalLinkRoleService.getLinkIdsByRoleId(roleIds);
        if (CollUtil.isEmpty(linkIds)) {
            return Collections.emptyList();
        }
        // 过滤开启状态的外链，并按序号升序排序
        return externalLinkMapper.selectListByIdsAndStatus(linkIds, CommonStatusEnum.ENABLE.getStatus());
    }

    @Override
    public void clickExternalLink(Long id) {
        // 校验存在
        validateExternalLinkExists(id);
        // 自增点击次数，使用 SQL 自增避免并发更新丢失。
        externalLinkMapper.incrementClickCount(id);
    }

    /**
     * 根据外链构建同步菜单的保存对象。菜单类型为菜单、路由地址为外链 URL，前端侧边栏点击后直接跳转外部网站。
     *
     * @param link 外链信息
     * @return 菜单保存对象，父菜单为顶层
     */
    private MenuSaveVO buildMenuSaveVO(ExternalLinkDO link) {
        MenuSaveVO menuSaveVO = new MenuSaveVO();
        menuSaveVO.setName(link.getName());
        menuSaveVO.setType(MenuTypeEnum.MENU.getType());
        menuSaveVO.setParentId(0L); // 顶层菜单。
        menuSaveVO.setPath(link.getUrl()); // 路由地址为 http(s) 外链，前端会自动新窗口打开。
        menuSaveVO.setIcon(link.getIcon());
        // 排序从 1000 开始，保证外链菜单排在本系统菜单之后；叠加外链自身排序，保持外链之间的相对顺序。
        menuSaveVO.setSort(1000 + (link.getSort() != null ? link.getSort() : 0));
        menuSaveVO.setStatus(link.getStatus());
        menuSaveVO.setVisible(true);
        menuSaveVO.setKeepAlive(false);
        menuSaveVO.setAlwaysShow(true);
        return menuSaveVO;
    }

}