package cn.iocoder.sva.module.ai.service.translation.tran.impl;

import cn.hutool.json.JSONUtil;
import cn.iocoder.sva.framework.common.util.json.JsonUtils;
import cn.iocoder.sva.module.ai.service.translation.tran.TaskManagerService;
import cn.iocoder.sva.module.ai.service.translation.tran.constant.TranslationTaskRedisKeyConstants;
import cn.iocoder.sva.module.ai.service.translation.tran.model.DownloadInfo;
import cn.iocoder.sva.module.ai.service.translation.tran.model.TaskInfo;
import cn.iocoder.sva.module.ai.service.translation.tran.model.TranslationPair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TaskManagerServiceImpl implements TaskManagerService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final int MAX_REALTIME_PAIRS = 100;
    
    private static final Duration TASK_RETENTION_DURATION = Duration.ofHours(8);

    @Override
    public String createTask() {
        String taskId = UUID.randomUUID().toString().substring(0, 8);
        TaskInfo task = TaskInfo.createNew(taskId);
        
        // 保存任务信息到 Redis，设置24小时过期
        String taskKey = TranslationTaskRedisKeyConstants.getTranslationTaskKey(taskId);
        stringRedisTemplate.opsForValue().set(taskKey, JsonUtils.toJsonString(task), 
            TASK_RETENTION_DURATION);
        
        // 添加到活跃任务集合（不设置过期时间，由定时任务清理）
        stringRedisTemplate.opsForSet().add(TranslationTaskRedisKeyConstants.TRANSLATION_TASKS_ACTIVE, taskId);
        
        // 保存任务创建时间到哈希表（不设置过期时间，由定时任务清理）
        stringRedisTemplate.opsForHash().put(
            TranslationTaskRedisKeyConstants.TRANSLATION_TASK_START_TIMES, 
            taskId, 
            String.valueOf(Instant.now().toEpochMilli())
        );
        
        log.info("创建任务: {}, 过期时间: {} 小时", taskId, TASK_RETENTION_DURATION.toHours());
        return taskId;
    }

    @Override
    public TaskInfo getTask(String taskId) {
        String taskKey = TranslationTaskRedisKeyConstants.getTranslationTaskKey(taskId);
        String taskJson = stringRedisTemplate.opsForValue().get(taskKey);
        
        if (taskJson != null) {
            try {
                TaskInfo task = JsonUtils.parseObject(taskJson, TaskInfo.class);
                
                // 计算已用时间
                String startTimeStr = (String) stringRedisTemplate.opsForHash().get(
                    TranslationTaskRedisKeyConstants.TRANSLATION_TASK_START_TIMES, 
                    taskId
                );
                if (startTimeStr != null) {
                    Instant startTime = Instant.ofEpochMilli(Long.parseLong(startTimeStr));
                    task.setElapsed(Duration.between(startTime, Instant.now()).toMillis() / 1000.0);
                }
                
                return task;
            } catch (Exception e) {
                log.error("从 Redis 获取任务失败: {}, error={}", taskId, e.getMessage(), e);
                return null;
            }
        }
        return null;
    }

    @Override
    public void updateStatus(String taskId, String status) {
        // 性能优化：直接更新状态字段，避免读取整个对象
        TaskInfo task = getTask(taskId);
        if (task != null) {
            task.setStatus(status);
            saveTask(taskId, task);
            log.debug("任务 {} 状态更新: {}", taskId, status);
        }
    }

    @Override
    public void updateProgress(String taskId, int progress, String progressMsg) {
        TaskInfo task = getTask(taskId);
        if (task != null) {
            task.setProgress(progress);
            task.setProgressMsg(progressMsg != null ? progressMsg : "");
            saveTask(taskId, task);
        }
    }

    @Override
    public void addRealtimePair(String taskId, TranslationPair pair) {
        TaskInfo task = getTask(taskId);
        if (task != null && task.getRealtimePairs() != null) {
            List<TranslationPair> pairs = task.getRealtimePairs();
            synchronized (pairs) {
                if (pairs.size() >= MAX_REALTIME_PAIRS) {
                    pairs.remove(0);
                }
                pairs.add(pair);
            }
            saveTask(taskId, task);
        }
    }

    @Override
    public void setFinalPairs(String taskId, List<TranslationPair> pairs) {
        TaskInfo task = getTask(taskId);
        if (task != null) {
            task.setPairs(pairs);
            saveTask(taskId, task);
        }
    }

    @Override
    public void setDownload(String taskId, String key, String path, String filename) {
        TaskInfo task = getTask(taskId);
        if (task != null) {
            DownloadInfo info = new DownloadInfo();
            info.setPath(path);
            info.setFilename(filename);
            task.getDownloads().put(key, info);
            saveTask(taskId, task);
        }
    }

    @Override
    public void setDownloadMinioUrl(String taskId, String key, String minioUrl) {
        TaskInfo task = getTask(taskId);
        if (task != null) {
            Map<String, DownloadInfo> downloads = task.getDownloads();
            downloads.computeIfAbsent(key, k -> new DownloadInfo()).setMinioUrl(minioUrl);
            saveTask(taskId, task);
            log.debug("任务 {} 设置 {} 的 MinIO URL: {}", taskId, key, minioUrl);
        }
    }

    @Override
    public void setError(String taskId, String error) {
        TaskInfo task = getTask(taskId);
        if (task != null) {
            task.setStatus("failed");
            task.setError(error);
            saveTask(taskId, task);
            log.error("任务 {} 失败: {}", taskId, error);
        }
    }

    @Override
    public void setQcIssues(String taskId, int qcIssues) {
        TaskInfo task = getTask(taskId);
        if (task != null) {
            task.setQcIssues(qcIssues);
            saveTask(taskId, task);
        }
    }

    @Override
    public void setEnableQc(String taskId, boolean enableQc) {
        TaskInfo task = getTask(taskId);
        if (task != null) {
            task.setEnableQc(enableQc);
            saveTask(taskId, task);
        }
    }

    @Override
    public void setFileId(String taskId, Long fileId) {
        TaskInfo task = getTask(taskId);
        if (task != null) {
            task.setFileId(fileId);
            saveTask(taskId, task);
        }
    }

    @Override
    public void setGlossaryId(String taskId, Long glossaryId) {
        TaskInfo task = getTask(taskId);
        if (task != null) {
            task.setGlossaryId(glossaryId);
            saveTask(taskId, task);
            log.debug("任务 {} 设置术语库ID: {}", taskId, glossaryId);
        }
    }

    @Override
    public Map<String, TaskInfo> getAllTasks() {
        // 从活跃任务集合中获取所有任务ID
        Set<String> taskIds = stringRedisTemplate.opsForSet().members(
            TranslationTaskRedisKeyConstants.TRANSLATION_TASKS_ACTIVE
        );
        
        Map<String, TaskInfo> result = new HashMap<>();
        if (taskIds != null) {
            for (String taskId : taskIds) {
                TaskInfo task = getTask(taskId);
                if (task != null) {
                    result.put(taskId, task);
                }
            }
        }
        return result;
    }

    @Override
    public void removeTask(String taskId) {
        // 从 Redis 删除任务信息
        String taskKey = TranslationTaskRedisKeyConstants.getTranslationTaskKey(taskId);
        stringRedisTemplate.delete(taskKey);
        
        // 从活跃任务集合中移除
        stringRedisTemplate.opsForSet().remove(
            TranslationTaskRedisKeyConstants.TRANSLATION_TASKS_ACTIVE, 
            taskId
        );
        
        // 从创建时间哈希表中移除
        stringRedisTemplate.opsForHash().delete(
            TranslationTaskRedisKeyConstants.TRANSLATION_TASK_START_TIMES, 
            taskId
        );
        
        log.info("删除任务: {}", taskId);
    }

    @Override
    public boolean exists(String taskId) {
        String taskKey = TranslationTaskRedisKeyConstants.getTranslationTaskKey(taskId);
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(taskKey));
    }

    /**
     * 保存任务到 Redis，保持原有的过期时间
     * 性能优化：避免每次都查询TTL，直接使用固定的过期时间
     */
    private void saveTask(String taskId, TaskInfo task) {
        String taskKey = TranslationTaskRedisKeyConstants.getTranslationTaskKey(taskId);
        // 直接使用固定的8小时过期时间，避免额外的TTL查询开销
        stringRedisTemplate.opsForValue().set(taskKey, JsonUtils.toJsonString(task), 
            TASK_RETENTION_DURATION);
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanupCompletedTasks() {
        Instant cutoff = Instant.now().minus(TASK_RETENTION_DURATION);
        int removedCount = 0;
        
        // 获取所有活跃任务
        Set<String> taskIds = stringRedisTemplate.opsForSet().members(
            TranslationTaskRedisKeyConstants.TRANSLATION_TASKS_ACTIVE
        );
        
        if (taskIds == null || taskIds.isEmpty()) {
            return;
        }
        
        for (String taskId : taskIds) {
            try {
                // 获取任务创建时间
                String startTimeStr = (String) stringRedisTemplate.opsForHash().get(
                    TranslationTaskRedisKeyConstants.TRANSLATION_TASK_START_TIMES, 
                    taskId
                );
                
                if (startTimeStr != null) {
                    Instant startTime = Instant.ofEpochMilli(Long.parseLong(startTimeStr));
                    
                    if (startTime.isBefore(cutoff)) {
                        TaskInfo task = getTask(taskId);
                        if (task != null) {
                            String status = task.getStatus();
                            if ("completed".equals(status) || "failed".equals(status)) {
                                removeTask(taskId);
                                removedCount++;
                                log.debug("清理过期任务: {}, 状态: {}, 创建时间: {}", 
                                         taskId, status, startTime);
                            }
                        } else {
                            // 任务不存在，从集合中移除
                            stringRedisTemplate.opsForSet().remove(
                                TranslationTaskRedisKeyConstants.TRANSLATION_TASKS_ACTIVE, 
                                taskId
                            );
                            stringRedisTemplate.opsForHash().delete(
                                TranslationTaskRedisKeyConstants.TRANSLATION_TASK_START_TIMES, 
                                taskId
                            );
                        }
                    }
                }
            } catch (Exception e) {
                log.error("清理任务 {} 时发生错误: {}", taskId, e.getMessage(), e);
            }
        }
        
        if (removedCount > 0) {
            log.info("定时清理完成，删除 {} 个过期任务", removedCount);
        }
    }
}