package cn.iocoder.sva.module.ai.service.translation;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.sva.framework.common.biz.system.permission.PermissionCommonApi;
import cn.iocoder.sva.framework.common.pojo.CommonResult;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;
import cn.iocoder.sva.framework.excel.core.util.ExcelUtils;
import cn.iocoder.sva.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.sva.module.ai.controller.admin.file.vo.file.FileUploadReqVO;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.GlossaryItemImportVO;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranGlossaryItemPageReqVO;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranGlossaryItemSaveReqVO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranGlossaryDO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranGlossaryItemDO;
import cn.iocoder.sva.module.ai.dal.mysql.translation.TranGlossaryItemMapper;
import cn.iocoder.sva.module.ai.dal.mysql.translation.TranGlossaryMapper;
import cn.iocoder.sva.module.ai.service.translation.tran.LlmClientService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.sva.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.sva.module.ai.enums.ErrorCodeConstants.TRAN_GLOSSARY_ITEM_NOT_EXISTS;
import static cn.iocoder.sva.module.ai.enums.ErrorCodeConstants.TRAN_GLOSSARY_NO_PERMISSION;

@Slf4j
@Service
@Validated
public class TranGlossaryItemServiceImpl implements TranGlossaryItemService {

    private static final Long SUPER_ADMIN_ROLE_ID = 1L;

    @Resource
    private TranGlossaryItemMapper tranGlossaryItemMapper;

    @Resource
    private TranGlossaryMapper tranGlossaryMapper;

    @Resource
    private LlmClientService llmClientService;

    @Resource
    private PermissionCommonApi permissionCommonApi;

    @Override
    public Long createTranGlossaryItem(TranGlossaryItemSaveReqVO createReqVO) {
        validateGlossaryPermission(createReqVO.getGlossaryId());

        TranGlossaryItemDO tranGlossaryItem = BeanUtils.toBean(createReqVO, TranGlossaryItemDO.class);
        tranGlossaryItemMapper.insert(tranGlossaryItem);

        updateGlossaryItemCount(tranGlossaryItem.getGlossaryId());

        llmClientService.clearCache();
        log.info("[createTranGlossaryItem] 新增术语，已清空 LLM Redis 缓存");

        return tranGlossaryItem.getId();
    }

    @Override
    public void updateTranGlossaryItem(TranGlossaryItemSaveReqVO updateReqVO) {
        validateTranGlossaryItemExists(updateReqVO.getId());

        TranGlossaryItemDO item = tranGlossaryItemMapper.selectById(updateReqVO.getId());
        if (item != null) {
            validateGlossaryPermission(item.getGlossaryId());
        }

        TranGlossaryItemDO updateObj = BeanUtils.toBean(updateReqVO, TranGlossaryItemDO.class);
        tranGlossaryItemMapper.updateById(updateObj);

        llmClientService.clearCache();
        log.info("[updateTranGlossaryItem] 更新术语，已清空 LLM Redis 缓存, id={}", updateReqVO.getId());
    }

    @Override
    public void deleteTranGlossaryItem(Long id) {
        TranGlossaryItemDO item = tranGlossaryItemMapper.selectById(id);
        validateTranGlossaryItemExists(id);

        if (item != null) {
            validateGlossaryPermission(item.getGlossaryId());
        }

        tranGlossaryItemMapper.deleteById(id);

        if (item != null) {
            updateGlossaryItemCount(item.getGlossaryId());
        }

        llmClientService.clearCache();
        log.info("[deleteTranGlossaryItem] 删除术语，已清空 LLM Redis 缓存, id={}", id);
    }

    @Override
    public void deleteTranGlossaryItemListByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<TranGlossaryItemDO> items = tranGlossaryItemMapper.selectBatchIds(ids);

        for (TranGlossaryItemDO item : items) {
            validateGlossaryPermission(item.getGlossaryId());
        }

        tranGlossaryItemMapper.deleteByIds(ids);

        items.stream()
                .map(TranGlossaryItemDO::getGlossaryId)
                .distinct()
                .forEach(this::updateGlossaryItemCount);

        llmClientService.clearCache();
        log.info("[deleteTranGlossaryItemListByIds] 批量删除术语，已清空 LLM Redis 缓存, count={}", ids.size());
    }


    private void validateTranGlossaryItemExists(Long id) {
        if (tranGlossaryItemMapper.selectById(id) == null) {
            throw exception(TRAN_GLOSSARY_ITEM_NOT_EXISTS);
        }
    }

    private void validateGlossaryPermission(Long glossaryId) {
        if (glossaryId == null) {
            log.warn("[validateGlossaryPermission] 术语库ID为空");
            throw exception(TRAN_GLOSSARY_NO_PERMISSION);
        }

        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String loginUsername = SecurityFrameworkUtils.getLoginUserUsername();

        if (loginUserId == null) {
            log.warn("[validateGlossaryPermission] 用户未登录");
            throw exception(TRAN_GLOSSARY_NO_PERMISSION);
        }

        // 获取用户的所有角色ID（包含直接分配和部门角色）
        CommonResult<Set<Long>> roleIdsResult = permissionCommonApi.getLoginUserAllRoleIds(loginUserId);
        Set<Long> userRoleIds = roleIdsResult.getCheckedData();

        if (CollUtil.isNotEmpty(userRoleIds) && userRoleIds.contains(SUPER_ADMIN_ROLE_ID)) {
            log.info("[validateGlossaryPermission] 超级管理员，允许操作, userId={}, glossaryId={}",
                    loginUserId, glossaryId);
            return;
        }

        TranGlossaryDO glossary = tranGlossaryMapper.selectById(glossaryId);
        if (glossary == null) {
            log.warn("[validateGlossaryPermission] 术语库不存在, glossaryId={}", glossaryId);
            throw exception(TRAN_GLOSSARY_ITEM_NOT_EXISTS);
        }

        if (CollUtil.isNotEmpty(userRoleIds) && glossary.getRoleId() != null
                && userRoleIds.contains(glossary.getRoleId())) {
            log.info("[validateGlossaryPermission] 匹配所属角色，允许操作, userId={}, glossaryId={}, roleId={}",
                    loginUserId, glossaryId, glossary.getRoleId());
            return;
        }

        if (StrUtil.isNotBlank(glossary.getUsername()) && StrUtil.isNotBlank(loginUsername)
                && glossary.getUsername().equals(loginUsername)) {
            log.info("[validateGlossaryPermission] 匹配所属人员，允许操作, userId={}, glossaryId={}, username={}",
                    loginUserId, glossaryId, loginUsername);
            return;
        }

        log.warn("[validateGlossaryPermission] 无权限操作, userId={}, username={}, glossaryId={}, roleId={}, owner={}",
                loginUserId, loginUsername, glossaryId, glossary.getRoleId(), glossary.getUsername());
        throw exception(TRAN_GLOSSARY_NO_PERMISSION);
    }

    private void updateGlossaryItemCount(Long glossaryId) {
        if (glossaryId == null) {
            return;
        }

        Long count = tranGlossaryItemMapper.selectCount(
            new LambdaQueryWrapper<TranGlossaryItemDO>()
                .eq(TranGlossaryItemDO::getGlossaryId, glossaryId)
        );

        TranGlossaryDO glossary = new TranGlossaryDO();
        glossary.setId(glossaryId);
        glossary.setItemCount(count.intValue());
        tranGlossaryMapper.updateById(glossary);
    }

    @Override
    public TranGlossaryItemDO getTranGlossaryItem(Long id) {
        return tranGlossaryItemMapper.selectById(id);
    }

    @Override
    public PageResult<TranGlossaryItemDO> getTranGlossaryItemPage(TranGlossaryItemPageReqVO pageReqVO) {
        return tranGlossaryItemMapper.selectPage(pageReqVO);
    }

    @Override
    public List<TranGlossaryItemDO> getTranGlossaryItemListByGlossaryId(Long glossaryId) {
        return tranGlossaryItemMapper.selectListByGlossaryId(glossaryId);
    }

    @Override
    public String uploadFile(FileUploadReqVO uploadReqVO) {
        validateGlossaryPermission(uploadReqVO.getParentId());

        MultipartFile file = uploadReqVO.getFile();
        log.info("========== [uploadFile] 开始处理上传文件 ==========");
        log.info("术语库ID: {}", uploadReqVO.getParentId());
        log.info("文件名: {}", file.getOriginalFilename());
        log.info("文件大小: {} bytes ({} KB)", file.getSize(), file.getSize() / 1024.0);
        log.info("Content-Type: {}", file.getContentType());
        log.info("文件是否为空: {}", file.isEmpty());

        List<GlossaryItemImportVO> read = new ArrayList<>();
        try {
            log.info("[uploadFile] 准备调用ExcelUtils.read解析文件");
            read = ExcelUtils.read(file, GlossaryItemImportVO.class);
            log.info("[uploadFile] 成功读取Excel数据, 记录数={}", read.size());

            // 打印前几条数据用于验证
            if (!read.isEmpty()) {
                int sampleSize = Math.min(3, read.size());
                log.info("[uploadFile] 前{}条数据示例:", sampleSize);
                for (int i = 0; i < sampleSize; i++) {
                    GlossaryItemImportVO item = read.get(i);
                    log.info("  [{}] source='{}', target='{}'", i+1, item.getSource(), item.getTarget());
                }
            }
        } catch (Exception e) {
            // 使用INFO级别打印错误信息，确保测试环境可见
            log.info("========== [uploadFile] 读取Excel文件失败 ==========");
            log.info("文件名: {}", file.getOriginalFilename());
            log.info("文件大小: {} bytes", file.getSize());
            log.info("错误类型: {}", e.getClass().getName());
            log.info("错误消息: {}", e.getMessage());

            // 如果是ServiceException（如加密文件），直接抛出，让全局异常处理器处理
            if (e instanceof cn.iocoder.sva.framework.common.exception.ServiceException) {
                throw (cn.iocoder.sva.framework.common.exception.ServiceException) e;
            }

            log.info("错误原因: {}", e.getCause() != null ? e.getCause().getMessage() : "无");
            log.info("详细堆栈信息: ", e);
            // 抛出异常，让全局异常处理器处理
            throw new IllegalArgumentException("文件格式错误：请检查CSV文件格式是否正确，确保没有特殊字符或格式问题。建议重新下载模板填写后上传。");
        }

        if (read == null || read.isEmpty()) {
            log.info("[uploadFile] 上传文件为空");
            throw new IllegalArgumentException("上传文件为空");
        }

        Long glossaryId = uploadReqVO.getParentId();

        List<TranGlossaryItemDO> toInsertList = new ArrayList<>();
        List<TranGlossaryItemDO> toUpdateList = new ArrayList<>();

        List<String> sourceLanguages = read.stream()
                .map(GlossaryItemImportVO::getSource)
                .distinct()
                .collect(Collectors.toList());

        log.info("[uploadFile] 提取源语言列表, count={}, languages={}", sourceLanguages.size(), sourceLanguages);

        List<TranGlossaryItemDO> existingItems = tranGlossaryItemMapper.selectList(
            new LambdaQueryWrapper<TranGlossaryItemDO>()
                .eq(TranGlossaryItemDO::getGlossaryId, glossaryId)
                .in(TranGlossaryItemDO::getSourceLanguage, sourceLanguages)
        );

        log.info("[uploadFile] 查询已存在的术语项, count={}", existingItems.size());

        Map<String, TranGlossaryItemDO> existingMap = existingItems.stream()
                .collect(Collectors.toMap(
                    TranGlossaryItemDO::getSourceLanguage,
                    item -> item,
                    (existing, replacement) -> existing
                ));

        for (GlossaryItemImportVO glossaryItemImportVO : read){
            String sourceLanguage = glossaryItemImportVO.getSource();
            TranGlossaryItemDO existingItem = existingMap.get(sourceLanguage);

            if (existingItem != null) {
                existingItem.setTargetLanguage(glossaryItemImportVO.getTarget());
                toUpdateList.add(existingItem);
            } else {
                TranGlossaryItemDO newItem = new TranGlossaryItemDO();
                newItem.setGlossaryId(glossaryId);
                newItem.setSourceLanguage(sourceLanguage);
                newItem.setTargetLanguage(glossaryItemImportVO.getTarget());
                toInsertList.add(newItem);
            }
        }

        log.info("[uploadFile] 数据处理完成, 新增={}, 更新={}", toInsertList.size(), toUpdateList.size());

        if (!toInsertList.isEmpty()) {
            log.info("[uploadFile] 开始批量插入, count={}", toInsertList.size());
            tranGlossaryItemMapper.insertBatch(toInsertList);
        }

        if (!toUpdateList.isEmpty()) {
            log.info("[uploadFile] 开始批量更新, count={}", toUpdateList.size());
            tranGlossaryItemMapper.updateBatch(toUpdateList);
        }

        updateGlossaryItemCount(glossaryId);

        llmClientService.clearCache();
        log.info("[uploadFile] 导入术语完成，已清空 LLM Redis 缓存, 新增={}, 更新={}",
                toInsertList.size(), toUpdateList.size());
        log.info("========== [uploadFile] 处理完成 ==========");

        return String.format("新增%d条，更新%d条", toInsertList.size(), toUpdateList.size());
    }

}