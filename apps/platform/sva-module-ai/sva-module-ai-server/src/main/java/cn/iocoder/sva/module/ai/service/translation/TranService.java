package cn.iocoder.sva.module.ai.service.translation;

import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranTranslateReqVO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranGlossaryDO;
import cn.iocoder.sva.module.ai.service.translation.tran.model.TaskInfo;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface TranService {

    String submitTranslationTask(@Valid TranTranslateReqVO reqVO);

    TaskInfo getTaskStatus(String taskId);

    ResponseEntity<Resource> downloadFile(String taskId, String fileType);

    List<TranGlossaryDO> getVisibleGlossaryList(Long userId, String username, String targetLanguage);

    /**
     * 保存术语到术语库
     * <p>
     * 接收术语映射和术语库ID，先校验用户是否有权限操作该术语库，
     * 然后将术语保存到数据库。如果术语已存在则更新，否则新增。
     *
     * @param terms 术语映射（key=源语言, value=目标语言）
     * @param username 用户名
     * @param glossaryId 术语库ID（必填）
     * @return 保存结果
     */
    Map<String, Object> saveTerms(Map<String, String> terms, String username, Long glossaryId);

    Map<String, Object> importGlossary(MultipartFile file);

    /**
     * 后台翻译任务（供后端其他功能调用）
     * <p>
     * 执行完整的翻译流程，包括：
     * 1. 查询术语库（如果启用术语替换）
     * 2. 上传原文件到 MinIO
     * 3. 创建文件记录到数据库
     * 4. 同步执行翻译任务
     * 5. 上传结果文件到 MinIO
     * 6. 更新数据库记录
     *
     * @param fileContent 文件内容（字节数组）
     * @param fileName 文件名（包含扩展名，如 "document.docx"）
     * @param targetLang 目标语言（如 "en"）
     * @param glossaryId 术语库ID（可选）
     * @param useGlossaryReplace 是否使用术语替换
     * @param strictFormat 是否严格保持格式
     * @param enableComparison 是否启用双语对照
     * @param enableQc 是否启用质量检查
     * @param modelId AI模型ID（可选，不传则使用默认模型）
     * @param disableCache 是否禁用缓存（1=禁用，0=启用）
     * @param roleId 聊天角色ID（可选，用于自定义翻译提示词）
     * @return true=翻译成功，false=翻译失败
     */
    boolean executeTranslationTask(byte[] fileContent, String fileName, String targetLang, Long glossaryId,
                                   boolean useGlossaryReplace, boolean strictFormat,
                                   boolean enableComparison, boolean enableQc,
                                   Long modelId, Integer disableCache, Long roleId);

    /**
     * 后台翻译任务（带用户名参数，用于 AI Tool 等无 SecurityContext 的场景）
     * <p>
     * 与 {@link #executeTranslationTask(byte[], String, String, Long, boolean, boolean, boolean, boolean, Long, Integer, Long)} 相同，
     * 但显式传入 username，避免依赖 SecurityContext
     *
     * @param fileContent 文件内容（字节数组）
     * @param fileName 文件名（包含扩展名，如 "document.docx"）
     * @param targetLang 目标语言（如 "en"）
     * @param glossaryId 术语库ID（可选）
     * @param useGlossaryReplace 是否使用术语替换
     * @param strictFormat 是否严格保持格式
     * @param enableComparison 是否启用双语对照
     * @param enableQc 是否启用质量检查
     * @param modelId AI模型ID（可选，不传则使用默认模型）
     * @param disableCache 是否禁用缓存（1=禁用，0=启用）
     * @param username 用户名（工号）
     * @param roleId 聊天角色ID（可选，用于自定义翻译提示词）
     * @return true=翻译成功，false=翻译失败
     */
    boolean executeTranslationTask(byte[] fileContent, String fileName, String targetLang, Long glossaryId,
                                   boolean useGlossaryReplace, boolean strictFormat,
                                   boolean enableComparison, boolean enableQc,
                                   Long modelId, Integer disableCache, String username, Long roleId);

}