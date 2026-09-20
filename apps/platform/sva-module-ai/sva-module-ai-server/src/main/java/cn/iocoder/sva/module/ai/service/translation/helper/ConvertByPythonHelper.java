package cn.iocoder.sva.module.ai.service.translation.helper;

import cn.iocoder.sva.module.ai.service.translation.tran.config.TransDocProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class ConvertByPythonHelper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestTemplate restTemplate;
    private final TransDocProperties properties;

    public ConvertByPythonHelper(RestTemplateBuilder restTemplateBuilder, TransDocProperties properties) {
        this(restTemplateBuilder
                .connectTimeout(properties.getPythonConnectTimeout())
                .readTimeout(properties.getPythonReadTimeout())
                .build(), properties);
    }

    ConvertByPythonHelper(RestTemplate restTemplate, TransDocProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public File convertPdfToDocx(File pdfFile) throws IOException {
        if (!StringUtils.hasText(properties.getPythonInternalToken())) {
            throw new IllegalStateException("文档引擎内部令牌未配置");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-Internal-Token", properties.getPythonInternalToken());

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(pdfFile));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                properties.getPythonRecognizeUrl(),
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("PDF OCR失败: " + response.getStatusCode());
        }
        JsonNode root = OBJECT_MAPPER.readTree(response.getBody());
        return writeDocumentV1ToDocx(root.path("document"));
    }

    private File writeDocumentV1ToDocx(JsonNode documentNode) throws IOException {
        if (!"normalized_document_v1".equals(documentNode.path("schema").asText())
                || !"1.1".equals(documentNode.path("schema_version").asText())
                || !documentNode.path("pages").isArray()) {
            throw new IllegalStateException("文档引擎未返回受支持的 document.v1 结构");
        }

        Path tempDirectory = Path.of(properties.getTempDir());
        Files.createDirectories(tempDirectory);
        Path output = Files.createTempFile(tempDirectory, "ocr-recognized-", ".docx");
        try (XWPFDocument wordDocument = new XWPFDocument();
             FileOutputStream outputStream = new FileOutputStream(output.toFile())) {
            List<JsonNode> pages = new ArrayList<>();
            documentNode.path("pages").forEach(pages::add);
            pages.sort(Comparator.comparingInt(page -> page.path("page_index").asInt()));

            boolean firstPage = true;
            for (JsonNode page : pages) {
                if (!firstPage) {
                    wordDocument.createParagraph().setPageBreak(true);
                }
                List<JsonNode> blocks = new ArrayList<>();
                page.path("blocks").forEach(blocks::add);
                blocks.sort(Comparator.comparingInt(ConvertByPythonHelper::readingOrder));
                for (JsonNode block : blocks) {
                    String text = block.path("content").path("text").asText("").trim();
                    if (text.isEmpty()) {
                        continue;
                    }
                    XWPFParagraph paragraph = wordDocument.createParagraph();
                    String role = block.path("layout_role").asText("");
                    if ("title".equals(role)) {
                        paragraph.setStyle("Title");
                    } else if ("heading".equals(role)) {
                        paragraph.setStyle("Heading1");
                    }
                    paragraph.createRun().setText(text);
                }
                firstPage = false;
            }
            wordDocument.write(outputStream);
        } catch (Exception exception) {
            Files.deleteIfExists(output);
            throw exception;
        }
        return output.toFile();
    }

    private static int readingOrder(JsonNode block) {
        JsonNode readingOrder = block.path("reading_order");
        return readingOrder.isIntegralNumber() ? readingOrder.asInt() : block.path("order").asInt();
    }
}
