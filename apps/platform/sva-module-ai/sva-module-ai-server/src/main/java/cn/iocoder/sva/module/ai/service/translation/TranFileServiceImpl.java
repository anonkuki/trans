package cn.iocoder.sva.module.ai.service.translation;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.sva.framework.common.biz.system.permission.PermissionCommonApi;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;
import cn.iocoder.sva.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranFilePageReqVO;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranFileSaveReqVO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranFileDO;
import cn.iocoder.sva.module.ai.dal.mysql.translation.TranFileMapper;
import cn.iocoder.sva.module.ai.service.file.FileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Set;

import static cn.iocoder.sva.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.sva.module.ai.enums.ErrorCodeConstants.TRAN_FILE_NOT_EXISTS;

/**
 * AI翻译文件信息 Service 实现类
 *
 * @author like
 */
@Service
@Validated
@Slf4j
public class TranFileServiceImpl implements TranFileService {

    @Resource
    private TranFileMapper tranFileMapper;

    @Resource
    private FileService fileService;

    @Resource
    private PermissionCommonApi permissionApi;

    @Override
    public Long createTranFile(TranFileSaveReqVO createReqVO) {
        TranFileDO tranFile = BeanUtils.toBean(createReqVO, TranFileDO.class);
        tranFileMapper.insert(tranFile);
        return tranFile.getId();
    }

    @Override
    public Long createTranFile(TranFileDO tranFileDO) {
        tranFileMapper.insert(tranFileDO);
        return tranFileDO.getId();
    }

    @Override
    public void updateTranFile(TranFileSaveReqVO updateReqVO) {
        validateTranFileExists(updateReqVO.getId());
        TranFileDO updateObj = BeanUtils.toBean(updateReqVO, TranFileDO.class);
        tranFileMapper.updateById(updateObj);
    }

    @Override
    public void updateTranFile(TranFileDO tranFileDO) {
        validateTranFileExists(tranFileDO.getId());
        tranFileMapper.updateById(tranFileDO);
    }

    @Override
    public void deleteTranFile(Long id) {
        validateTranFileExists(id);
        tranFileMapper.deleteById(id);
    }

    @Override
    public void deleteTranFileListByIds(List<Long> ids) {
        tranFileMapper.deleteByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTranFileWithMinioFiles(Long id) throws Exception {
        TranFileDO tranFile = tranFileMapper.selectById(id);
        if (tranFile == null) {
            throw exception(TRAN_FILE_NOT_EXISTS);
        }

        log.info("[deleteTranFileWithMinioFiles][id={}] 开始删除文件记录", id);
        log.info("[deleteTranFileWithMinioFiles][id={}] sourceFileUrl: {}", id, tranFile.getSourceFileUrl());
        log.info("[deleteTranFileWithMinioFiles][id={}] fileUrl: {}", id, tranFile.getFileUrl());
        log.info("[deleteTranFileWithMinioFiles][id={}] compareFileUrl: {}", id, tranFile.getCompareFileUrl());
        log.info("[deleteTranFileWithMinioFiles][id={}] qcFileUrl: {}", id, tranFile.getQcFileUrl());
        log.info("[deleteTranFileWithMinioFiles][id={}] contrastFileUrl: {}", id, tranFile.getContrastFileUrl());

        deleteMinioFiles(tranFile);

        tranFileMapper.deleteById(id);
        log.info("[deleteTranFileWithMinioFiles][id={}] 已删除翻译文件记录及关联的 MinIO 文件", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTranFileListWithMinioFiles(List<Long> ids) throws Exception {
        List<TranFileDO> tranFiles = tranFileMapper.selectByIds(ids);

        for (TranFileDO tranFile : tranFiles) {
            try {
                log.info("[deleteTranFileListWithMinioFiles][id={}] 开始删除文件记录", tranFile.getId());
                deleteMinioFiles(tranFile);
                tranFileMapper.deleteById(tranFile.getId());
                log.info("[deleteTranFileListWithMinioFiles][id={}] 已删除翻译文件记录及关联的 MinIO 文件", tranFile.getId());
            } catch (Exception e) {
                log.error("[deleteTranFileListWithMinioFiles][id={}] 删除失败", tranFile.getId(), e);
                throw e;
            }
        }
    }

    /**
     * 删除翻译文件关联的所有 MinIO 文件
     *
     * @param tranFile 翻译文件记录
     */
    private void deleteMinioFiles(TranFileDO tranFile) {
        log.info("[deleteMinioFiles] 开始删除文件 ID: {} 的关联文件", tranFile.getId());
        deleteMinioFileByUrl(tranFile.getSourceFileUrl(), "原始文件");
        deleteMinioFileByUrl(tranFile.getFileUrl(), "翻译结果文件");
        deleteMinioFileByUrl(tranFile.getCompareFileUrl(), "对照表文件");
        deleteMinioFileByUrl(tranFile.getQcFileUrl(), "QC报告文件");
        deleteMinioFileByUrl(tranFile.getContrastFileUrl(), "双语文件");
        log.info("[deleteMinioFiles] 完成删除文件 ID: {} 的关联文件", tranFile.getId());
    }

    /**
     * 根据 URL 删除 MinIO 文件
     *
     * @param fileUrl 文件 URL
     * @param fileType 文件类型描述（用于日志）
     */
    private void deleteMinioFileByUrl(String fileUrl, String fileType) {
        if (StrUtil.isBlank(fileUrl)) {
            log.debug("[deleteMinioFileByUrl] {} URL 为空，跳过", fileType);
            return;
        }

        try {
            log.info("[deleteMinioFileByUrl] 准备删除 {}: {}", fileType, fileUrl);
            fileService.deleteFileByUrl(fileUrl);
            log.info("[deleteMinioFileByUrl] ✓ 成功删除 {}: {}", fileType, fileUrl);
        } catch (Exception e) {
            log.error("[deleteMinioFileByUrl] ✗ 删除 {} 失败: {}", fileType, fileUrl, e);
        }
    }


    private void validateTranFileExists(Long id) {
        if (tranFileMapper.selectById(id) == null) {
            throw exception(TRAN_FILE_NOT_EXISTS);
        }
    }

    @Override
    public TranFileDO getTranFile(Long id) {
        return tranFileMapper.selectById(id);
    }

    @Override
    public PageResult<TranFileDO> getTranFilePage(TranFilePageReqVO pageReqVO) {
        // ===================== 【权限控制】添加用户权限隔离 =====================
        // 获取当前登录用户信息
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String loginUsername = SecurityFrameworkUtils.getLoginUserUsername();

        if (loginUserId != null && loginUsername != null && !loginUsername.isEmpty()) {
            // 如果前端没有传入 username 参数，则自动限制为当前登录用户
            if (pageReqVO.getUsername() == null || pageReqVO.getUsername().isEmpty()) {
                // 检查是否为超级管理员（角色ID = 1）
                boolean isSuperAdmin = false;
                try {
                    Set<Long> userRoleIds = permissionApi.getLoginUserAllRoleIds(loginUserId).getData();
                    if (userRoleIds != null && userRoleIds.contains(1L)) {
                        isSuperAdmin = true;
                    }
                } catch (Exception e) {
                    log.warn("[getTranFilePage] 获取用户角色失败，按普通用户处理", e);
                }

                // 普通用户：只能查询自己的文档
                if (!isSuperAdmin) {
                    pageReqVO.setUsername(loginUsername);
                    log.debug("[getTranFilePage] 普通用户 {} 只能查询自己的文档", loginUsername);
                } else {
                    log.debug("[getTranFilePage] 超级管理员 {} 可以查询所有文档", loginUsername);
                }
            }
        }

        return tranFileMapper.selectPage(pageReqVO);
    }

}
