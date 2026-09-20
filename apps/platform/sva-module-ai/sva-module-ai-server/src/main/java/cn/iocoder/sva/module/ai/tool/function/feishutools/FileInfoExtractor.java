package cn.iocoder.sva.module.ai.tool.function.feishutools;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.sva.module.ai.dal.dataobject.file.ChatbotFileDO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranFileDO;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件信息提取器
 * 负责从各种文件DO对象中提取信息
 */
@Slf4j
public class FileInfoExtractor {

    /**
     * 从文件记录中提取各种文件的URL
     */
    public Map<String, Object> extractFileUrls(TranFileDO fileDO) {
        log.debug("[extractFileUrls] 开始提取文件URL, fileName={}", fileDO.getFileName());
        Map<String, Object> fileUrls = new HashMap<>();

        fileUrls.put("fileName", fileDO.getFileName());
        fileUrls.put("fileExt", fileDO.getFileExt());
        fileUrls.put("fileStatus", fileDO.getFileStatus());

        // 翻译后的文件
        if (StrUtil.isNotBlank(fileDO.getFileUrl())) {
            fileUrls.put("translatedFile", fileDO.getFileUrl());
            log.debug("[extractFileUrls] 翻译文件URL: {}", fileDO.getFileUrl());
        }

        // 对照表
        if (StrUtil.isNotBlank(fileDO.getCompareFileUrl())) {
            fileUrls.put("comparisonFile", fileDO.getCompareFileUrl());
            log.debug("[extractFileUrls] 对照表URL: {}", fileDO.getCompareFileUrl());
        }

        // 质检报告
        if (StrUtil.isNotBlank(fileDO.getQcFileUrl())) {
            fileUrls.put("qcReport", fileDO.getQcFileUrl());
            log.debug("[extractFileUrls] QC报告URL: {}", fileDO.getQcFileUrl());
        }

        // 双语对照文档
        if (StrUtil.isNotBlank(fileDO.getContrastFileUrl())) {
            fileUrls.put("contrastFile", fileDO.getContrastFileUrl());
            log.debug("[extractFileUrls] 双语对照文档URL: {}", fileDO.getContrastFileUrl());
        }

        // 原始文件
        if (StrUtil.isNotBlank(fileDO.getSourceFileUrl())) {
            fileUrls.put("sourceFile", fileDO.getSourceFileUrl());
            log.debug("[extractFileUrls] 原始文件URL: {}", fileDO.getSourceFileUrl());
        }

        log.debug("[extractFileUrls] 文件URL提取完成");
        return fileUrls;
    }

    /**
     * 从 ChatbotFile 对象中提取文件信息
     */
    public Map<String, Object> extractChatbotFileInfo(ChatbotFileDO file) {
        log.debug("[extractChatbotFileInfo] 开始提取文件信息, id={}, fileName={}", file.getId(), file.getFileName());
        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put("id", file.getId());
        fileInfo.put("fileName", file.getFileName());
        fileInfo.put("filePath", file.getFilePath());
        fileInfo.put("fileSize", file.getFileSize());
        fileInfo.put("fileType", file.getFileType());
        fileInfo.put("fileExtension", file.getFileExtension());
        fileInfo.put("userJobNumber", file.getUserJobNumber());
        fileInfo.put("sessionId", file.getSessionId());
        fileInfo.put("uploadTime", file.getUploadTime());
        fileInfo.put("userQuestion", file.getUserQuestion());

        log.debug("[extractChatbotFileInfo] 文件信息提取完成");
        return fileInfo;
    }

    /**
     * 从文件路径中提取文件名
     */
    public String extractFileNameFromPath(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            return "unknown.docx";
        }

        int lastSlashIndex = filePath.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < filePath.length() - 1) {
            return filePath.substring(lastSlashIndex + 1);
        }

        return filePath;
    }
}