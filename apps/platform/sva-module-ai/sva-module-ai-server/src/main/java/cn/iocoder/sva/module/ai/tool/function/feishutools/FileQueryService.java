package cn.iocoder.sva.module.ai.tool.function.feishutools;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.sva.module.ai.dal.dataobject.file.ChatbotFileDO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranFileDO;
import cn.iocoder.sva.module.ai.dal.mysql.file.ChatbotFileMapper;
import cn.iocoder.sva.module.ai.dal.mysql.translation.TranFileMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件查询服务
 * 负责查询用户上传的文件和翻译结果文件
 */
@Slf4j
public class FileQueryService {

    private final ChatbotFileMapper chatbotFileMapper;
    private final TranFileMapper tranFileMapper;
    private final UserContextManager userContextManager;
    private final FileInfoExtractor fileInfoExtractor;

    public FileQueryService(ChatbotFileMapper chatbotFileMapper,
                            TranFileMapper tranFileMapper,
                            UserContextManager userContextManager,
                            FileInfoExtractor fileInfoExtractor) {
        this.chatbotFileMapper = chatbotFileMapper;
        this.tranFileMapper = tranFileMapper;
        this.userContextManager = userContextManager;
        this.fileInfoExtractor = fileInfoExtractor;
    }

    /**
     * 查询用户最近上传的一个文件
     */
    public Map<String, Object> getLatestUploadedFile(String username) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (StrUtil.isBlank(username)) {
                username = userContextManager.getEffectiveUsername();
            }

            if (StrUtil.isBlank(username)) {
                log.warn("[getLatestUploadedFile] 无法获取用户名");
                result.put("success", false);
                result.put("message", "无法获取用户名");
                return result;
            }

            String sessionId = userContextManager.getUserChatId(username);

            LambdaQueryWrapper<ChatbotFileDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ChatbotFileDO::getUserJobNumber, username);

            if (StrUtil.isNotBlank(sessionId)) {
                queryWrapper.eq(ChatbotFileDO::getSessionId, sessionId);
            }

            queryWrapper.orderByDesc(ChatbotFileDO::getUploadTime)
                    .last("LIMIT 1");

            ChatbotFileDO latestFile = chatbotFileMapper.selectOne(queryWrapper);

            if (latestFile != null) {
                log.info("[getLatestUploadedFile] 查询成功, 找到文件: id={}, fileName={}",
                        latestFile.getId(), latestFile.getFileName());
                result.put("success", true);
                result.put("fileInfo", fileInfoExtractor.extractChatbotFileInfo(latestFile));
                result.put("message", "找到最近上传的文件");
            } else {
                log.warn("[getLatestUploadedFile] 未找到符合条件的文件");
                result.put("success", false);
                result.put("message", "未找到符合条件的文件");
            }
        } catch (Exception e) {
            log.error("[getLatestUploadedFile] 查询最近上传文件失败", e);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 查询用户最近上传的N个文件
     */
    public Map<String, Object> getRecentUploadedFiles(String username, Integer count) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (StrUtil.isBlank(username)) {
                username = userContextManager.getEffectiveUsername();
            }

            if (StrUtil.isBlank(username)) {
                log.warn("[getRecentUploadedFiles] 无法获取用户名");
                result.put("success", false);
                result.put("message", "无法获取用户名");
                return result;
            }

            if (count == null || count <= 0) {
                count = 5;
            }

            String sessionId = userContextManager.getUserChatId(username);

            LambdaQueryWrapper<ChatbotFileDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ChatbotFileDO::getUserJobNumber, username);

            if (StrUtil.isNotBlank(sessionId)) {
                queryWrapper.eq(ChatbotFileDO::getSessionId, sessionId);
            }

            queryWrapper.orderByDesc(ChatbotFileDO::getUploadTime)
                    .last("LIMIT " + count);

            List<ChatbotFileDO> recentFiles = chatbotFileMapper.selectList(queryWrapper);

            if (recentFiles != null && !recentFiles.isEmpty()) {
                List<Map<String, Object>> fileList = new ArrayList<>();
                for (ChatbotFileDO file : recentFiles) {
                    fileList.add(fileInfoExtractor.extractChatbotFileInfo(file));
                }

                log.info("[getRecentUploadedFiles] 查询成功, 找到{}个文件", fileList.size());
                result.put("success", true);
                result.put("files", fileList);
                result.put("count", fileList.size());
                result.put("message", "找到" + fileList.size() + "个文件");
            } else {
                log.warn("[getRecentUploadedFiles] 未找到符合条件的文件");
                result.put("success", false);
                result.put("message", "未找到符合条件的文件");
            }
        } catch (Exception e) {
            log.error("[getRecentUploadedFiles] 查询最近上传文件列表失败", e);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取当前用户的最新翻译文件记录
     */
    public TranFileDO getLatestTranFileByCurrentUser() {
        try {
            String username = userContextManager.getEffectiveUsername();

            if (StrUtil.isBlank(username)) {
                log.warn("[getLatestTranFileByCurrentUser] 无法获取当前用户名");
                return null;
            }

            LambdaQueryWrapper<TranFileDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TranFileDO::getUsername, username)
                    .orderByDesc(TranFileDO::getCreateTime)
                    .last("LIMIT 1");

            TranFileDO fileDO = tranFileMapper.selectOne(queryWrapper);

            if (fileDO != null) {
                log.info("[getLatestTranFileByCurrentUser] 找到最新翻译文件, fileId={}, fileName={}",
                        fileDO.getId(), fileDO.getFileName());
            }

            return fileDO;
        } catch (Exception e) {
            log.error("[getLatestTranFileByCurrentUser] 查询失败", e);
            return null;
        }
    }
}