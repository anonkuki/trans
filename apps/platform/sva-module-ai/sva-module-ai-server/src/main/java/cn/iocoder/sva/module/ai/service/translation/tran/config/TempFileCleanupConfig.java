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
import java.util.stream.Stream;

@Slf4j
@Component
public class TempFileCleanupConfig {

    @Autowired
    private TransDocProperties properties;

    @Scheduled(fixedRate = 86400000)
    public void cleanupTempFiles() {
        Path tempDir = Paths.get(properties.getTempDir());

        if (!Files.exists(tempDir) || !Files.isDirectory(tempDir)) {
            log.warn("临时目录不存在或不是目录: {}", tempDir);
            return;
        }

        int deletedCount = 0;
        int errorCount = 0;

        try (Stream<Path> files = Files.list(tempDir)) {
            deletedCount = files.filter(p -> {
                String filename = p.getFileName().toString();
                return filename.startsWith("input_") ||
                       filename.startsWith("output_") ||
                       filename.startsWith("compare_") ||
                       filename.startsWith("import_");
            })
            .filter(p -> {
                try {
                    Instant lastModified = Files.getLastModifiedTime(p).toInstant();
                    return lastModified.isBefore(Instant.now().minus(Duration.ofHours(2)));
                } catch (IOException e) {
                    log.debug("获取文件修改时间失败: {}", p, e);
                    return false;
                }
            })
            .mapToInt(p -> {
                try {
                    Files.delete(p);
                    log.info("清理过期临时文件: {}", p.getFileName());
                    return 1;
                } catch (IOException e) {
                    log.error("删除临时文件失败: {}", p, e);
                    return 0;
                }
            })
            .sum();

        } catch (IOException e) {
            log.error("扫描临时目录失败: {}", tempDir, e);
            return;
        }

        if (deletedCount > 0) {
            log.info("临时文件清理完成，删除 {} 个文件", deletedCount);
        } else {
            log.debug("没有需要清理的临时文件");
        }
    }
}
