package cn.iocoder.sva.module.system.controller.admin.auth.vo;

import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 外部系统单点登录 Response VO")
@Data
public class AuthSsoRespVO {

    @Schema(description = "响应编码，0 表示成功", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer code;

    @Schema(description = "是否成功", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean success;

    @Schema(description = "响应消息", requiredMode = Schema.RequiredMode.REQUIRED, example = "成功")
    private String message;

    @Schema(description = "登录后的首页链接，携带 accessToken", example = "http://192.168.1.100:8080/workbench?accessToken=xxx")
    private String data;

    @Schema(description = "链路追踪编号", example = "d921398a-8139-4bdc-bddd-23a4045f7738")
    private String traceId;

    @Schema(description = "主机地址", example = "192.168.1.100")
    private String host;

    @Schema(description = "响应时间", example = "2024-02-22 16:26:08")
    private String timestamp;

    @Schema(description = "是否成功", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean ok;

    public static AuthSsoRespVO success(String url, String host, String traceId) {
        AuthSsoRespVO respVO = buildBase(traceId, host);
        respVO.setCode(0);
        respVO.setSuccess(true);
        respVO.setOk(true);
        respVO.setMessage("成功");
        respVO.setData(url);
        return respVO;
    }

    public static AuthSsoRespVO error(Integer code, String message, String host, String traceId) {
        AuthSsoRespVO respVO = buildBase(traceId, host);
        respVO.setCode(code);
        respVO.setSuccess(false);
        respVO.setOk(false);
        respVO.setMessage(message);
        return respVO;
    }

    private static AuthSsoRespVO buildBase(String traceId, String host) {
        AuthSsoRespVO respVO = new AuthSsoRespVO();
        respVO.setTraceId(traceId);
        respVO.setHost(host);
        respVO.setTimestamp(DateUtil.now()); // yyyy-MM-dd HH:mm:ss
        return respVO;
    }

}
