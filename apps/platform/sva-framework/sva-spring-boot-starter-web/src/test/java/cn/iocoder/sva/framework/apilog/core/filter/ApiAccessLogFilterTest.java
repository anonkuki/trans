package cn.iocoder.sva.framework.apilog.core.filter;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiAccessLogFilterTest {

    @Test
    void sanitizeJson_removesCredentialFieldsCaseInsensitively() {
        String source = "{\"oldPassword\":\"old-secret\",\"NEWPASSWORD\":\"new-secret\","
                + "\"nested\":{\"clientSecret\":\"client-secret\",\"name\":\"safe\"}}";

        String sanitized = ReflectionTestUtils.invokeMethod(ApiAccessLogFilter.class,
                "sanitizeJson", source, null);

        assertFalse(sanitized.contains("old-secret"));
        assertFalse(sanitized.contains("new-secret"));
        assertFalse(sanitized.contains("client-secret"));
        assertTrue(sanitized.contains("safe"));
    }

    @Test
    void sanitizeJson_returnsNullWhenPayloadCannotBeParsed() {
        String sanitized = ReflectionTestUtils.invokeMethod(ApiAccessLogFilter.class,
                "sanitizeJson", "{\"password\":\"secret\"", null);

        assertNull(sanitized);
    }

    @Test
    void sanitizeMap_removesCredentialFieldsCaseInsensitivelyWithoutMutatingInput() {
        Map<String, String> source = new LinkedHashMap<>();
        source.put("oldPassword", "old-secret");
        source.put("AUTHORIZATION", "bearer-secret");
        source.put("name", "safe");

        String sanitized = ReflectionTestUtils.invokeMethod(ApiAccessLogFilter.class,
                "sanitizeMap", source, null);

        assertFalse(sanitized.contains("old-secret"));
        assertFalse(sanitized.contains("bearer-secret"));
        assertTrue(sanitized.contains("safe"));
        assertTrue(source.containsKey("oldPassword"));
        assertTrue(source.containsKey("AUTHORIZATION"));
    }
}
