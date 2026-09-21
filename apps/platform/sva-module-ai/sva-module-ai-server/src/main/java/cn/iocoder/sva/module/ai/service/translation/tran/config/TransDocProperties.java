package cn.iocoder.sva.module.ai.service.translation.tran.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "transdoc")
public class TransDocProperties {

    /**
     * Glossary file path
     */
    private String glossaryPath = "glossary.json";

    /**
     * Temporary directory for file processing
     */
    private String tempDir = System.getProperty("java.io.tmpdir");

    /**
     * Concurrency level for document processing
     */
    private int concurrency = 6;

    /**
     * PDF转Word的模式：0或空表示使用PDFBox（默认），1表示使用Python服务
     */
    private Integer pdfConvertMode = 0;

    /**
     * Python 文档引擎的内部 OCR URL。接口返回 document.v1，由系统1生成中间 DOCX 并继续翻译。
     */
    private String pythonRecognizeUrl = "http://localhost:8030/internal/v1/ocr/recognize";

    /**
     * 系统1调用文档引擎使用的内部令牌，仅允许通过服务端配置注入。
     */
    private String pythonInternalToken;

    /**
     * 文档引擎连接超时。
     */
    private Duration pythonConnectTimeout = Duration.ofSeconds(10);

    /**
     * 文档引擎读取超时，应略大于服务端 OCR 轮询上限。
     */
    private Duration pythonReadTimeout = Duration.ofMinutes(31);

    /**
     * Whether a failed Python OCR request may fall back to PDFBox. Disabled by default so
     * authentication, transport, or schema errors cannot silently change recognition behavior.
     */
    private boolean pythonFallbackEnabled = false;

    /**
     * 默认翻译角色名称（从 Nacos 配置中心获取）
     * 如果前端未传入 roleId，则使用此角色名称查询角色并获取提示词
     */
    private String defaultRoleName;

}
