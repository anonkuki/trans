package cn.iocoder.sva.module.ai.service.translation.helper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.sva.framework.common.biz.system.permission.PermissionCommonApi;
import cn.iocoder.sva.framework.common.pojo.CommonResult;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranGlossaryItemSaveReqVO;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranGlossarySaveReqVO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranGlossaryDO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranGlossaryItemDO;
import cn.iocoder.sva.module.ai.dal.mysql.translation.TranGlossaryMapper;
import cn.iocoder.sva.module.ai.service.translation.TranGlossaryItemService;
import cn.iocoder.sva.module.ai.service.translation.TranGlossaryService;
import cn.iocoder.sva.framework.security.core.util.SecurityFrameworkUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GlossaryHelper {

    @Autowired
    private TranGlossaryService tranGlossaryService;

    @Autowired
    private TranGlossaryItemService tranGlossaryItemService;

    @Autowired
    private TranGlossaryMapper tranGlossaryMapper;

    @Autowired
    private PermissionCommonApi permissionApi;

    /**
     * 根据ID查询术语库
     *
     * @param glossaryId 术语库ID
     * @return 术语库对象，不存在则返回null
     */
    public TranGlossaryDO getGlossaryById(Long glossaryId) {
        if (glossaryId == null) {
            return null;
        }
        return tranGlossaryMapper.selectById(glossaryId);
    }

    /**
     * 校验用户是否有权限操作术语库
     * <p>
     * 权限校验规则（满足其一即可）：
     * 1. 用户具备的角色包含该术语库的所属角色（roleId）
     * 2. 用户是该术语库的所属人员（username）
     *
     * @param glossaryId 术语库ID
     * @param username 当前登录用户名
     * @return true=有权限，false=无权限
     */
    public boolean checkGlossaryPermission(Long glossaryId, String username) {
        if (glossaryId == null) {
            log.warn("[checkGlossaryPermission] 术语库ID为空");
            return false;
        }

        // 查询术语库信息
        TranGlossaryDO glossary = tranGlossaryMapper.selectById(glossaryId);
        if (glossary == null) {
            log.warn("[checkGlossaryPermission] 术语库不存在: glossaryId={}", glossaryId);
            return false;
        }

        // 获取当前登录用户ID
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            log.warn("[checkGlossaryPermission] 用户未登录");
            return false;
        }

        // 通过API获取用户的角色列表
        CommonResult<Set<Long>> roleIdsResult = permissionApi.getLoginUserAllRoleIds(userId);
        Set<Long> userRoleIds = roleIdsResult.getCheckedData();
        if (userRoleIds == null) {
            userRoleIds = new HashSet<>();
        }

        // 规则1：检查用户角色是否包含术语库的所属角色
        if (CollUtil.isNotEmpty(userRoleIds) && glossary.getRoleId() != null) {
            if (userRoleIds.contains(glossary.getRoleId())) {
                log.info("[checkGlossaryPermission] 匹配所属角色，允许操作: userId={}, glossaryId={}, roleId={}", 
                        userId, glossaryId, glossary.getRoleId());
                return true;
            }
        }

        // 规则2：检查用户是否是术语库的所属人员
        if (StrUtil.isNotBlank(glossary.getUsername()) &&
                StrUtil.isNotBlank(username)) {
            if (glossary.getUsername().equals(username)) {
                log.info("[checkGlossaryPermission] 匹配所属人员，允许操作: userId={}, glossaryId={}, username={}", 
                        userId, glossaryId, username);
                return true;
            }
        }

        // 两者都不满足，无权限
        log.warn("[checkGlossaryPermission] 无权限操作: userId={}, username={}, glossaryId={}, roleId={}, owner={}", 
                userId, username, glossaryId, glossary.getRoleId(), glossary.getUsername());
        return false;
    }

    public void ensureUserGlossaryExists(String username, String targetLanguage) {
        LambdaQueryWrapper<TranGlossaryDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TranGlossaryDO::getUsername, username)
                    .eq(TranGlossaryDO::getTargetLanguage, targetLanguage)
                    .last("LIMIT 1");

        TranGlossaryDO existingGlossary = tranGlossaryMapper.selectOne(queryWrapper);

        if (existingGlossary != null) {
            log.debug("[ensureUserGlossaryExists] 术语库已存在, username={}, targetLanguage={}, glossaryId={}",
                    username, targetLanguage, existingGlossary.getId());
            return;
        }

        String sourceLanguage;
        String languageDirection;

        if ("en".equalsIgnoreCase(targetLanguage)) {
            sourceLanguage = "cn";
            languageDirection = "中文->英文";
        } else {
            sourceLanguage = "en";
            languageDirection = "英文->中文";
        }

        TranGlossarySaveReqVO createReqVO = new TranGlossarySaveReqVO();
        createReqVO.setGlossaryName(username + "的术语库");
        createReqVO.setSourceLanguage(sourceLanguage);
        createReqVO.setTargetLanguage(targetLanguage);
        createReqVO.setLanguageDirection(languageDirection);
        createReqVO.setItemCount(0);
        createReqVO.setUsername(username);
        createReqVO.setIsEnabled(1);
        createReqVO.setRoleId(1L);

        Long glossaryId = tranGlossaryService.createTranGlossary(createReqVO);
        log.info("[ensureUserGlossaryExists] 自动创建用户术语库, username={}, targetLanguage={}, sourceLanguage={}, languageDirection={}, glossaryId={}",
                username, targetLanguage, sourceLanguage, languageDirection, glossaryId);
    }

    public Long getOrCreateUserGlossary(String username, String targetLanguage) {
        LambdaQueryWrapper<TranGlossaryDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TranGlossaryDO::getUsername, username)
                    .eq(TranGlossaryDO::getTargetLanguage, targetLanguage)
                    .last("LIMIT 1");

        TranGlossaryDO existingGlossary = tranGlossaryMapper.selectOne(queryWrapper);

        if (existingGlossary != null) {
            log.info("[getOrCreateUserGlossary] 找到已存在的术语库, username={}, targetLanguage={}, glossaryId={}",
                    username, targetLanguage, existingGlossary.getId());
            return existingGlossary.getId();
        }

        TranGlossarySaveReqVO createReqVO = new TranGlossarySaveReqVO();
        createReqVO.setGlossaryName(username + "的术语库");
        createReqVO.setSourceLanguage("cn");
        createReqVO.setTargetLanguage(targetLanguage);
        createReqVO.setLanguageDirection("cn->" + targetLanguage);
        createReqVO.setItemCount(0);
        createReqVO.setUsername(username);
        createReqVO.setIsEnabled(1);
        createReqVO.setRoleId(1L);

        Long glossaryId = tranGlossaryService.createTranGlossary(createReqVO);
        log.info("[getOrCreateUserGlossary] 创建新术语库, username={}, targetLanguage={}, glossaryId={}",
                username, targetLanguage, glossaryId);

        return glossaryId;
    }

    public List<TranGlossaryDO> getVisibleGlossaryList(Long userId, String username, String targetLanguage) {
        if (userId == null || username == null) {
            log.warn("[getVisibleGlossaryList] 用户未登录");
            return new ArrayList<>();
        }

        if (targetLanguage == null || targetLanguage.trim().isEmpty()) {
            log.warn("[getVisibleGlossaryList] 目标语言不能为空");
            return new ArrayList<>();
        }

        try {
            // 注释掉自动创建术语库的逻辑，避免新用户看到空术语库
            // ensureUserGlossaryExists(username, targetLanguage);

            // 通过API获取用户的角色列表
            CommonResult<Set<Long>> roleIdsResult = permissionApi.getLoginUserAllRoleIds(userId);
            Set<Long> userRoleIds = roleIdsResult.getCheckedData();
            if (userRoleIds == null) {
                userRoleIds = new HashSet<>();
            }

            final Set<Long> finalRoleIds = userRoleIds;
            final String finalTargetLanguage = targetLanguage;

            boolean isSuperAdmin = finalRoleIds.contains(1L);

            LambdaQueryWrapper<TranGlossaryDO> queryWrapper = new LambdaQueryWrapper<>();

            queryWrapper.eq(TranGlossaryDO::getTargetLanguage, finalTargetLanguage);

            if (!isSuperAdmin) {
                if (CollUtil.isNotEmpty(finalRoleIds)) {
                    queryWrapper.and(wrapper -> wrapper
                        .in(TranGlossaryDO::getRoleShow, finalRoleIds)
                        .or()
                        .eq(TranGlossaryDO::getUsername, username)
                    );
                } else {
                    queryWrapper.eq(TranGlossaryDO::getUsername, username);
                }
            }

            queryWrapper.eq(TranGlossaryDO::getIsEnabled, 1)
                       .orderByDesc(TranGlossaryDO::getCreateTime);

            List<TranGlossaryDO> glossaryList = tranGlossaryMapper.selectList(queryWrapper);

            log.info("[getVisibleGlossaryList] 查询到 {} 个可见术语库, userId={}, username={}, targetLanguage={}, isSuperAdmin={}, roleIds={}",
                    glossaryList.size(), userId, username, finalTargetLanguage, isSuperAdmin, finalRoleIds);

            return glossaryList;

        } catch (Exception e) {
            log.error("[getVisibleGlossaryList] 查询可见术语库失败, userId={}, username={}, targetLanguage={}",
                    userId, username, targetLanguage, e);
            return new ArrayList<>();
        }
    }

    public int saveTermsToGlossary(Long glossaryId, Map<String, String> terms) {
        if (terms == null || terms.isEmpty()) {
            return 0;
        }

        List<String> sourceLanguages = terms.keySet().stream()
                .filter(key -> key != null && !key.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());

        if (sourceLanguages.isEmpty()) {
            return 0;
        }

        List<TranGlossaryItemDO> existingItems = tranGlossaryItemService.getTranGlossaryItemListByGlossaryId(glossaryId);

        Map<String, TranGlossaryItemDO> existingMap = existingItems.stream()
                .filter(item -> item.getSourceLanguage() != null)
                .collect(Collectors.toMap(
                    TranGlossaryItemDO::getSourceLanguage,
                    item -> item,
                    (existing, replacement) -> existing
                ));

        List<TranGlossaryItemSaveReqVO> toInsertList = new ArrayList<>();
        List<TranGlossaryItemSaveReqVO> toUpdateList = new ArrayList<>();

        for (Map.Entry<String, String> entry : terms.entrySet()) {
            String sourceLanguage = entry.getKey();
            String targetLanguage = entry.getValue();

            if (sourceLanguage == null || sourceLanguage.trim().isEmpty() ||
                targetLanguage == null || targetLanguage.trim().isEmpty()) {
                continue;
            }

            String trimmedSource = sourceLanguage.trim();
            String trimmedTarget = targetLanguage.trim();

            TranGlossaryItemDO existingItem = existingMap.get(trimmedSource);

            if (existingItem != null) {
                TranGlossaryItemSaveReqVO updateReqVO = new TranGlossaryItemSaveReqVO();
                updateReqVO.setId(existingItem.getId());
                updateReqVO.setGlossaryId(glossaryId);
                updateReqVO.setSourceLanguage(trimmedSource);
                updateReqVO.setTargetLanguage(trimmedTarget);
                toUpdateList.add(updateReqVO);
            } else {
                TranGlossaryItemSaveReqVO insertReqVO = new TranGlossaryItemSaveReqVO();
                insertReqVO.setGlossaryId(glossaryId);
                insertReqVO.setSourceLanguage(trimmedSource);
                insertReqVO.setTargetLanguage(trimmedTarget);
                toInsertList.add(insertReqVO);
            }
        }

        int insertCount = 0;
        for (TranGlossaryItemSaveReqVO insertReqVO : toInsertList) {
            tranGlossaryItemService.createTranGlossaryItem(insertReqVO);
            insertCount++;
        }

        int updateCount = 0;
        for (TranGlossaryItemSaveReqVO updateReqVO : toUpdateList) {
            tranGlossaryItemService.updateTranGlossaryItem(updateReqVO);
            updateCount++;
        }

        int totalCount = insertCount + updateCount;
        log.info("[saveTermsToGlossary] 成功保存 {} 条术语到术语库 (新增: {}, 更新: {}), glossaryId={}",
                totalCount, insertCount, updateCount, glossaryId);
        return totalCount;
    }

    public Map<String, String> buildGlossaryMap(List<TranGlossaryItemDO> glossaryItems) {
        if (glossaryItems == null || glossaryItems.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, String> glossaryMap = new HashMap<>();
        for (TranGlossaryItemDO item : glossaryItems) {
            glossaryMap.put(item.getSourceLanguage(), item.getTargetLanguage());
        }

        log.info("[buildGlossaryMap] 构建术语映射表，共 {} 条术语", glossaryMap.size());

        return glossaryMap;
    }
}