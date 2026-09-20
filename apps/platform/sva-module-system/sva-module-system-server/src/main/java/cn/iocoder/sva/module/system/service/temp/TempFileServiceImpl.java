package cn.iocoder.sva.module.system.service.temp;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import cn.iocoder.sva.module.system.controller.admin.temp.vo.*;
import cn.iocoder.sva.module.system.dal.dataobject.temp.TempFileDO;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.pojo.PageParam;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;
import cn.iocoder.sva.module.infra.api.file.FileApi;

import cn.iocoder.sva.module.system.dal.mysql.temp.TempFileMapper;

import static cn.iocoder.sva.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.sva.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.sva.framework.common.util.collection.CollectionUtils.diffList;
import static cn.iocoder.sva.module.system.enums.ErrorCodeConstants.*;

/**
 * 模板管理 Service 实现类
 *
 * @author like
 */
@Service
@Validated
@Slf4j
public class TempFileServiceImpl implements TempFileService {

    @Resource
    private TempFileMapper tempFileMapper;

    @Resource
    private FileApi fileApi;

    @Override
    public Long createTempFile(TempFileSaveReqVO createReqVO) {
        // 插入
        TempFileDO tempFile = BeanUtils.toBean(createReqVO, TempFileDO.class);
        tempFileMapper.insert(tempFile);

        // 返回
        return tempFile.getId();
    }

    @Override
    public void updateTempFile(TempFileSaveReqVO updateReqVO) {
        // 校验存在
        validateTempFileExists(updateReqVO.getId());
        // 更新
        TempFileDO updateObj = BeanUtils.toBean(updateReqVO, TempFileDO.class);
        tempFileMapper.updateById(updateObj);
    }

    @Override
    public void deleteTempFile(Long id) {
        // 校验存在
        TempFileDO tempFile = validateTempFileExists(id);
        
        // 删除 MinIO 中的文件
        deleteMinioFileByUrl(tempFile.getUrl());
        
        // 删除数据库记录
        tempFileMapper.deleteById(id);
        
        log.info("[deleteTempFile][id={}] 已删除模板文件记录及关联的 MinIO 文件", id);
    }

    @Override
    public void deleteTempFileListByIds(List<Long> ids) {
        // 先获取所有记录，用于删除 MinIO 文件
        List<TempFileDO> tempFiles = tempFileMapper.selectByIds(ids);
        
        // 删除每个文件对应的 MinIO 文件
        for (TempFileDO tempFile : tempFiles) {
            try {
                deleteMinioFileByUrl(tempFile.getUrl());
            } catch (Exception e) {
                log.error("[deleteTempFileListByIds][id={}] 删除 MinIO 文件失败", tempFile.getId(), e);
            }
        }
        
        // 批量删除数据库记录
        tempFileMapper.deleteByIds(ids);
        
        log.info("[deleteTempFileListByIds] 已批量删除 {} 个模板文件记录及关联的 MinIO 文件", ids.size());
    }


    private TempFileDO validateTempFileExists(Long id) {
        TempFileDO tempFile = tempFileMapper.selectById(id);
        if (tempFile == null) {
            throw exception(TEMP_FILE_NOT_EXISTS);
        }
        return tempFile;
    }

    /**
     * 根据 URL 删除 MinIO 文件
     *
     * @param fileUrl 文件 URL
     */
    private void deleteMinioFileByUrl(String fileUrl) {
        if (StrUtil.isBlank(fileUrl)) {
            log.debug("[deleteMinioFileByUrl] 文件 URL 为空，跳过");
            return;
        }

        try {
            log.info("[deleteMinioFileByUrl] 准备删除 MinIO 文件: {}", fileUrl);
            // 通过 RPC 调用 infra 模块的删除接口
            fileApi.deleteFileByUrl(fileUrl);
            log.info("[deleteMinioFileByUrl] ✓ 成功删除 MinIO 文件: {}", fileUrl);
        } catch (Exception e) {
            log.error("[deleteMinioFileByUrl] ✗ 删除 MinIO 文件失败: {}", fileUrl, e);
        }
    }

    @Override
    public TempFileDO getTempFile(Long id) {
        return tempFileMapper.selectById(id);
    }

    @Override
    public TempFileDO getTempFileByTempName(String tempName) {
        return tempFileMapper.selectOne(TempFileDO::getTempName, tempName);
    }

    @Override
    public PageResult<TempFileDO> getTempFilePage(TempFilePageReqVO pageReqVO) {
        return tempFileMapper.selectPage(pageReqVO);
    }

    @Override
    public Map<String, Object> uploadFile(MultipartFile file, String tempName) throws Exception {
        // 1. 校验文件
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        // 2. 校验模板标识
        if (StrUtil.isBlank(tempName)) {
            throw new IllegalArgumentException("模板标识不能为空");
        }

        // 3. 读取文件内容
        byte[] content = file.getBytes();
        String originalFilename = file.getOriginalFilename();
        
        // 4. 处理文件名
        String name = StrUtil.isNotBlank(originalFilename) ? originalFilename : DigestUtil.sha256Hex(content);
        
        // 5. 获取文件类型（MIME类型）
        String type = file.getContentType();
        if (StrUtil.isBlank(type)) {
            type = "application/octet-stream";
        }

        // 6. 调用 infra 模块的 FileApi 上传到 MinIO
        String directory = "system/temp";
        String url = fileApi.createFile(content, name, directory, type);

        // 7. 保存到 TempFile 表
        TempFileDO tempFile = new TempFileDO();
        tempFile.setTempName(tempName);
        tempFile.setName(name);
        tempFile.setUrl(url);
        tempFile.setType(type);
        tempFile.setSize((int) file.getSize());
        
        tempFileMapper.insert(tempFile);

        // 8. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("id", tempFile.getId());
        result.put("name", tempFile.getName());
        result.put("url", tempFile.getUrl());
        result.put("type", tempFile.getType());
        result.put("size", tempFile.getSize());
        
        return result;
    }

}