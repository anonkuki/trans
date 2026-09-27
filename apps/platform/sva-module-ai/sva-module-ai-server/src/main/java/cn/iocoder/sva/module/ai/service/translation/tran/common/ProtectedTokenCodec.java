package cn.iocoder.sva.module.ai.service.translation.tran.common;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces regulated-document tokens with stable sentinels during model calls and restores the
 * exact source spelling afterward.
 */
public final class ProtectedTokenCodec {

    private static final Pattern PROTECTED_TOKEN_PATTERN = Pattern.compile(
            "\\d+(?:['′])?(?:,\\d+(?:['′])?)+"
                    + "|(?<![A-Za-z0-9])(?=[A-Z0-9-]*[A-Z])(?=[A-Z0-9-]*\\d)[A-Z0-9]+(?:-[A-Z0-9]+)+(?![A-Za-z0-9])"
                    + "|\\d+(?:[.,/:]\\d+)*(?:\\s*(?:%|°C|℃|mg|mL|ml|μg|µg|ug|mcg|nm|g/min|g|U|h|min|s)(?![A-Za-z]))?",
            Pattern.CASE_INSENSITIVE);

    private ProtectedTokenCodec() {
    }

    public static EncodedText encode(String source) {
        String safeSource = source == null ? "" : source;
        Matcher matcher = PROTECTED_TOKEN_PATTERN.matcher(safeSource);
        StringBuffer encoded = new StringBuffer();
        Map<String, String> placeholders = new LinkedHashMap<>();
        int index = 0;
        while (matcher.find()) {
            String placeholder = String.format("__SVA_KEEP_%04d__", index++);
            placeholders.put(placeholder, matcher.group());
            matcher.appendReplacement(encoded, Matcher.quoteReplacement(placeholder));
        }
        matcher.appendTail(encoded);
        return new EncodedText(encoded.toString(), placeholders);
    }

    public record EncodedText(String text, Map<String, String> placeholders) {

        public EncodedText {
            placeholders = Collections.unmodifiableMap(new LinkedHashMap<>(placeholders));
        }

        public boolean hasAllPlaceholders(String candidate) {
            if (candidate == null) {
                return placeholders.isEmpty();
            }
            for (String placeholder : placeholders.keySet()) {
                int first = candidate.indexOf(placeholder);
                if (first < 0 || candidate.indexOf(placeholder, first + placeholder.length()) >= 0) {
                    return false;
                }
            }
            return true;
        }

        public String restore(String candidate) {
            if (!hasAllPlaceholders(candidate)) {
                throw new IllegalArgumentException("protected translation tokens were lost or duplicated");
            }
            String restored = candidate;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                restored = restored.replace(entry.getKey(), entry.getValue());
            }
            return restored;
        }
    }
}
