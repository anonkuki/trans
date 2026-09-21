package cn.iocoder.sva.module.system.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 外部系统单点登录 Request VO")
@Data
public class AuthSsoReqVO {

    @Schema(description = "RSA 公钥加密的登录密文", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "登录密文不能为空")
    private String data;

}
