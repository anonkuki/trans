package cn.iocoder.sva.module.system.service.auth;

import cn.iocoder.sva.module.system.controller.admin.auth.vo.*;
import cn.iocoder.sva.module.system.dal.dataobject.user.AdminUserDO;
import jakarta.validation.Valid;

/**
 * 管理后台的认证 Service 接口
 *
 * 提供用户的登录、登出的能力
 *
 * @author 科兴源码
 */
public interface AdminAuthService {

    /**
     * 参数配置的键名：外部单点登录的 RSA 私钥
     */
    String SSO_LOGIN_PRIVATE_KEY = "login.private";
    /**
     * 参数配置的键名：外部单点登录成功后返回的前端首页地址（如 http://192.168.1.100:8080）
     */
    String SSO_HOME_URL_KEY = "login.sso.url";

    /**
     * 验证账号 + 密码。如果通过，则返回用户
     *
     * @param username 账号
     * @param password 密码
     * @return 用户
     */
    AdminUserDO authenticate(String username, String password);

    /**
     * 账号登录
     *
     * @param reqVO 登录信息
     * @return 登录结果
     */
    AuthLoginRespVO login(@Valid AuthLoginReqVO reqVO);

    /**
     * 基于 token 退出登录
     *
     * @param token token
     * @param logType 登出类型
     */
    void logout(String token, Integer logType);

    /**
     * 短信验证码发送
     *
     * @param reqVO 发送请求
     */
    void sendSmsCode(AuthSmsSendReqVO reqVO);

    /**
     * 短信登录
     *
     * @param reqVO 登录信息
     * @return 登录结果
     */
    AuthLoginRespVO smsLogin(AuthSmsLoginReqVO reqVO);

    /**
     * 社交快捷登录，使用 code 授权码
     *
     * @param reqVO 登录信息
     * @return 登录结果
     */
    AuthLoginRespVO socialLogin(@Valid AuthSocialLoginReqVO reqVO);

    /**
     * 刷新访问令牌
     *
     * @param refreshToken 刷新令牌
     * @return 登录结果
     */
    AuthLoginRespVO refreshToken(String refreshToken);

    /**
     * 用户注册
     *
     * @param createReqVO 注册用户
     * @return 注册结果
     */
    AuthLoginRespVO register(AuthRegisterReqVO createReqVO);

    /**
     * 重置密码
     *
     * @param reqVO 验证码信息
     */
    void resetPassword(AuthResetPasswordReqVO reqVO);

    /**
     * 飞书网页应用免登登录
     * @param code 免登授权码
     * @return 登录结果
     */
    AuthLoginRespVO feishuSsoLogin(String code);

    /**
     * 外部系统单点登录：使用 RSA 密文直接登录，密文解密后取 staffId 作为登录账号，无需密码校验
     *
     * @param data RSA 公钥加密的密文，解密后为 JSON，包含 staffId 字段
     * @return 登录结果
     */
    AuthLoginRespVO ssoLogin(String data);

}
