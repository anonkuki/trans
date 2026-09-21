package cn.iocoder.sva.module.system.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class LogRecordConstantsTest {

    @Test
    void passwordResetLogMustNotContainPasswordExpressions() {
        String template = LogRecordConstants.SYSTEM_USER_UPDATE_PASSWORD_SUCCESS.toLowerCase();

        assertFalse(template.contains("password"));
    }

}
