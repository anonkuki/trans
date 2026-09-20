package cn.iocoder.sva.module.ai.service.translation.tran.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 异步任务状态模型
 * <p>
 * 对应 Python 项目 main.py 中的 tasks 字典，用于跟踪翻译任务的完整生命周期。
 * <p>
 * 任务状态流转：pending → running → completed / failed
 * <ul>
 *   <li>pending: 任务已创建，等待执行</li>
 *   <li>running: 任务正在执行中</li>
 *   <li>completed: 任务执行成功</li>
 *   <li>failed: 任务执行失败（error 字段包含错误信息）</li>
 * </ul>
 * <p>
 * 前端通过轮询 GET /ai/tran/task/{taskId} 获取此对象来展示进度和结果。
 */
@Data
public class TaskInfo {

    @JsonProperty("taskId")
    private String id;

    @JsonIgnore
    private Long fileId;

    @JsonIgnore
    private Long glossaryId;

    private String status;

    private int progress;

    @JsonProperty("progressMsg")
    private String progressMsg;

    private double elapsed;

    @JsonProperty("realtime_pairs")
    private List<TranslationPair> realtimePairs;

    private List<TranslationPair> pairs;

    @JsonProperty("downloads")
    private Map<String, DownloadInfo> downloadsInfo;

    @JsonProperty("minioUrls")
    public Map<String, String> getMinioUrls() {
        Map<String, String> urls = new HashMap<>();
        if (downloadsInfo != null) {
            for (Map.Entry<String, DownloadInfo> entry : downloadsInfo.entrySet()) {
                if (entry.getValue() != null && entry.getValue().getMinioUrl() != null) {
                    urls.put(entry.getKey(), entry.getValue().getMinioUrl());
                }
            }
        }
        return urls;
    }

    private String error;

    @JsonProperty("qc_issues")
    private Integer qcIssues;

    @JsonProperty("enable_qc")
    private boolean enableQc;

    public TaskInfo() {
        this.status = "pending";
        this.progress = 0;
        this.progressMsg = "";
        this.elapsed = 0;
        this.realtimePairs = new ArrayList<>();
        this.pairs = new ArrayList<>();
        this.downloadsInfo = new HashMap<>();
    }

    /**
     * 获取下载信息Map（内部使用，不序列化）
     */
    @JsonIgnore
    public Map<String, DownloadInfo> getDownloads() {
        return downloadsInfo;
    }

    public static TaskInfo createNew(String taskId) {
        TaskInfo task = new TaskInfo();
        task.setId(taskId);
        return task;
    }
}