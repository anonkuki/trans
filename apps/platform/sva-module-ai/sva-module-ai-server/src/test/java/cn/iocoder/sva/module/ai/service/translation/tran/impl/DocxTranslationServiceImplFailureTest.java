package cn.iocoder.sva.module.ai.service.translation.tran.impl;

import cn.iocoder.sva.module.ai.service.translation.tran.DocxTranslationService.TranslationResult;
import cn.iocoder.sva.module.ai.service.translation.tran.GlossaryService;
import cn.iocoder.sva.module.ai.service.translation.tran.LlmClientService;
import cn.iocoder.sva.module.ai.service.translation.tran.QcService;
import cn.iocoder.sva.module.ai.service.translation.tran.config.TransDocProperties;
import cn.iocoder.sva.module.ai.service.translation.tran.config.TranslationModeConfig;
import cn.iocoder.sva.module.ai.service.translation.tran.model.UsageStats;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DocxTranslationServiceImplFailureTest {

    @TempDir
    Path tempDir;

    @Test
    void targetChineseDetectionMustNotSkipEnglishSentenceWithOneCjkCharacter() {
        DocxTranslationServiceImpl service = new DocxTranslationServiceImpl(
                new TransDocProperties(), mock(LlmClientService.class),
                mock(GlossaryService.class), mock(QcService.class));

        Boolean mixedEnglish = ReflectionTestUtils.invokeMethod(service, "isPureTarget",
                "According to the source of the test product, prepare the test sample tube.依", "Chinese");
        Boolean chineseWithIdentifiers = ReflectionTestUtils.invokeMethod(service, "isPureTarget",
                "按照SOP和GMP要求执行。", "Chinese");

        assertEquals(false, mixedEnglish);
        assertEquals(true, chineseWithIdentifiers);
    }

    @Test
    void modelFailureMustNotBeReportedAsSuccessfulTranslation() throws Exception {
        Path input = tempDir.resolve("input.docx");
        try (XWPFDocument document = new XWPFDocument();
             OutputStream output = Files.newOutputStream(input)) {
            document.createParagraph().createRun().setText("需要翻译的受控文本");
            document.write(output);
        }

        LlmClientService llmClient = mock(LlmClientService.class);
        LlmClientService.TranslateResult failed = new LlmClientService.TranslateResult(
                "", new UsageStats(), "[ERROR] upstream rejected request");
        LlmClientService.TranslateResult preserved = new LlmClientService.TranslateResult(
                "需要翻译的受控文本", new UsageStats(), "[ERROR] upstream rejected request");
        when(llmClient.cachedCall(any(), anyString(), anyString())).thenReturn(failed);
        when(llmClient.sanitizeOrRetry(anyString(), anyString(), anyString(), any())).thenReturn(preserved);

        TranslationModeConfig modeConfig = mock(TranslationModeConfig.class);
        when(modeConfig.isConstraintMode()).thenReturn(false);
        TransDocProperties properties = new TransDocProperties();
        properties.setConcurrency(1);
        DocxTranslationServiceImpl service = new DocxTranslationServiceImpl(
                properties, llmClient, mock(GlossaryService.class), mock(QcService.class));
        ReflectionTestUtils.setField(service, "translationModeConfig", modeConfig);

        TranslationResult result = service.processDocument(
                input.toString(), tempDir.resolve("output.docx").toString(), "English",
                false, Map.of(), null, null, true, false, false, false);

        assertEquals(1, result.getErrors());
        assertTrue(result.getPairs().get(0).getStatus().startsWith("[ERROR]"));
    }

    @Test
    void repeatedParagraphsShareOneInFlightModelCall() throws Exception {
        Path input = tempDir.resolve("repeated.docx");
        try (XWPFDocument document = new XWPFDocument();
             OutputStream output = Files.newOutputStream(input)) {
            document.createParagraph().createRun().setText("重复的受控文本");
            document.createParagraph().createRun().setText("重复的受控文本");
            document.write(output);
        }

        LlmClientService llmClient = mock(LlmClientService.class);
        LlmClientService.TranslateResult translated = new LlmClientService.TranslateResult(
                "Repeated controlled text", new UsageStats(), "未命中");
        when(llmClient.cachedCall(any(), anyString(), anyString())).thenReturn(translated);
        when(llmClient.sanitizeOrRetry(anyString(), anyString(), anyString(), any())).thenReturn(translated);

        TranslationModeConfig modeConfig = mock(TranslationModeConfig.class);
        when(modeConfig.isConstraintMode()).thenReturn(false);
        TransDocProperties properties = new TransDocProperties();
        properties.setConcurrency(4);
        DocxTranslationServiceImpl service = new DocxTranslationServiceImpl(
                properties, llmClient, mock(GlossaryService.class), mock(QcService.class));
        ReflectionTestUtils.setField(service, "translationModeConfig", modeConfig);

        TranslationResult result = service.processDocument(
                input.toString(), tempDir.resolve("repeated-output.docx").toString(), "English",
                false, Map.of(), null, null, true, false, false, false);

        assertEquals(0, result.getErrors());
        verify(llmClient, times(1)).cachedCall(any(), anyString(), anyString());
    }

    @Test
    void transientModelFailureIsRetriedBeforeDocumentIsMarkedFailed() throws Exception {
        Path input = tempDir.resolve("retry.docx");
        try (XWPFDocument document = new XWPFDocument();
             OutputStream output = Files.newOutputStream(input)) {
            document.createParagraph().createRun().setText("需要重试的受控文本");
            document.write(output);
        }

        LlmClientService llmClient = mock(LlmClientService.class);
        LlmClientService.TranslateResult failed = new LlmClientService.TranslateResult(
                "需要重试的受控文本", new UsageStats(), "[ERROR] read timed out");
        LlmClientService.TranslateResult translated = new LlmClientService.TranslateResult(
                "Controlled text requiring retry", new UsageStats(), "未命中");
        when(llmClient.cachedCall(any(), anyString(), anyString())).thenReturn(failed, translated);
        when(llmClient.sanitizeOrRetry(anyString(), anyString(), anyString(), any()))
                .thenReturn(failed, translated);

        TranslationModeConfig modeConfig = mock(TranslationModeConfig.class);
        when(modeConfig.isConstraintMode()).thenReturn(false);
        TransDocProperties properties = new TransDocProperties();
        properties.setConcurrency(1);
        DocxTranslationServiceImpl service = new DocxTranslationServiceImpl(
                properties, llmClient, mock(GlossaryService.class), mock(QcService.class));
        ReflectionTestUtils.setField(service, "translationModeConfig", modeConfig);

        TranslationResult result = service.processDocument(
                input.toString(), tempDir.resolve("retry-output.docx").toString(), "English",
                false, Map.of(), null, null, true, false, false, false);

        assertEquals(0, result.getErrors());
        verify(llmClient, times(2)).cachedCall(any(), anyString(), anyString());
    }

    @Test
    void numericLossUsesStrictDigitPreservingCorrection() throws Exception {
        Path input = tempDir.resolve("numeric.docx");
        try (XWPFDocument document = new XWPFDocument();
             OutputStream output = Files.newOutputStream(input)) {
            document.createParagraph().createRun().setText("平行制备2份");
            document.write(output);
        }

        LlmClientService llmClient = mock(LlmClientService.class);
        LlmClientService.TranslateResult spelledOut = new LlmClientService.TranslateResult(
                "Prepare two copies in parallel", new UsageStats(), "OK");
        LlmClientService.TranslateResult corrected = new LlmClientService.TranslateResult(
                "Prepare 2 copies in parallel", new UsageStats(), "OK");
        when(llmClient.cachedCall(eq(LlmClientService.CallKind.NORMAL), anyString(), anyString()))
                .thenReturn(spelledOut);
        when(llmClient.cachedCall(eq(LlmClientService.CallKind.STRICT), anyString(), anyString()))
                .thenReturn(corrected);
        when(llmClient.sanitizeOrRetry(anyString(), anyString(), anyString(), any()))
                .thenAnswer(invocation -> new LlmClientService.TranslateResult(
                        invocation.getArgument(2), new UsageStats(), "OK"));

        TranslationModeConfig modeConfig = mock(TranslationModeConfig.class);
        when(modeConfig.isConstraintMode()).thenReturn(false);
        TransDocProperties properties = new TransDocProperties();
        properties.setConcurrency(1);
        DocxTranslationServiceImpl service = new DocxTranslationServiceImpl(
                properties, llmClient, mock(GlossaryService.class), mock(QcService.class));
        ReflectionTestUtils.setField(service, "translationModeConfig", modeConfig);

        TranslationResult result = service.processDocument(
                input.toString(), tempDir.resolve("numeric-output.docx").toString(), "English",
                false, Map.of(), null, null, true, false, false, false);

        assertEquals(0, result.getErrors());
        verify(llmClient).cachedCall(eq(LlmClientService.CallKind.STRICT), anyString(), eq("English"));
    }

    @Test
    void constraintModeAlsoUsesStrictDigitPreservingCorrection() throws Exception {
        Path input = tempDir.resolve("constraint-numeric.docx");
        try (XWPFDocument document = new XWPFDocument();
             OutputStream output = Files.newOutputStream(input)) {
            document.createParagraph().createRun().setText("平行制备2份");
            document.write(output);
        }

        LlmClientService llmClient = mock(LlmClientService.class);
        LlmClientService.TranslateResult spelledOut = new LlmClientService.TranslateResult(
                "Prepare two copies in parallel", new UsageStats(), "OK");
        LlmClientService.TranslateResult corrected = new LlmClientService.TranslateResult(
                "Prepare 2 copies in parallel", new UsageStats(), "OK");
        when(llmClient.cachedCall(eq(LlmClientService.CallKind.NORMAL), anyString(), anyString()))
                .thenReturn(spelledOut);
        when(llmClient.cachedCall(eq(LlmClientService.CallKind.STRICT), anyString(), anyString()))
                .thenReturn(corrected);
        when(llmClient.sanitizeOrRetry(anyString(), anyString(), anyString(), any()))
                .thenAnswer(invocation -> new LlmClientService.TranslateResult(
                        invocation.getArgument(2), new UsageStats(), "OK"));

        TranslationModeConfig modeConfig = mock(TranslationModeConfig.class);
        when(modeConfig.isConstraintMode()).thenReturn(true);
        TransDocProperties properties = new TransDocProperties();
        properties.setConcurrency(1);
        DocxTranslationServiceImpl service = new DocxTranslationServiceImpl(
                properties, llmClient, mock(GlossaryService.class), mock(QcService.class));
        ReflectionTestUtils.setField(service, "translationModeConfig", modeConfig);

        TranslationResult result = service.processDocument(
                input.toString(), tempDir.resolve("constraint-numeric-output.docx").toString(), "English",
                false, Map.of(), null, null, true, false, false, false);

        assertEquals(0, result.getErrors());
        verify(llmClient).cachedCall(eq(LlmClientService.CallKind.STRICT), anyString(), eq("English"));
    }

    @Test
    void strictFormattingTranslatesTheWholeParagraphOnceInsteadOfRunFragments() throws Exception {
        Path input = tempDir.resolve("multi-run.docx");
        try (XWPFDocument document = new XWPFDocument();
             OutputStream output = Files.newOutputStream(input)) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.createRun().setText("取样");
            paragraph.createRun().setBold(true);
            paragraph.getRuns().get(1).setText("后立即检测");
            document.write(output);
        }

        LlmClientService llmClient = mock(LlmClientService.class);
        LlmClientService.TranslateResult translated = new LlmClientService.TranslateResult(
                "Test immediately after sampling", new UsageStats(), "OK");
        when(llmClient.cachedCall(eq(LlmClientService.CallKind.NORMAL), anyString(), eq("English")))
                .thenReturn(translated);
        when(llmClient.sanitizeOrRetry(anyString(), anyString(), anyString(), any())).thenReturn(translated);

        TranslationModeConfig modeConfig = mock(TranslationModeConfig.class);
        when(modeConfig.isConstraintMode()).thenReturn(false);
        TransDocProperties properties = new TransDocProperties();
        properties.setConcurrency(1);
        DocxTranslationServiceImpl service = new DocxTranslationServiceImpl(
                properties, llmClient, mock(GlossaryService.class), mock(QcService.class));
        ReflectionTestUtils.setField(service, "translationModeConfig", modeConfig);

        TranslationResult result = service.processDocument(
                input.toString(), tempDir.resolve("multi-run-output.docx").toString(), "English",
                false, Map.of(), null, null, true, false, false, false);

        assertEquals(0, result.getErrors());
        verify(llmClient, times(1)).cachedCall(
                eq(LlmClientService.CallKind.NORMAL), eq("取样后立即检测"), eq("English"));
    }

    @Test
    void missingRequiredGlossaryTermTriggersStrictCorrection() throws Exception {
        Path input = tempDir.resolve("glossary.docx");
        try (XWPFDocument document = new XWPFDocument();
             OutputStream output = Files.newOutputStream(input)) {
            document.createParagraph().createRun().setText("水痘减毒活疫苗应冷藏保存");
            document.write(output);
        }

        LlmClientService llmClient = mock(LlmClientService.class);
        LlmClientService.TranslateResult missingTerm = new LlmClientService.TranslateResult(
                "The live vaccine shall be refrigerated", new UsageStats(), "OK");
        LlmClientService.TranslateResult corrected = new LlmClientService.TranslateResult(
                "Varicella Vaccine, Live shall be refrigerated", new UsageStats(), "OK");
        when(llmClient.translateWithGlossaryConstraint(anyString(), eq("English"), any()))
                .thenReturn(missingTerm);
        when(llmClient.cachedCall(eq(LlmClientService.CallKind.STRICT), anyString(), eq("English")))
                .thenReturn(corrected);
        when(llmClient.sanitizeOrRetry(anyString(), anyString(), anyString(), any()))
                .thenAnswer(invocation -> new LlmClientService.TranslateResult(
                        invocation.getArgument(2), new UsageStats(), "OK"));

        TranslationModeConfig modeConfig = mock(TranslationModeConfig.class);
        when(modeConfig.isConstraintMode()).thenReturn(true);
        TransDocProperties properties = new TransDocProperties();
        properties.setConcurrency(1);
        GlossaryService glossaryService = mock(GlossaryService.class);
        when(glossaryService.replaceTermsMaxCover(anyString(), any()))
                .thenAnswer(invocation -> {
                    String source = invocation.getArgument(0);
                    return new GlossaryService.ReplaceResult(source, 1, source.length());
                });
        DocxTranslationServiceImpl service = new DocxTranslationServiceImpl(
                properties, llmClient, glossaryService, mock(QcService.class));
        ReflectionTestUtils.setField(service, "translationModeConfig", modeConfig);

        TranslationResult result = service.processDocument(
                input.toString(), tempDir.resolve("glossary-output.docx").toString(), "English",
                true, Map.of("水痘减毒活疫苗", "Varicella Vaccine, Live"),
                null, null, true, false, false, false);

        assertEquals(0, result.getErrors());
        assertEquals("Varicella Vaccine, Live shall be refrigerated", result.getPairs().get(0).getTarget());
        verify(llmClient).cachedCall(eq(LlmClientService.CallKind.STRICT), anyString(), eq("English"));
    }

    @Test
    void alternatingBilingualDocumentIsCopiedWithoutCallingTheModel() throws Exception {
        Path input = tempDir.resolve("bilingual.docx");
        try (XWPFDocument document = new XWPFDocument();
             OutputStream output = Files.newOutputStream(input)) {
            String[] texts = {
                    "1. 范围", "1. Scope",
                    "本附录规定无菌产品的生产要求。", "This annex specifies requirements for sterile products.",
                    "2. 原则", "2. Principle",
                    "应采用质量风险管理原则。", "Quality risk management principles should be applied.",
                    "3. 厂房", "3. Premises",
                    "关键区域应保持清洁。", "Critical areas should be kept clean."
            };
            for (String text : texts) {
                document.createParagraph().createRun().setText(text);
            }
            document.write(output);
        }

        LlmClientService llmClient = mock(LlmClientService.class);
        TranslationModeConfig modeConfig = mock(TranslationModeConfig.class);
        TransDocProperties properties = new TransDocProperties();
        properties.setConcurrency(1);
        DocxTranslationServiceImpl service = new DocxTranslationServiceImpl(
                properties, llmClient, mock(GlossaryService.class), mock(QcService.class));
        ReflectionTestUtils.setField(service, "translationModeConfig", modeConfig);
        Path output = tempDir.resolve("bilingual-output.docx");

        TranslationResult result = service.processDocument(
                input.toString(), output.toString(), "English",
                false, Map.of(), null, null, true, false, false, false);

        assertEquals(0, result.getErrors());
        assertEquals(0, result.getSegments());
        assertTrue(Files.exists(output));
        verifyNoInteractions(llmClient);
    }

    @Test
    void strictCorrectionMustPreserveDecimalAndUnitLiterally() throws Exception {
        Path input = tempDir.resolve("decimal.docx");
        try (XWPFDocument document = new XWPFDocument();
             OutputStream output = Files.newOutputStream(input)) {
            document.createParagraph().createRun().setText("加入2.5 mL样品");
            document.write(output);
        }

        LlmClientService llmClient = mock(LlmClientService.class);
        LlmClientService.TranslateResult invalid = new LlmClientService.TranslateResult(
                "Add 25 mL sample", new UsageStats(), "OK");
        when(llmClient.cachedCall(any(), anyString(), anyString())).thenReturn(invalid);
        when(llmClient.sanitizeOrRetry(anyString(), anyString(), anyString(), any())).thenReturn(invalid);
        TranslationModeConfig modeConfig = mock(TranslationModeConfig.class);
        when(modeConfig.isConstraintMode()).thenReturn(false);
        TransDocProperties properties = new TransDocProperties();
        properties.setConcurrency(1);
        DocxTranslationServiceImpl service = new DocxTranslationServiceImpl(
                properties, llmClient, mock(GlossaryService.class), mock(QcService.class));
        ReflectionTestUtils.setField(service, "translationModeConfig", modeConfig);

        TranslationResult result = service.processDocument(
                input.toString(), tempDir.resolve("decimal-output.docx").toString(), "English",
                false, Map.of(), null, null, true, false, false, false);

        assertEquals(1, result.getErrors());
        assertTrue(result.getPairs().get(0).getStatus().startsWith("[ERROR]"));
    }
}
