package cn.iocoder.sva.module.ai.tool.function.feishutools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranFileDO;
import cn.iocoder.sva.module.ai.service.file.FileService;
import cn.iocoder.sva.module.ai.service.translation.TranService;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 翻译服务执行器
 * 负责执行各种文档的翻译任务
 */
@Slf4j
public class TranslationExecutor {

    private final TranService tranService;
    private final FileService fileService;
    private final FileQueryService fileQueryService;
    private final FileInfoExtractor fileInfoExtractor;
    private final UserContextManager userContextManager;

    public TranslationExecutor(TranService tranService,
                               FileService fileService,
                               FileQueryService fileQueryService,
                               FileInfoExtractor fileInfoExtractor,
                               UserContextManager userContextManager) {
        this.tranService = tranService;
        this.fileService = fileService;
        this.fileQueryService = fileQueryService;
        this.fileInfoExtractor = fileInfoExtractor;
        this.userContextManager = userContextManager;
    }

    /**
     * 执行通用翻译任务
     */
    public Map<String, Object> executeTranslation(String filePath,
                                                  String targetLanguage,
                                                  Boolean enableComparison,
                                                  Boolean enableQc,
                                                  Boolean strictFormat,
                                                  Boolean useGlossary,
                                                  Long glossaryId,
                                                  String fileType) {
        log.info("[executeTranslation] ========== 翻译工具被调用 ==========");
//        log.info("[executeTranslation] 参数详情:");
//        log.info("[executeTranslation]   - filePath: {}", filePath);
//        log.info("[executeTranslation]   - targetLanguage: {}", targetLanguage);
//        log.info("[executeTranslation]   - enableComparison: {}", enableComparison);
//        log.info("[executeTranslation]   - enableQc: {}", enableQc);
//        log.info("[executeTranslation]   - strictFormat: {}", strictFormat);
//        log.info("[executeTranslation]   - useGlossary: {}", useGlossary);
//        log.info("[executeTranslation]   - glossaryId: {}", glossaryId);
//        log.info("[executeTranslation]   - fileType: {}", fileType);
        
        Map<String, Object> result = new HashMap<>();

        try {
            // 参数验证
            if (StrUtil.isBlank(filePath)) {
                result.put("success", false);
                result.put("message", "文件路径不能为空");
                return result;
            }

            if (StrUtil.isBlank(targetLanguage)) {
                result.put("success", false);
                result.put("message", "目标语言不能为空");
                return result;
            }

            // 设置默认值（用户未明确说明的默认为 false）
            if (enableComparison == null) enableComparison = true;   // 双语对照默认启用
            if (enableQc == null) enableQc = false;                  // 质量检查默认不启用
            if (useGlossary == null) useGlossary = true;             // 术语库默认启用

            // 获取当前用户名
            String username = userContextManager.getEffectiveUsername();
            if (StrUtil.isBlank(username)) {
                log.error("[executeTranslation] 无法获取当前用户信息");
                result.put("success", false);
                result.put("message", "无法获取当前用户信息");
                return result;
            }
            log.info("[executeTranslation] 获取到用户名: username={}", username);

            // ===================== 【关键调试】记录术语库使用情况 =====================
//            if (useGlossary != null && useGlossary) {
//                if (glossaryId != null) {
//                    log.info("[executeTranslation]  用户启用了术语库，glossaryId={}", glossaryId);
//                } else {
//                    log.warn("[executeTranslation]  用户启用了术语库(useGlossary=true)，但 glossaryId=null！这会导致术语库不生效！");
//                    log.warn("[executeTranslation]  可能原因：AI没有调用 get_user_glossary_list 工具，或者调用后没有正确提取 glossaryId");
//                }
//            } else {
//                log.info("[executeTranslation] 用户未启用术语库 (useGlossary={})", useGlossary);
//            }

            // 从 MinIO 读取文件内容
            byte[] fileContent = readFileFromMinio(filePath);
            if (fileContent == null || fileContent.length == 0) {
                log.error("[executeTranslation] 从MinIO读取文件失败: {}", filePath);
                result.put("success", false);
                result.put("message", "无法读取文件: " + filePath);
                return result;
            }

            String fileName = fileInfoExtractor.extractFileNameFromPath(filePath);

            // 调用翻译服务（传入 username 和最终的 glossaryId）
//            log.info("[executeTranslation] 即将调用 tranService.executeTranslationTask");
//            log.info("[executeTranslation]   - 传入的 glossaryId: {}", useGlossary ? glossaryId : null);
//            log.info("[executeTranslation]   - 传入的 useGlossaryReplace: {}", useGlossary);
            
            boolean translationSuccess = tranService.executeTranslationTask(
                    fileContent,
                    fileName,
                    targetLanguage,
                    useGlossary ? glossaryId : null,
                    useGlossary,
                    strictFormat,
                    enableComparison,
                    enableQc,
                    null,
                    0,
                    username,
                    null
            );

            if (!translationSuccess) {
                log.error("[executeTranslation] 翻译任务执行失败");
                result.put("success", false);
                result.put("message", "翻译任务执行失败");
                return result;
            }

            // 查询结果
            TranFileDO fileDO = fileQueryService.getLatestTranFileByCurrentUser();

            if (fileDO == null) {
                log.error("[executeTranslation] 未找到翻译结果文件记录");
                result.put("success", false);
                result.put("message", "未找到翻译结果文件");
                return result;
            }

            result.put("success", true);
            result.put("message", "翻译完成");
            result.put("files", fileInfoExtractor.extractFileUrls(fileDO));
            result.put("fileName", fileName);
            result.put("targetLanguage", targetLanguage);
            
            log.info("[executeTranslation] ========== 翻译完成 ==========");

        } catch (Exception e) {
            log.error("[executeTranslation] {}翻译任务失败", fileType, e);
            result.put("success", false);
            result.put("message", "翻译任务失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 执行Excel翻译任务（特殊处理）
     */
    public Map<String, Object> executeExcelTranslation(String filePath,
                                                       String targetLanguage,
                                                       Boolean enableQc,
                                                       Boolean useGlossary,
                                                       Long glossaryId) {
        log.info("[executeExcelTranslation] 工具被调用, filePath={}, targetLanguage={}", filePath, targetLanguage);
        Map<String, Object> result = new HashMap<>();

        try {
            if (StrUtil.isBlank(filePath)) {
                result.put("success", false);
                result.put("message", "文件路径不能为空");
                return result;
            }

            if (StrUtil.isBlank(targetLanguage)) {
                result.put("success", false);
                result.put("message", "目标语言不能为空");
                return result;
            }

            if (enableQc == null) enableQc = false;
            if (useGlossary == null) useGlossary = false;

            // 获取当前用户名
            String username = userContextManager.getEffectiveUsername();
            if (StrUtil.isBlank(username)) {
                log.error("[executeExcelTranslation] 无法获取当前用户信息");
                result.put("success", false);
                result.put("message", "无法获取当前用户信息");
                return result;
            }
            log.info("[executeExcelTranslation] 获取到用户名: username={}", username);

            byte[] fileContent = readFileFromMinio(filePath);
            if (fileContent == null || fileContent.length == 0) {
                log.error("[executeExcelTranslation] 从MinIO读取文件失败: {}", filePath);
                result.put("success", false);
                result.put("message", "无法读取文件: " + filePath);
                return result;
            }

            String fileName = fileInfoExtractor.extractFileNameFromPath(filePath);

            // Excel不支持双语对照和严格格式
            boolean translationSuccess = tranService.executeTranslationTask(
                    fileContent,
                    fileName,
                    targetLanguage,
                    useGlossary ? glossaryId : null,
                    useGlossary,
                    false,
                    false,
                    enableQc,
                    null,
                    0,
                    username,
                    null
            );

            if (!translationSuccess) {
                log.error("[executeExcelTranslation] 翻译任务执行失败");
                result.put("success", false);
                result.put("message", "翻译任务执行失败");
                return result;
            }

            TranFileDO fileDO = fileQueryService.getLatestTranFileByCurrentUser();

            if (fileDO == null) {
                log.error("[executeExcelTranslation] 未找到翻译结果文件记录");
                result.put("success", false);
                result.put("message", "未找到翻译结果文件");
                return result;
            }

            result.put("success", true);
            result.put("message", "翻译完成");
            result.put("files", fileInfoExtractor.extractFileUrls(fileDO));
            result.put("fileName", fileName);
            result.put("targetLanguage", targetLanguage);

        } catch (Exception e) {
            log.error("[executeExcelTranslation] Excel翻译任务失败", e);
            result.put("success", false);
            result.put("message", "翻译任务失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 从 URL 读取文件内容（支持 MinIO 等 HTTP 可访问的文件）
     */
    private byte[] readFileFromMinio(String filePath) {
        try {
            log.info("[readFileFromMinio] 开始从URL读取文件: url={}", filePath);
            
            if (StrUtil.isBlank(filePath)) {
                log.error("[readFileFromMinio] 文件路径为空");
                return null;
            }
            
            // 直接使用 HTTP GET 下载文件
            byte[] content = HttpUtil.downloadBytes(filePath);
            
            if (content == null || content.length == 0) {
                log.error("[readFileFromMinio] 下载文件内容为空: url={}", filePath);
                return null;
            }
            
            log.info("[readFileFromMinio] 成功读取文件, size={} bytes", content.length);
            return content;
            
        } catch (Exception e) {
            log.error("[readFileFromMinio] 从URL读取文件失败: url={}", filePath, e);
            return null;
        }
    }
}