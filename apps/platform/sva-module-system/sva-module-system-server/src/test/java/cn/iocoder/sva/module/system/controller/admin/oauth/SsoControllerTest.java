package cn.iocoder.sva.module.system.controller.admin.oauth;

import cn.iocoder.sva.module.system.controller.admin.auth.vo.AuthSsoReqVO;
import cn.iocoder.sva.module.system.controller.admin.auth.vo.AuthSsoRespVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SsoControllerTest {

    @Test
    void shouldRejectLegacyRsaSsoWithoutIssuingRedirectToken() {
        AuthSsoReqVO request = new AuthSsoReqVO();
        request.setData("untrusted-encrypted-staff-id");

        AuthSsoRespVO response = new SsoController().sso(request);

        assertFalse(response.getSuccess());
        assertEquals(503, response.getCode());
        assertEquals(null, response.getData());
    }
}
