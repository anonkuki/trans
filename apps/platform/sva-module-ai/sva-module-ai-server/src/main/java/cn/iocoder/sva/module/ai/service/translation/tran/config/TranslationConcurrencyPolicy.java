package cn.iocoder.sva.module.ai.service.translation.tran.config;

/**
 * Chooses a bounded worker count from the amount of translatable content.
 */
public final class TranslationConcurrencyPolicy {

    private TranslationConcurrencyPolicy() {
    }

    public static int resolve(TransDocProperties properties, int taskCount, long characterCount) {
        int safeTaskCount = Math.max(1, taskCount);
        int selected;

        if (!properties.isAdaptiveConcurrencyEnabled()) {
            selected = properties.getConcurrency();
        } else if (taskCount <= properties.getSmallSegmentThreshold()
                && characterCount <= properties.getSmallCharacterThreshold()) {
            selected = properties.getSmallConcurrency();
        } else if (taskCount > properties.getMediumSegmentThreshold()
                || characterCount > properties.getMediumCharacterThreshold()) {
            selected = properties.getLargeConcurrency();
        } else {
            selected = properties.getMediumConcurrency();
        }

        int globalLimit = Math.max(1, properties.getGlobalConcurrency());
        return Math.max(1, Math.min(Math.min(selected, safeTaskCount), globalLimit));
    }
}
