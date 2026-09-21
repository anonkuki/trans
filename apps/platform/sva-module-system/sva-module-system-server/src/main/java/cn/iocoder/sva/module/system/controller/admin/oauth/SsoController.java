package cn.iocoder.sva.module.system.controller.admin.oauth;

import cn.iocoder.sva.framework.common.util.monitor.TracerUtils;
import cn.iocoder.sva.module.system.controller.admin.auth.vo.AuthSsoReqVO;
import cn.iocoder.sva.module.system.controller.admin.auth.vo.AuthSsoRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 外部系统单点登录 Controller
 *
 * 供外部系统通过 POST /oauth/sso 传入 RSA 密文，直接登录系统并返回携带 accessToken 的首页链接
 */
@Tag(name = "管理后台 - 外部系统单点登录")
@RestController
@RequestMapping("/oauth")
@Slf4j
public class SsoController {

    @PostMapping("/sso")
    @PermitAll
    @Operation(summary = "外部系统单点登录（RSA 密文直接登录）")
    public AuthSsoRespVO sso(@RequestBody @Valid AuthSsoReqVO reqVO) {
        String traceId = TracerUtils.getTraceId();
        // 交付协议仅用可公开的 RSA 公钥加密 staffId，不能证明调用方身份，且没有可靠的防重放机制。
        // 在改为签名断言或标准 OIDC/OAuth2 code flow 前保持关闭，避免产生可伪造的登录令牌。
        log.warn("[sso][已拒绝不安全的旧版 RSA 单点登录请求]");
        return AuthSsoRespVO.error(503, "旧版单点登录协议已停用，请接入签名断言或标准 OIDC", null, traceId);
    }

}
