package cn.iocoder.sva.module.ai.service.translation.tran.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranslationConcurrencyPolicyTest {

    @Test
    void selectsTwelveWorkersForSmallDocuments() {
        assertEquals(12, TranslationConcurrencyPolicy.resolve(defaults(), 20, 4_000));
    }

    @Test
    void selectsTenWorkersForMediumDocuments() {
        assertEquals(10, TranslationConcurrencyPolicy.resolve(defaults(), 80, 20_000));
    }

    @Test
    void selectsEightWorkersWhenSegmentCountIsLarge() {
        assertEquals(8, TranslationConcurrencyPolicy.resolve(defaults(), 151, 20_000));
    }

    @Test
    void selectsEightWorkersWhenCharacterCountIsLarge() {
        assertEquals(8, TranslationConcurrencyPolicy.resolve(defaults(), 20, 50_001));
    }

    @Test
    void neverCreatesMoreWorkersThanTasksOrGlobalCapacity() {
        TransDocProperties properties = defaults();
        assertEquals(5, TranslationConcurrencyPolicy.resolve(properties, 5, 1_000));

        properties.setGlobalConcurrency(7);
        assertEquals(7, TranslationConcurrencyPolicy.resolve(properties, 20, 4_000));
    }

    @Test
    void usesLegacyFixedConcurrencyWhenAdaptiveModeIsDisabled() {
        TransDocProperties properties = defaults();
        properties.setAdaptiveConcurrencyEnabled(false);
        properties.setConcurrency(6);

        assertEquals(6, TranslationConcurrencyPolicy.resolve(properties, 200, 80_000));
    }

    @Test
    void keepsLegacyFixedConcurrencyUntilAdaptiveModeIsExplicitlyEnabled() {
        TransDocProperties properties = new TransDocProperties();

        assertEquals(6, TranslationConcurrencyPolicy.resolve(properties, 200, 80_000));
    }

    private static TransDocProperties defaults() {
        TransDocProperties properties = new TransDocProperties();
        properties.setAdaptiveConcurrencyEnabled(true);
        return properties;
    }
}
