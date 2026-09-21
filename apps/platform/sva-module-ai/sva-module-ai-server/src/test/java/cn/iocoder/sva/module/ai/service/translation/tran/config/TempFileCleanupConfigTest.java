package cn.iocoder.sva.module.ai.service.translation.tran.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;

class TempFileCleanupConfigTest {

    @Test
    void removesExpiredOcrRecognizedDocument(@TempDir Path tempDir) throws Exception {
        Path recognized = Files.writeString(tempDir.resolve("ocr-recognized-test.docx"), "sensitive text");
        Files.setLastModifiedTime(recognized, FileTime.from(Instant.now().minusSeconds(3 * 60 * 60)));

        TransDocProperties properties = new TransDocProperties();
        properties.setTempDir(tempDir.toString());
        TempFileCleanupConfig cleanup = new TempFileCleanupConfig();
        ReflectionTestUtils.setField(cleanup, "properties", properties);

        cleanup.cleanupTempFiles();

        assertFalse(Files.exists(recognized));
    }
}
