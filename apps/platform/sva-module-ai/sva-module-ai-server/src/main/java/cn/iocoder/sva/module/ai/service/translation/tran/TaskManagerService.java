package cn.iocoder.sva.module.ai.service.translation.tran;

import cn.iocoder.sva.module.ai.service.translation.tran.model.TaskInfo;
import cn.iocoder.sva.module.ai.service.translation.tran.model.TranslationPair;

import java.util.List;
import java.util.Map;

/**
 * 异步任务管理服务接口
 * <p>
 * 管理翻译任务的创建、查询、取消等生命周期操作。
 * 对应 Python 项目 main.py 中的 tasks 字典和后台线程任务管理。
 */
public interface TaskManagerService {

    /**
     * 创建新任务
     *
     * @return 任务ID
     */
    String createTask();

    /**
     * 获取任务信息
     *
     * @param taskId 任务ID
     * @return 任务信息，不存在则返回 null
     */
    TaskInfo getTask(String taskId);

    /**
     * 更新任务状态
     *
     * @param taskId  任务ID
     * @param status  新状态（pending/running/completed/failed）
     */
    void updateStatus(String taskId, String status);

    /**
     * 更新任务进度
     *
     * @param taskId      任务ID
     * @param progress    进度（0-100）
     * @param progressMsg 进度消息
     */
    void updateProgress(String taskId, int progress, String progressMsg);

    /**
     * 添加翻译对到实时列表
     *
     * @param taskId  任务ID
     * @param pair    翻译对
     */
    void addRealtimePair(String taskId, TranslationPair pair);

    /**
     * 设置最终翻译对列表
     *
     * @param taskId 任务ID
     * @param pairs  翻译对列表
     */
    void setFinalPairs(String taskId, List<TranslationPair> pairs);

    /**
     * 设置下载文件
     *
     * @param taskId  任务ID
     * @param key     文件标识（如 "file"、"excel"、"qc"）
     * @param path    文件路径
     * @param filename 下载时的文件名
     */
    void setDownload(String taskId, String key, String path, String filename);

    /**
     * 设置下载文件的 MinIO URL
     *
     * @param taskId  任务ID
     * @param key     文件标识（如 "file"、"excel"、"qc"）
     * @param minioUrl MinIO 文件访问 URL
     */
    void setDownloadMinioUrl(String taskId, String key, String minioUrl);

    /**
     * 设置错误信息
     *
     * @param taskId 任务ID
     * @param error  错误信息
     */
    void setError(String taskId, String error);

    /**
     * 设置QC问题数
     *
     * @param taskId    任务ID
     * @param qcIssues  问题数
     */
    void setQcIssues(String taskId, int qcIssues);

    /**
     * 设置是否启用QC
     *
     * @param taskId    任务ID
     * @param enableQc  是否启用QC
     */
    void setEnableQc(String taskId, boolean enableQc);

    /**
     * 设置关联的文件记录ID
     *
     * @param taskId 任务ID
     * @param fileId 文件记录ID
     */
    void setFileId(String taskId, Long fileId);

    /**
     * 设置关联的术语表记录ID
     *
     * @param taskId 任务ID
     * @param glossaryId 术语表记录ID
     */
    void setGlossaryId(String taskId, Long glossaryId);

    /**
     * 获取所有任务
     *
     * @return 任务映射（taskId -> TaskInfo）
     */
    Map<String, TaskInfo> getAllTasks();

    /**
     * 删除任务
     *
     * @param taskId 任务ID
     */
    void removeTask(String taskId);

    /**
     * 检查任务是否存在
     *
     * @param taskId 任务ID
     * @return 是否存在
     */
    boolean exists(String taskId);
}