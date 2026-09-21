package cn.iocoder.sva.module.ai.service.translation.tran.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
public class TempFileCleanupConfig {

    @Autowired
    private TransDocProperties properties;

    /**
     * 定时清理过期临时文件（兜底机制）
     * <p>
     * 正常情况下，翻译任务完成后会在 finally 块中立即清理临时文件。
     * 此定时任务仅用于清理因异常情况未被删除的残留文件。
     * <p>
     * 执行频率：每6小时执行一次
     * 清理条件：文件修改时间超过2小时
     */
    @Scheduled(cron = "0 0 */6 * * ?")  // 每6小时执行一次
    public void cleanupTempFiles() {
        log.debug("========== 开始执行临时文件清理任务 ==========");
        long startTime = System.currentTimeMillis();

        Path tempDir = Paths.get(properties.getTempDir());

        if (!Files.exists(tempDir) || !Files.isDirectory(tempDir)) {
            log.warn("临时目录不存在或不是目录: {}", tempDir);
            return;
        }

        log.debug("临时目录: {}", tempDir.toAbsolutePath());

        int deletedCount = 0;
        int errorCount = 0;
        int skippedCount = 0;

        try (Stream<Path> files = Files.list(tempDir)) {
            List<Path> candidateFiles = files.filter(p -> {
                String filename = p.getFileName().toString();
                return filename.startsWith("input_") ||
                       filename.startsWith("output_") ||
                       filename.startsWith("compare_") ||
                       filename.startsWith("import_") ||
                       filename.startsWith("ocr-recognized-");
            })
            .filter(p -> {
                try {
                    Instant lastModified = Files.getLastModifiedTime(p).toInstant();
                    // 只清理超过2小时的临时文件（作为兜底机制）
                    return lastModified.isBefore(Instant.now().minus(Duration.ofHours(2)));
                } catch (IOException e) {
                    log.debug("获取文件修改时间失败: {}", p, e);
                    return false;
                }
            })
            .collect(java.util.stream.Collectors.toList());

            log.debug("找到 {} 个候选文件需要清理", candidateFiles.size());

            // 逐个删除文件
            for (Path p : candidateFiles) {
                try {
                    long fileSize = Files.size(p);
                    Instant lastModified = Files.getLastModifiedTime(p).toInstant();
                    java.time.LocalDateTime modifiedTime = java.time.LocalDateTime.ofInstant(
                            lastModified, java.time.ZoneId.systemDefault());

                    Files.delete(p);
                    deletedCount++;
                    log.debug("✅ 清理过期临时文件: {} (大小: {} bytes, 修改时间: {})",
                            p.getFileName(), fileSize, modifiedTime);
                } catch (IOException e) {
                    errorCount++;
                    log.error("❌ 删除临时文件失败: {}, 错误: {}", p, e.getMessage());
                }
            }

        } catch (IOException e) {
            log.error("扫描临时目录失败: {}", tempDir, e);
            return;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("========== 临时文件清理完成 ==========");
        log.info("  - 删除文件数: {}", deletedCount);
        if (errorCount > 0) {
            log.warn("  - 失败文件数: {}", errorCount);
        }
        log.debug("  - 跳过文件数: {}", skippedCount);
        log.debug("  - 耗时: {} ms", elapsed);

        if (deletedCount > 0) {
            log.info("临时文件清理完成，共删除 {} 个文件", deletedCount);
        } else {
            log.debug("没有需要清理的临时文件");
        }
    }
}
