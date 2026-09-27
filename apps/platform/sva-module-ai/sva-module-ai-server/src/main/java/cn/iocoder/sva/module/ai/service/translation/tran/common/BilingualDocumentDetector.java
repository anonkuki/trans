package cn.iocoder.sva.module.ai.service.translation.tran.common;

import java.util.ArrayList;
import java.util.List;

/** Detects already translated documents whose Chinese and English paragraphs alternate. */
public final class BilingualDocumentDetector {

    private enum Language { CHINESE, ENGLISH, MIXED }

    private BilingualDocumentDetector() {
    }

    public static boolean isAlternatingBilingual(List<String> paragraphs) {
        if (paragraphs == null || paragraphs.isEmpty()) {
            return false;
        }

        List<Language> classified = new ArrayList<>();
        int chinese = 0;
        int english = 0;
        for (String paragraph : paragraphs) {
            Language language = classify(paragraph);
            if (language == Language.MIXED) {
                continue;
            }
            classified.add(language);
            if (language == Language.CHINESE) {
                chinese++;
            } else {
                english++;
            }
        }
        if (chinese < 4 || english < 4 || classified.size() < 10) {
            return false;
        }

        int transitions = 0;
        for (int i = 1; i < classified.size(); i++) {
            if (classified.get(i) != classified.get(i - 1)) {
                transitions++;
            }
        }
        double alternationRatio = transitions / (double) (classified.size() - 1);
        double languageBalance = Math.min(chinese, english) / (double) Math.max(chinese, english);
        return alternationRatio >= 0.60 && languageBalance >= 0.50;
    }

    private static Language classify(String text) {
        if (text == null || text.isBlank()) {
            return Language.MIXED;
        }
        int cjk = 0;
        int latin = 0;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN) {
                cjk++;
            } else if ((codePoint >= 'A' && codePoint <= 'Z') || (codePoint >= 'a' && codePoint <= 'z')) {
                latin++;
            }
        }
        if (cjk >= 2 && cjk >= latin) {
            return Language.CHINESE;
        }
        if (latin >= 4 && cjk == 0) {
            return Language.ENGLISH;
        }
        return Language.MIXED;
    }
}
