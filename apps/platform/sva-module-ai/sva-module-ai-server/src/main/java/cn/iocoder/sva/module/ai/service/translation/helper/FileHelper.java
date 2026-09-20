package cn.iocoder.sva.module.ai.service.translation.helper;

import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranFileDO;
import cn.iocoder.sva.module.ai.service.file.FileService;
import cn.iocoder.sva.module.ai.service.translation.TranFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;

@Slf4j
@Component
public class FileHelper {

    @Autowired
    private FileService fileService;

    @Autowired
    private TranFileService tranFileService;

    public String uploadToMinio(byte[] content, String filename, String directory) {
        try {
            String mimeType = getMimeType(filename);
            return fileService.createFile(content, filename, directory, mimeType);
        } catch (Exception e) {
            log.error("上传文件到 MinIO 失败: {}", filename, e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    public Long createFileRecord(String taskId, String fileName, String sourceFileUrl,
                                  String fileExt, long fileSize, String targetLang, String username) {
        TranFileDO fileDO = new TranFileDO();
        fileDO.setFileName(fileName);
        fileDO.setFileUrl("");
        fileDO.setSourceFileUrl(sourceFileUrl);
        fileDO.setFileExt(fileExt);
        fileDO.setFileSize(fileSize);
        fileDO.setFileType(targetLang);
        fileDO.setFileStatus(0);
        fileDO.setUsername(username);

        Long fileId = tranFileService.createTranFile(fileDO);
        log.info("[createFileRecord][taskId={}] 文件记录已创建到数据库, fileId={}", taskId, fileId);

        return fileId;
    }

    public TranFileDO getFileRecord(Long fileId) {
        return tranFileService.getTranFile(fileId);
    }

    public void updateFileRecord(Long fileId, String translatedFileUrl,
                                  String compareFileUrl, String qcFileUrl,
                                  String contrastFileUrl, Integer fileStatus) {
        if (fileId == null) {
            log.warn("[updateFileRecord] 文件记录ID为空，跳过更新");
            return;
        }

        TranFileDO fileDO = tranFileService.getTranFile(fileId);
        if (fileDO == null) {
            log.warn("[updateFileRecord] 文件记录不存在, fileId={}", fileId);
            return;
        }

        if (translatedFileUrl != null) {
            fileDO.setFileUrl(translatedFileUrl);
        }
        if (compareFileUrl != null) {
            fileDO.setCompareFileUrl(compareFileUrl);
        }
        if (qcFileUrl != null) {
            fileDO.setQcFileUrl(qcFileUrl);
        }
        if (contrastFileUrl != null) {
            fileDO.setContrastFileUrl(contrastFileUrl);
        }
        if (fileStatus != null) {
            fileDO.setFileStatus(fileStatus);
        }

        tranFileService.updateTranFile(fileDO);
        log.info("[updateFileRecord][fileId={}] 文件记录已更新到数据库", fileId);
    }

    public String getMimeType(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }
        String lowerName = filename.toLowerCase();
        if (lowerName.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xlsm")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if (lowerName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerName.endsWith(".txt")) {
            return "text/plain";
        } else if (lowerName.endsWith(".json")) {
            return "application/json";
        }
        return "application/octet-stream";
    }

    public byte[] readFileToBytes(String filePath) throws Exception {
        try (FileInputStream fis = new FileInputStream(filePath);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            return bos.toByteArray();
        }
    }
}