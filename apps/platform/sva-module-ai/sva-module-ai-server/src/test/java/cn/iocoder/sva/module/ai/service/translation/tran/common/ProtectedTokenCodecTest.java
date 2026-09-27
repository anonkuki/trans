package cn.iocoder.sva.module.ai.service.translation.tran.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtectedTokenCodecTest {

    @Test
    void restoresRepeatedNumbersUnitsDatesAndIdentifiersExactly() {
        ProtectedTokenCodec.EncodedText encoded = ProtectedTokenCodec.encode(
                "BJ-SOP-TM-0201-05：取2份，每份2.5 mL，20℃，日期2026/09/28，合格率100%。");

        assertFalse(encoded.text().contains("BJ-SOP-TM-0201-05"));
        assertFalse(encoded.text().contains("2.5 mL"));
        assertTrue(encoded.hasAllPlaceholders(encoded.text()));
        List<String> tokens = encoded.placeholders().keySet().stream().toList();
        assertEquals(
                "BJ-SOP-TM-0201-05: Prepare 2 copies, each containing 2.5 mL at 20℃ on 2026/09/28, with a 100% pass rate.",
                encoded.restore(tokens.get(0) + ": Prepare " + tokens.get(1)
                        + " copies, each containing " + tokens.get(2) + " at " + tokens.get(3)
                        + " on " + tokens.get(4) + ", with a " + tokens.get(5) + " pass rate.")
        );
    }

    @Test
    void detectsMissingOrDuplicatedPlaceholder() {
        ProtectedTokenCodec.EncodedText encoded = ProtectedTokenCodec.encode("重复2次，温度20℃");
        String firstPlaceholder = encoded.placeholders().keySet().iterator().next();

        assertFalse(encoded.hasAllPlaceholders(encoded.text().replace(firstPlaceholder, "")));
        assertFalse(encoded.hasAllPlaceholders(encoded.text() + firstPlaceholder));
    }

    @Test
    void protectsChemicalLocantsWithPrimeMarksAsOneToken() {
        String source = "2,4,2',4'-四羟基二苯甲酮";

        ProtectedTokenCodec.EncodedText encoded = ProtectedTokenCodec.encode(source);

        assertEquals(1, encoded.placeholders().size());
        assertEquals("2,4,2',4'", encoded.placeholders().values().iterator().next());
        assertEquals(source, encoded.restore(encoded.text()));
    }
}
