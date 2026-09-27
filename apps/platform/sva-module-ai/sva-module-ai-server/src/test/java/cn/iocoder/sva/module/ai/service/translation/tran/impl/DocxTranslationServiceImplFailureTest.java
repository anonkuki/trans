package cn.iocoder.sva.module.ai.service.translation.tran.impl;

import cn.iocoder.sva.module.ai.service.translation.tran.DocxTranslationService.TranslationResult;
import cn.iocoder.sva.module.ai.service.translation.tran.GlossaryService;
import cn.iocoder.sva.module.ai.service.translation.tran.LlmClientService;
import cn.iocoder.sva.module.ai.service.translation.tran.QcService;
import cn.iocoder.sva.module.ai.service.translation.tran.config.TransDocProperties;
import cn.iocoder.sva.module.ai.service.translation.tran.config.TranslationModeConfig;
import cn.iocoder.sva.module.ai.service.translation.tran.model.UsageStats;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
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
import static org.mockito.Mockito.when;

class DocxTranslationServiceImplFailureTest {

    @TempDir
    Path tempDir;

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
}
