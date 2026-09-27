package cn.iocoder.sva.module.ai.service.translation.helper;

import cn.iocoder.sva.module.ai.service.translation.tran.config.TransDocProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STSectionMark;
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
import java.math.BigInteger;

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

            for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
                JsonNode page = pages.get(pageIndex);
                boolean landscape = pageNeedsLandscape(page);
                List<JsonNode> blocks = new ArrayList<>();
                page.path("blocks").forEach(blocks::add);
                blocks.sort(Comparator.comparingInt(ConvertByPythonHelper::readingOrder));
                for (JsonNode block : blocks) {
                    String text = block.path("content").path("text").asText("").trim();
                    if (text.isEmpty()) {
                        continue;
                    }
                    if (isHtmlTable(block, text)) {
                        appendHtmlTable(wordDocument, text);
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
                if (pageIndex < pages.size() - 1) {
                    XWPFParagraph sectionBreak = wordDocument.createParagraph();
                    CTSectPr section = sectionBreak.getCTP().addNewPPr().addNewSectPr();
                    configureSection(section, page, landscape);
                    section.addNewType().setVal(STSectionMark.NEXT_PAGE);
                } else {
                    CTSectPr section = wordDocument.getDocument().getBody().isSetSectPr()
                            ? wordDocument.getDocument().getBody().getSectPr()
                            : wordDocument.getDocument().getBody().addNewSectPr();
                    configureSection(section, page, landscape);
                }
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

    private static boolean isHtmlTable(JsonNode block, String text) {
        return ("table".equalsIgnoreCase(block.path("type").asText())
                || "table".equalsIgnoreCase(block.path("content").path("kind").asText()))
                && text.toLowerCase().contains("<table");
    }

    private static boolean pageNeedsLandscape(JsonNode page) {
        if (page.path("width").asDouble() > page.path("height").asDouble()) {
            return true;
        }
        for (JsonNode block : page.path("blocks")) {
            String text = block.path("content").path("text").asText("");
            if (isHtmlTable(block, text)) {
                Element table = Jsoup.parseBodyFragment(text).selectFirst("table");
                if (table != null) {
                    int columns = table.select("tr").stream()
                            .mapToInt(row -> directCells(row).stream()
                                    .mapToInt(cell -> positiveSpan(cell.attr("colspan")))
                                    .sum())
                            .max().orElse(1);
                    if (columns >= 5) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void configureSection(CTSectPr section, JsonNode page, boolean landscape) {
        long width = Math.max(1L, Math.round(page.path("width").asDouble(595) * 20));
        long height = Math.max(1L, Math.round(page.path("height").asDouble(842) * 20));
        if (landscape && width < height) {
            long swap = width;
            width = height;
            height = swap;
        } else if (!landscape && width > height) {
            long swap = width;
            width = height;
            height = swap;
        }
        CTPageSz pageSize = section.isSetPgSz() ? section.getPgSz() : section.addNewPgSz();
        pageSize.setW(BigInteger.valueOf(width));
        pageSize.setH(BigInteger.valueOf(height));
        pageSize.setOrient(landscape ? STPageOrientation.LANDSCAPE : STPageOrientation.PORTRAIT);
    }

    private static void appendHtmlTable(XWPFDocument document, String html) {
        Element sourceTable = Jsoup.parseBodyFragment(html).selectFirst("table");
        if (sourceTable == null) {
            document.createParagraph().createRun().setText(html);
            return;
        }
        List<Element> sourceRows = sourceTable.select("tr");
        int columnCount = sourceRows.stream()
                .mapToInt(row -> directCells(row).stream()
                        .mapToInt(cell -> positiveSpan(cell.attr("colspan")))
                        .sum())
                .max()
                .orElse(1);
        if (sourceRows.isEmpty() || columnCount <= 0) {
            return;
        }

        XWPFTable table = document.createTable(sourceRows.size(), columnCount);
        table.setWidth("100%");
        CTTblGrid tableGrid = table.getCTTbl().getTblGrid();
        if (tableGrid == null) {
            tableGrid = table.getCTTbl().addNewTblGrid();
        }
        if (tableGrid.sizeOfGridColArray() == 0) {
            BigInteger columnWidth = BigInteger.valueOf(Math.max(1, 9000 / columnCount));
            for (int column = 0; column < columnCount; column++) {
                tableGrid.addNewGridCol().setW(columnWidth);
            }
        }
        table.getRow(0).setRepeatHeader(true);
        boolean[][] occupied = new boolean[sourceRows.size()][columnCount];
        for (int rowIndex = 0; rowIndex < sourceRows.size(); rowIndex++) {
            int columnIndex = 0;
            for (Element sourceCell : directCells(sourceRows.get(rowIndex))) {
                while (columnIndex < columnCount && occupied[rowIndex][columnIndex]) {
                    columnIndex++;
                }
                if (columnIndex >= columnCount) {
                    break;
                }
                int rowSpan = Math.min(positiveSpan(sourceCell.attr("rowspan")), sourceRows.size() - rowIndex);
                int columnSpan = Math.min(positiveSpan(sourceCell.attr("colspan")), columnCount - columnIndex);
                XWPFTableCell target = table.getRow(rowIndex).getCell(columnIndex);
                target.setText(sourceCell.text().replace("\\n", "\n").strip());

                if (columnSpan > 1) {
                    mergeHorizontally(table, rowIndex, columnIndex, columnIndex + columnSpan - 1);
                }
                if (rowSpan > 1) {
                    mergeVertically(table, columnIndex, rowIndex, rowIndex + rowSpan - 1, columnSpan);
                }
                for (int r = rowIndex; r < rowIndex + rowSpan; r++) {
                    for (int c = columnIndex; c < columnIndex + columnSpan; c++) {
                        occupied[r][c] = true;
                    }
                }
                columnIndex += columnSpan;
            }
        }
        if (columnCount >= 5) {
            table.getRows().forEach(row -> row.getTableCells().forEach(cell ->
                    cell.getParagraphs().forEach(paragraph -> paragraph.getRuns()
                            .forEach(run -> run.setFontSize(8)))));
        }
    }

    private static int positiveSpan(String value) {
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private static List<Element> directCells(Element row) {
        return row.children().stream()
                .filter(child -> "td".equals(child.normalName()) || "th".equals(child.normalName()))
                .toList();
    }

    private static void mergeHorizontally(XWPFTable table, int row, int fromColumn, int toColumn) {
        for (int column = fromColumn; column <= toColumn; column++) {
            XWPFTableCell cell = table.getRow(row).getCell(column);
            cell.getCTTc().addNewTcPr().addNewHMerge()
                    .setVal(column == fromColumn ? STMerge.RESTART : STMerge.CONTINUE);
        }
    }

    private static void mergeVertically(XWPFTable table, int column, int fromRow, int toRow, int columnSpan) {
        for (int row = fromRow; row <= toRow; row++) {
            for (int offset = 0; offset < columnSpan; offset++) {
                XWPFTableCell cell = table.getRow(row).getCell(column + offset);
                cell.getCTTc().addNewTcPr().addNewVMerge()
                        .setVal(row == fromRow ? STMerge.RESTART : STMerge.CONTINUE);
            }
        }
    }
}
