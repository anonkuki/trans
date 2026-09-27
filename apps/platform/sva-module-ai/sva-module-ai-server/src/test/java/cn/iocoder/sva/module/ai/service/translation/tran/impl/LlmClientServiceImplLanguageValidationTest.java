package cn.iocoder.sva.module.ai.service.translation.tran.impl;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmClientServiceImplLanguageValidationTest {

    private final LlmClientServiceImpl service = new LlmClientServiceImpl();

    @Test
    void allowsPreservedEmailAndUrlInChineseTranslation() {
        assertFalse(violates("localproduction@who.int\n网站："));
        assertFalse(violates("https://www.who.int/teams/regulation-prequalification/lpa\nwww.who.int"));
    }

    @Test
    void stillRejectsUntranslatedEnglishSentence() {
        assertTrue(violates("This paragraph is still entirely untranslated and must be retried."));
    }

    private boolean violates(String content) {
        return Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(
                service, "violatesTargetLanguage", content, "Chinese"));
    }
}
