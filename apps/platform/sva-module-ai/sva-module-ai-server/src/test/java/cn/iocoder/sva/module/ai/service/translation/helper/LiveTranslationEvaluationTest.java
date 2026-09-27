package cn.iocoder.sva.module.ai.service.translation.helper;

import cn.iocoder.sva.module.ai.service.translation.tran.GlossaryService;
import cn.iocoder.sva.module.ai.service.translation.tran.QcService;
import cn.iocoder.sva.module.ai.service.translation.tran.DocxTranslationService.TranslationResult;
import cn.iocoder.sva.module.ai.service.translation.tran.config.TransDocProperties;
import cn.iocoder.sva.module.ai.service.translation.tran.config.TranslationModeConfig;
import cn.iocoder.sva.module.ai.service.translation.tran.context.AiModelContext;
import cn.iocoder.sva.module.ai.service.translation.tran.context.ChatModelContext;
import cn.iocoder.sva.module.ai.service.translation.tran.context.PromptContext;
import cn.iocoder.sva.module.ai.service.translation.tran.context.TranslationCacheContext;
import cn.iocoder.sva.module.ai.service.translation.tran.impl.DocxTranslationServiceImpl;
import cn.iocoder.sva.module.ai.service.translation.tran.impl.LlmClientServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Opt-in live evaluation entry point for the inherited translation engine.
 *
 * <p>The test is skipped unless {@code RUN_LIVE_TRANSLATION_EVAL=1}. Secrets are read only from
 * process environment variables and are never written to test reports or source files.</p>
 */
class LiveTranslationEvaluationTest {

    @AfterEach
    void clearContexts() {
        TranslationCacheContext.clear();
        PromptContext.clear();
        AiModelContext.clear();
        ChatModelContext.clear();
    }

    @Test
    void translateConfiguredDocumentThroughSystemOneEngine() throws Exception {
        Assumptions.assumeTrue("1".equals(System.getenv("RUN_LIVE_TRANSLATION_EVAL")));
        runConfiguredTranslation();
    }

    @Test
    void benchmarkConfiguredDocumentAcrossConcurrencyLevels() throws Exception {
        Assumptions.assumeTrue("1".equals(System.getenv("RUN_LIVE_TRANSLATION_BENCHMARK")));

        Path input = requiredPath("translation.benchmark.input");
        Path outputDirectory = Path.of(requiredProperty("translation.benchmark.output-dir")).toAbsolutePath();
        Files.createDirectories(outputDirectory);
        int warmups = Integer.parseInt(System.getProperty("translation.benchmark.warmups", "1"));
        int runs = Integer.parseInt(System.getProperty("translation.benchmark.runs", "3"));
        int[] levels = java.util.Arrays.stream(
                        System.getProperty("translation.benchmark.concurrencies", "1,2,4,6,8,12").split(","))
                .map(String::trim)
                .mapToInt(Integer::parseInt)
                .toArray();
        List<String> csv = new ArrayList<>();
        csv.add("concurrency,phase,run,ocr_ms,translation_ms,total_ms,output");

        for (int concurrency : levels) {
            for (int iteration = 1; iteration <= warmups + runs; iteration++) {
                String phase = iteration <= warmups ? "warmup" : "formal";
                int phaseRun = iteration <= warmups ? iteration : iteration - warmups;
                Path output = outputDirectory.resolve(String.format(
                        "concurrency-%02d-%s-%02d.docx", concurrency, phase, phaseRun));
                System.setProperty("translation.eval.input", input.toString());
                System.setProperty("translation.eval.output", output.toString());
                System.setProperty("translation.eval.concurrency", Integer.toString(concurrency));

                RunMetrics metrics = runConfiguredTranslation();
                csv.add(String.format("%d,%s,%d,%d,%d,%d,%s",
                        concurrency, phase, phaseRun, metrics.ocrMillis(), metrics.translationMillis(),
                        metrics.ocrMillis() + metrics.translationMillis(), output.getFileName()));
                clearContexts();
            }
        }

        Path csvPath = outputDirectory.resolve("concurrency-benchmark.csv");
        Files.write(csvPath, csv, StandardCharsets.UTF_8);
        System.out.printf("LIVE_TRANSLATION_BENCHMARK output=%s rows=%d%n", csvPath, csv.size() - 1);
    }

    private RunMetrics runConfiguredTranslation() throws Exception {

        Path input = requiredPath("translation.eval.input");
        Path output = Path.of(requiredProperty("translation.eval.output")).toAbsolutePath();
        Files.createDirectories(output.getParent());

        String apiKey = requiredEnvironment("QWEN_API_KEY");
        String baseUrl = normalizeSpringOpenAiBaseUrl(environmentOrDefault(
                "QWEN_API_BASE", "https://dashscope.aliyuncs.com/compatible-mode/v1"));
        String model = environmentOrDefault("QWEN_MODEL", "qwen3.8-flash");
        String targetLanguage = System.getProperty("translation.eval.target", "English");
        int concurrency = Integer.parseInt(System.getProperty("translation.eval.concurrency", "1"));

        TransDocProperties properties = new TransDocProperties();
        properties.setConcurrency(concurrency);
        properties.setTempDir(environmentOrDefault("TRANSDOC_TEMP_DIR", "D:/Temp/trans-platform"));
        properties.setPythonRecognizeUrl(environmentOrDefault(
                "TRANSDOC_PYTHON_RECOGNIZE_URL",
                "http://127.0.0.1:8030/internal/v1/ocr/recognize"));
        properties.setPythonInternalToken(requiredEnvironment("TRANSDOC_PYTHON_INTERNAL_TOKEN"));
        properties.setPythonConnectTimeout(Duration.ofSeconds(10));
        properties.setPythonReadTimeout(Duration.ofMinutes(31));

        Path translationInput = input;
        long ocrStarted = System.nanoTime();
        long ocrMillis = 0;
        if (input.getFileName().toString().toLowerCase().endsWith(".pdf")) {
            File recognized = new ConvertByPythonHelper(new RestTemplateBuilder(), properties)
                    .convertPdfToDocx(input.toFile());
            translationInput = recognized.toPath();
            ocrMillis = elapsedMillis(ocrStarted);
        }

        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder().baseUrl(baseUrl).apiKey(apiKey).build())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(0.2)
                        .build())
                .build();

        LlmClientServiceImpl llmClient = new LlmClientServiceImpl();
        ReflectionTestUtils.setField(llmClient, "transDocProperties", properties);

        TranslationModeConfig modeConfig = mock(TranslationModeConfig.class);
        when(modeConfig.isConstraintMode()).thenReturn(false);
        DocxTranslationServiceImpl translator = new DocxTranslationServiceImpl(
                properties, llmClient, mock(GlossaryService.class), mock(QcService.class));
        ReflectionTestUtils.setField(translator, "translationModeConfig", modeConfig);

        ChatModelContext.set(chatModel);
        AiModelContext.ModelInfo modelInfo = new AiModelContext.ModelInfo();
        modelInfo.setModelCode(model);
        modelInfo.setModelName(model);
        modelInfo.setChatModel(chatModel);
        AiModelContext.set(modelInfo);
        PromptContext.set(buildEvaluationPrompt(targetLanguage));
        TranslationCacheContext.set(true);

        long translationStarted = System.nanoTime();
        TranslationResult result = translator.processDocument(
                translationInput.toString(),
                output.toString(),
                targetLanguage,
                false,
                Map.of(),
                null,
                null,
                true,
                false,
                false,
                false);
        long translationMillis = elapsedMillis(translationStarted);

        assertTrue(Files.isRegularFile(output), "translation output was not created");
        assertTrue(Files.size(output) > 0, "translation output is empty");
        assertTrue(result.getError() == null || result.getError().isBlank(),
                () -> "translation reported an error: " + result.getError());

        System.out.printf(
                "LIVE_TRANSLATION_EVAL input=%s output=%s model=%s concurrency=%d ocr_ms=%d translation_ms=%d total_ms=%d%n",
                input.getFileName(), output, model, concurrency, ocrMillis, translationMillis,
                ocrMillis + translationMillis);
        return new RunMetrics(ocrMillis, translationMillis);
    }

    private record RunMetrics(long ocrMillis, long translationMillis) {
    }

    private static String buildEvaluationPrompt(String targetLanguage) {
        return "You are a professional translator for regulated pharmaceutical and vaccine manufacturing documents. "
                + "Translate accurately into " + targetLanguage + ". Preserve numbers, units, product names, "
                + "document identifiers, terminology, and sentence meaning. Return translation only, without "
                + "commentary or Markdown.";
    }

    private static Path requiredPath(String name) {
        Path path = Path.of(requiredProperty(name)).toAbsolutePath();
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("File does not exist: " + path);
        }
        return path;
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing system property: " + name);
        }
        return value;
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing environment variable: " + name);
        }
        return value;
    }

    private static String environmentOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String normalizeSpringOpenAiBaseUrl(String baseUrl) {
        String normalized = baseUrl.replaceAll("/+$", "");
        return normalized.endsWith("/v1")
                ? normalized.substring(0, normalized.length() - 3)
                : normalized;
    }

    private static long elapsedMillis(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }
}
