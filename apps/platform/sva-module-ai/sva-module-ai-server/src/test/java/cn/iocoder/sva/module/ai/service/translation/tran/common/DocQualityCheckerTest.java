package cn.iocoder.sva.module.ai.service.translation.tran.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocQualityCheckerTest {

    @Test
    void repeatedNumberOccurrencesMustNotBeCollapsed() {
        DocQualityChecker.QcResult result = DocQualityChecker.checkNumbers(
                "平行制备2份，每份取2 mL。", "Prepare 2 copies in parallel and take mL from each.");

        assertFalse(result.isOk());
        assertTrue(result.getStatus().contains("2"));
    }

    @Test
    void equivalentTemperatureAndMicrogramUnitsRemainAccepted() {
        assertTrue(DocQualityChecker.checkNumbers("20℃；100µg", "20 °C; 100 μg").isOk());
    }
}
