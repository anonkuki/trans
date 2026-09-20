package cn.iocoder.sva.module.ai.service.translation.helper;

import cn.iocoder.sva.module.ai.service.translation.tran.config.TransDocProperties;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ConvertByPythonHelperTest {

    @Test
    void shouldCallProtectedOcrEndpointAndBuildOrderedDocx() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        TransDocProperties properties = new TransDocProperties();
        properties.setPythonRecognizeUrl("http://document-engine/internal/v1/ocr/recognize");
        properties.setPythonInternalToken("internal-secret");

        String response = """
                {
                  "contract_version":"1.0",
                  "request_id":"req-1",
                  "provider":"paddle",
                  "document":{
                    "schema":"normalized_document_v1",
                    "schema_version":"1.1",
                    "document_id":"req-1",
                    "source":{"provider":"paddle"},
                    "page_count":4,
                    "pages":[
                      {"page_index":1,"width":595,"height":842,"unit":"pt","blocks":[
                        {"block_id":"b3","page_index":1,"order":2,"type":"text",
                         "geometry":{"bbox":[0,0,1,1]},"content":{"kind":"text","text":"Second page"},
                         "layout_role":"paragraph","semantic_role":"body","structure_role":"unknown",
                         "policy":{"translate":true,"translate_reason":""},
                         "provenance":{"provider":"paddle","raw_label":"","raw_sub_type":"","raw_bbox":[0,0,1,1],"raw_path":""},
                         "metadata":{},"source":{"provider":"paddle"},
                         "continuation_hint":{"source":"","group_id":"","role":"","scope":"","reading_order":0,"confidence":1.0}}
                      ]},
                      {"page_index":0,"width":595,"height":842,"unit":"pt","blocks":[
                        {"block_id":"b2","page_index":0,"order":1,"reading_order":2,"type":"text",
                         "geometry":{"bbox":[0,0,1,1]},"content":{"kind":"text","text":"Body"},
                         "layout_role":"paragraph","semantic_role":"body","structure_role":"unknown",
                         "policy":{"translate":true,"translate_reason":""},
                         "provenance":{"provider":"paddle","raw_label":"","raw_sub_type":"","raw_bbox":[0,0,1,1],"raw_path":""},
                         "metadata":{},"source":{"provider":"paddle"},
                         "continuation_hint":{"source":"","group_id":"","role":"","scope":"","reading_order":0,"confidence":1.0}},
                        {"block_id":"b1","page_index":0,"order":2,"reading_order":1,"type":"text",
                         "geometry":{"bbox":[0,0,1,1]},"content":{"kind":"text","text":"Heading"},
                         "layout_role":"heading","semantic_role":"body","structure_role":"unknown",
                         "policy":{"translate":true,"translate_reason":""},
                         "provenance":{"provider":"paddle","raw_label":"","raw_sub_type":"","raw_bbox":[0,0,1,1],"raw_path":""},
                         "metadata":{},"source":{"provider":"paddle"},
                         "continuation_hint":{"source":"","group_id":"","role":"","scope":"","reading_order":0,"confidence":1.0}}
                      ]},
                      {"page_index":2,"width":595,"height":842,"unit":"pt","blocks":[]},
                      {"page_index":3,"width":595,"height":842,"unit":"pt","blocks":[]}
                    ],
                    "derived":{},
                    "markers":{}
                  }
                }
                """;
        server.expect(once(), requestTo(properties.getPythonRecognizeUrl()))
                .andExpect(header("X-Internal-Token", "internal-secret"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        File pdf = File.createTempFile("ocr-client-", ".pdf");
        Files.writeString(pdf.toPath(), "%PDF-1.4\n");
        File docx = null;
        try {
            ConvertByPythonHelper helper = new ConvertByPythonHelper(restTemplate, properties);
            docx = helper.convertPdfToDocx(pdf);

            try (XWPFDocument document = new XWPFDocument(Files.newInputStream(docx.toPath()))) {
                List<String> paragraphs = document.getParagraphs().stream()
                        .map(paragraph -> paragraph.getText())
                        .filter(text -> !text.isBlank())
                        .toList();
                assertThat(paragraphs).containsExactly("Heading", "Body", "Second page");
                assertThat(document.getParagraphs().stream().filter(XWPFParagraph::isPageBreak).count())
                        .isEqualTo(3);
            }
            server.verify();
        } finally {
            Files.deleteIfExists(pdf.toPath());
            if (docx != null) {
                Files.deleteIfExists(docx.toPath());
            }
        }
    }

    @Test
    void shouldRejectUnexpectedDocumentSchema() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        TransDocProperties properties = new TransDocProperties();
        properties.setPythonRecognizeUrl("http://document-engine/internal/v1/ocr/recognize");
        properties.setPythonInternalToken("internal-secret");
        server.expect(requestTo(properties.getPythonRecognizeUrl()))
                .andRespond(withSuccess("{\"document\":{\"schema\":\"unknown\"}}", MediaType.APPLICATION_JSON));

        File pdf = File.createTempFile("ocr-client-", ".pdf");
        try {
            ConvertByPythonHelper helper = new ConvertByPythonHelper(restTemplate, properties);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> helper.convertPdfToDocx(pdf))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("document.v1");
        } finally {
            Files.deleteIfExists(pdf.toPath());
        }
    }

    @Test
    void shouldSurfaceDocumentEngineTimeoutForSystemOneFallback() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        TransDocProperties properties = new TransDocProperties();
        properties.setPythonRecognizeUrl("http://document-engine/internal/v1/ocr/recognize");
        properties.setPythonInternalToken("internal-secret");
        server.expect(requestTo(properties.getPythonRecognizeUrl()))
                .andRespond(request -> {
                    throw new ResourceAccessException("Read timed out");
                });

        File pdf = File.createTempFile("ocr-client-", ".pdf");
        try {
            ConvertByPythonHelper helper = new ConvertByPythonHelper(restTemplate, properties);
            assertThatThrownBy(() -> helper.convertPdfToDocx(pdf))
                    .isInstanceOf(ResourceAccessException.class)
                    .hasMessageContaining("timed out");
        } finally {
            Files.deleteIfExists(pdf.toPath());
        }
    }
}
