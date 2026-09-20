package cn.iocoder.sva.module.ai.service.translation;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.sva.framework.common.biz.system.permission.PermissionCommonApi;
import cn.iocoder.sva.framework.common.pojo.CommonResult;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;
import cn.iocoder.sva.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranGlossaryPageReqVO;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranGlossarySaveReqVO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranGlossaryDO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranGlossaryItemDO;
import cn.iocoder.sva.module.ai.dal.mysql.translation.TranGlossaryItemMapper;
import cn.iocoder.sva.module.ai.dal.mysql.translation.TranGlossaryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Set;

import static cn.iocoder.sva.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.sva.module.ai.enums.ErrorCodeConstants.TRAN_GLOSSARY_NOT_EXISTS;
import static cn.iocoder.sva.module.ai.enums.ErrorCodeConstants.TRAN_GLOSSARY_NO_PERMISSION;

/**
 * 术语库管理 Service 实现类
 *
 * @author like
 */
@Slf4j
@Service
@Validated
public class TranGlossaryServiceImpl implements TranGlossaryService {

    private static final Long SUPER_ADMIN_ROLE_ID = 1L;

    @Resource
    private TranGlossaryMapper tranGlossaryMapper;
    
    @Resource
    private TranGlossaryItemMapper tranGlossaryItemMapper;
    
    @Resource
    private PermissionCommonApi permissionCommonApi;

    @Override
    public Long createTranGlossary(TranGlossarySaveReqVO createReqVO) {
        String loginUsername = SecurityFrameworkUtils.getLoginUserUsername();
        
        TranGlossaryDO tranGlossary = BeanUtils.toBean(createReqVO, TranGlossaryDO.class);
        
        if (StrUtil.isBlank(tranGlossary.getUsername())) {
            tranGlossary.setUsername(loginUsername);
        }

        tranGlossaryMapper.insert(tranGlossary);

        return tranGlossary.getId();
    }

    @Override
    public void updateTranGlossary(TranGlossarySaveReqVO updateReqVO) {
        validateTranGlossaryExists(updateReqVO.getId());
        
        validateTranGlossaryPermission(updateReqVO.getId());
        
        TranGlossaryDO updateObj = BeanUtils.toBean(updateReqVO, TranGlossaryDO.class);
        tranGlossaryMapper.updateById(updateObj);
    }

    @Override
    public void deleteTranGlossary(Long id) {
        validateTranGlossaryExists(id);
        
        validateTranGlossaryPermission(id);
        
        tranGlossaryItemMapper.delete(
            new LambdaQueryWrapper<TranGlossaryItemDO>()
                .eq(TranGlossaryItemDO::getGlossaryId, id)
        );
        
        tranGlossaryMapper.deleteById(id);
    }

    @Override
        public void deleteTranGlossaryListByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        
        for (Long glossaryId : ids) {
            validateTranGlossaryPermission(glossaryId);
            
            tranGlossaryItemMapper.delete(
                new LambdaQueryWrapper<TranGlossaryItemDO>()
                    .eq(TranGlossaryItemDO::getGlossaryId, glossaryId)
            );
        }
        
        tranGlossaryMapper.deleteByIds(ids);
        }


    private void validateTranGlossaryExists(Long id) {
        if (tranGlossaryMapper.selectById(id) == null) {
            throw exception(TRAN_GLOSSARY_NOT_EXISTS);
        }
    }
    
    private void validateTranGlossaryPermission(Long glossaryId) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String loginUsername = SecurityFrameworkUtils.getLoginUserUsername();
        
        if (loginUserId == null) {
            log.warn("[validateTranGlossaryPermission] 用户未登录");
            throw exception(TRAN_GLOSSARY_NO_PERMISSION);
        }
        
        // 获取用户的所有角色ID（包含直接分配和部门角色）
        CommonResult<Set<Long>> roleIdsResult = permissionCommonApi.getLoginUserAllRoleIds(loginUserId);
        Set<Long> userRoleIds = roleIdsResult.getCheckedData();
        
        if (CollUtil.isNotEmpty(userRoleIds) && userRoleIds.contains(SUPER_ADMIN_ROLE_ID)) {
            log.info("[validateTranGlossaryPermission] 超级管理员，允许操作, userId={}, glossaryId={}", 
                    loginUserId, glossaryId);
            return;
        }
        
        TranGlossaryDO glossary = tranGlossaryMapper.selectById(glossaryId);
        if (glossary == null) {
            throw exception(TRAN_GLOSSARY_NOT_EXISTS);
        }
        
        if (CollUtil.isNotEmpty(userRoleIds) && glossary.getRoleId() != null 
                && userRoleIds.contains(glossary.getRoleId())) {
            log.info("[validateTranGlossaryPermission] 匹配所属角色，允许操作, userId={}, glossaryId={}, roleId={}", 
                    loginUserId, glossaryId, glossary.getRoleId());
            return;
        }
        
        if (StrUtil.isNotBlank(glossary.getUsername()) && StrUtil.isNotBlank(loginUsername) 
                && glossary.getUsername().equals(loginUsername)) {
            log.info("[validateTranGlossaryPermission] 匹配所属人员，允许操作, userId={}, glossaryId={}, username={}", 
                    loginUserId, glossaryId, loginUsername);
            return;
        }
        
        log.warn("[validateTranGlossaryPermission] 无权限操作, userId={}, username={}, glossaryId={}, roleId={}, owner={}", 
                loginUserId, loginUsername, glossaryId, glossary.getRoleId(), glossary.getUsername());
        throw exception(TRAN_GLOSSARY_NO_PERMISSION);
    }

    @Override
    public TranGlossaryDO getTranGlossary(Long id) {
        return tranGlossaryMapper.selectById(id);
    }

    @Override
    public PageResult<TranGlossaryDO> getTranGlossaryPage(TranGlossaryPageReqVO pageReqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String loginUsername = SecurityFrameworkUtils.getLoginUserUsername();
        return getTranGlossaryPage(pageReqVO, loginUserId, loginUsername);
    }

    @Override
    public PageResult<TranGlossaryDO> getTranGlossaryPage(TranGlossaryPageReqVO pageReqVO, Long userId) {
        return getTranGlossaryPage(pageReqVO, userId, null);
    }

    @Override
    public PageResult<TranGlossaryDO> getTranGlossaryPage(TranGlossaryPageReqVO pageReqVO, Long userId, String username) {
        if (userId == null) {
            log.warn("[getTranGlossaryPage] 用户ID为空，拒绝查询");
            return new PageResult<>(CollUtil.newArrayList(), 0L);
        }
        
        // 获取用户的所有角色ID（包含直接分配和部门角色）
        CommonResult<Set<Long>> roleIdsResult = permissionCommonApi.getLoginUserAllRoleIds(userId);
        Set<Long> userRoleIds = roleIdsResult.getCheckedData();
        
        // 超级管理员可以查看所有术语库
        if (CollUtil.isNotEmpty(userRoleIds) && userRoleIds.contains(SUPER_ADMIN_ROLE_ID)) {
            log.info("[getTranGlossaryPage] 超级管理员查询所有术语库, userId={}", userId);
            return tranGlossaryMapper.selectPage(pageReqVO);
        }
        
        pageReqVO.setRoleId(null);
        
        LambdaQueryWrapper<TranGlossaryDO> queryWrapper = new LambdaQueryWrapper<>();
        
        // 构建查询条件：角色相关 + 可见角色 + 用户自建
        queryWrapper.and(wrapper -> {
            // 条件1：术语库的所属角色在用户角色列表中
            if (CollUtil.isNotEmpty(userRoleIds)) {
                wrapper.in(TranGlossaryDO::getRoleId, userRoleIds);
            }
            
            // 条件2：术语库的可见角色在用户角色列表中
            if (CollUtil.isNotEmpty(userRoleIds)) {
                wrapper.or().in(TranGlossaryDO::getRoleShow, userRoleIds);
            }
            
            // 条件3：用户自己创建的术语库（即使没有角色也能查看）
            if (StrUtil.isNotBlank(username)) {
                wrapper.or().eq(TranGlossaryDO::getUsername, username);
            }
            
            if (CollUtil.isNotEmpty(userRoleIds)) {
                log.info("[getTranGlossaryPage] 普通用户查询术语库，包含角色相关、可见角色和用户自建, userId={}, username={}, roleIds={}", 
                        userId, username, userRoleIds);
            } else {
                log.info("[getTranGlossaryPage] 无角色用户查询术语库（仅用户自建）, userId={}, username={}", 
                        userId, username);
            }
        });
        
        queryWrapper.like(pageReqVO.getGlossaryName() != null, TranGlossaryDO::getGlossaryName, pageReqVO.getGlossaryName());
        queryWrapper.eq(pageReqVO.getSourceLanguage() != null, TranGlossaryDO::getSourceLanguage, pageReqVO.getSourceLanguage());
        queryWrapper.eq(pageReqVO.getTargetLanguage() != null, TranGlossaryDO::getTargetLanguage, pageReqVO.getTargetLanguage());
        queryWrapper.eq(pageReqVO.getLanguageDirection() != null, TranGlossaryDO::getLanguageDirection, pageReqVO.getLanguageDirection());
        queryWrapper.eq(pageReqVO.getItemCount() != null, TranGlossaryDO::getItemCount, pageReqVO.getItemCount());
        queryWrapper.eq(pageReqVO.getUsername() != null, TranGlossaryDO::getUsername, pageReqVO.getUsername());
        queryWrapper.eq(pageReqVO.getDeptId() != null, TranGlossaryDO::getDeptId, pageReqVO.getDeptId());
        queryWrapper.eq(pageReqVO.getIsEnabled() != null, TranGlossaryDO::getIsEnabled, pageReqVO.getIsEnabled());
        queryWrapper.orderByDesc(TranGlossaryDO::getId);
        
        return tranGlossaryMapper.selectPage(pageReqVO, queryWrapper);
    }

}