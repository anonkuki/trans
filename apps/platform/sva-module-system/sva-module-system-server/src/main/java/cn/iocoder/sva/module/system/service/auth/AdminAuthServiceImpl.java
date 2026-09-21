package cn.iocoder.sva.module.system.service.auth;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.sva.framework.common.enums.CommonStatusEnum;
import cn.iocoder.sva.framework.common.enums.UserTypeEnum;
import cn.iocoder.sva.framework.common.util.monitor.TracerUtils;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;
import cn.iocoder.sva.framework.common.util.servlet.ServletUtils;
import cn.iocoder.sva.framework.common.util.validation.ValidationUtils;
import cn.iocoder.sva.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.sva.module.infra.api.config.ConfigApi;
import cn.iocoder.sva.module.system.api.logger.dto.LoginLogCreateReqDTO;
import cn.iocoder.sva.module.system.api.sms.SmsCodeApi;
import cn.iocoder.sva.module.system.api.sms.dto.code.SmsCodeUseReqDTO;
import cn.iocoder.sva.module.system.api.social.dto.SocialUserBindReqDTO;
import cn.iocoder.sva.module.system.api.social.dto.SocialUserRespDTO;
import cn.iocoder.sva.module.system.config.OauthPropertiesConfig;
import cn.iocoder.sva.module.system.controller.admin.auth.vo.*;
import cn.iocoder.sva.module.system.convert.auth.AuthConvert;
import cn.iocoder.sva.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.iocoder.sva.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.sva.module.system.dal.dataobject.permission.UserRoleDO;
import cn.iocoder.sva.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.sva.module.system.dal.mysql.permission.UserRoleMapper;
import cn.iocoder.sva.module.system.enums.logger.LoginLogTypeEnum;
import cn.iocoder.sva.module.system.enums.logger.LoginResultEnum;
import cn.iocoder.sva.module.system.enums.oauth2.OAuth2ClientConstants;
import cn.iocoder.sva.module.system.enums.sms.SmsSceneEnum;
import cn.iocoder.sva.module.system.service.logger.LoginLogService;
import cn.iocoder.sva.module.system.service.member.MemberService;
import cn.iocoder.sva.module.system.service.oauth2.OAuth2TokenService;
import cn.iocoder.sva.module.system.service.permission.RoleService;
import cn.iocoder.sva.module.system.service.social.SocialUserService;
import cn.iocoder.sva.module.system.service.user.AdminUserService;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Resource;
import jakarta.validation.Validator;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;

import static cn.iocoder.sva.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.sva.framework.common.util.servlet.ServletUtils.getClientIP;
import static cn.iocoder.sva.module.system.enums.ErrorCodeConstants.*;

/**
 * Auth Service 实现类
 *
 * @author 科兴源码
 */
@Service
@Slf4j
public class AdminAuthServiceImpl implements AdminAuthService {

    @Resource
    private AdminUserService userService;
    @Resource
    private LoginLogService loginLogService;
    @Resource
    private OAuth2TokenService oauth2TokenService;
    @Resource
    private SocialUserService socialUserService;
    @Resource
    private MemberService memberService;
    @Resource
    private Validator validator;
    @Resource
    private CaptchaService captchaService;
    @Resource
    private SmsCodeApi smsCodeApi;

    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private RoleService roleService;

    @Resource
    private ILdapService ldapService;

    @Resource
    private OauthPropertiesConfig oauthPropertiesConfig;
    @Resource
    private RestTemplate restTemplate;

    @Resource
    private ConfigApi configApi;

    /**
     * 验证码的开关，默认为 true
     */
    @Value("${sva.captcha.enable:true}")
    @Setter // 为了单测：开启或者关闭验证码
    private Boolean captchaEnable;

    /**
     * 密码验证模式开关：1-混合模式(超级管理员走数据库，普通用户走外部系统)，0-纯数据库模式
     */
    @Value("${sva.auth.password-mode:1}")
    private Integer passwordMode;

    @Override
    public AdminUserDO authenticate(String username, String password) {
        final LoginLogTypeEnum logTypeEnum = LoginLogTypeEnum.LOGIN_USERNAME;
        // 校验账号是否存在
        AdminUserDO user = userService.getUserByUsername(username);
        if (user == null) {
            createLoginLog(null, username, logTypeEnum, LoginResultEnum.BAD_CREDENTIALS);
            throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
        }
        Long id = user.getId();
        //根据用户查询角色，判断这个用户是否是超级管理员
        List<UserRoleDO> roleIdList = userRoleMapper.selectList(new QueryWrapper<UserRoleDO>().eq("user_id", id));
        List<Long> param = new ArrayList<>();
        for (UserRoleDO userRoleDO : roleIdList){
            param.add(userRoleDO.getRoleId());
        }
        List<RoleDO> roleList = roleService.getRoleList(param);

        boolean isSuperAdmin = false;
        if (!roleList.isEmpty()) {
            for (RoleDO roleDO : roleList) {
                if ("super_admin".equals(roleDO.getCode())) {
                    isSuperAdmin = true;
                    break;
                }
            }
        }
        // 校验是否禁用
        if (CommonStatusEnum.isDisable(user.getStatus())) {
            createLoginLog(user.getId(), username, logTypeEnum, LoginResultEnum.USER_DISABLED);
            throw exception(AUTH_LOGIN_USER_DISABLED);
        }

        // 根据密码验证模式进行密码校验
        if (passwordMode == 0) {
            // 纯数据库模式：所有用户都使用数据库密码验证
            if (!userService.isPasswordMatch(password, user.getPassword())) {
                createLoginLog(user.getId(), username, logTypeEnum, LoginResultEnum.BAD_CREDENTIALS);
                throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
            }
        } else {
            // 混合模式：超级管理员走数据库，普通用户走外部系统
            if (isSuperAdmin) {
                if (!userService.isPasswordMatch(password, user.getPassword())) {
                    createLoginLog(user.getId(), username, logTypeEnum, LoginResultEnum.BAD_CREDENTIALS);
                    throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
                }
            } else {
                // 普通用户使用域控校验密码
                boolean result = ldapService.loginVerify(username, password);
                if (!result){
                    createLoginLog(user.getId(), username, logTypeEnum, LoginResultEnum.BAD_CREDENTIALS);
                    throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
                }
            }
        }

        return user;
    }

    @Override
    @DataPermission(enable = false)
    public AuthLoginRespVO login(AuthLoginReqVO reqVO) {
        // 校验验证码
        validateCaptcha(reqVO);

        // 使用账号密码，进行登录
        AdminUserDO user = authenticate(reqVO.getUsername(), reqVO.getPassword());

        // 如果 socialType 非空，说明需要绑定社交用户
        if (reqVO.getSocialType() != null) {
            socialUserService.bindSocialUser(new SocialUserBindReqDTO(user.getId(), getUserType().getValue(),
                    reqVO.getSocialType(), reqVO.getSocialCode(), reqVO.getSocialState()));
        }
        // 创建 Token 令牌，记录登录日志
        return createTokenAfterLoginSuccess(user.getId(), reqVO.getUsername(), LoginLogTypeEnum.LOGIN_USERNAME);
    }

    @Override
    public void sendSmsCode(AuthSmsSendReqVO reqVO) {
        // 如果是重置密码场景，需要校验图形验证码是否正确
        if (Objects.equals(SmsSceneEnum.ADMIN_MEMBER_RESET_PASSWORD.getScene(), reqVO.getScene())) {
            ResponseModel response = doValidateCaptcha(reqVO);
            if (!response.isSuccess()) {
                throw exception(AUTH_REGISTER_CAPTCHA_CODE_ERROR, response.getRepMsg());
            }
        }

        // 登录场景，验证是否存在
        if (userService.getUserByMobile(reqVO.getMobile()) == null) {
            throw exception(AUTH_MOBILE_NOT_EXISTS);
        }
        // 发送验证码
        smsCodeApi.sendSmsCode(AuthConvert.INSTANCE.convert(reqVO).setCreateIp(getClientIP()));
    }

    @Override
    public AuthLoginRespVO smsLogin(AuthSmsLoginReqVO reqVO) {
        // 校验验证码
        smsCodeApi.useSmsCode(AuthConvert.INSTANCE.convert(reqVO, SmsSceneEnum.ADMIN_MEMBER_LOGIN.getScene(), getClientIP())).checkError();

        // 获得用户信息
        AdminUserDO user = userService.getUserByMobile(reqVO.getMobile());
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }

        // 创建 Token 令牌，记录登录日志
        return createTokenAfterLoginSuccess(user.getId(), reqVO.getMobile(), LoginLogTypeEnum.LOGIN_MOBILE);
    }

    private void createLoginLog(Long userId, String username,
                                LoginLogTypeEnum logTypeEnum, LoginResultEnum loginResult) {
        // 插入登录日志
        LoginLogCreateReqDTO reqDTO = new LoginLogCreateReqDTO();
        reqDTO.setLogType(logTypeEnum.getType());
        reqDTO.setTraceId(TracerUtils.getTraceId());
        reqDTO.setUserId(userId);
        reqDTO.setUserType(getUserType().getValue());
        reqDTO.setUsername(username);
        reqDTO.setUserAgent(ServletUtils.getUserAgent());
        reqDTO.setUserIp(ServletUtils.getClientIP());
        reqDTO.setResult(loginResult.getResult());
        loginLogService.createLoginLog(reqDTO);
        // 更新最后登录时间
        if (userId != null && Objects.equals(LoginResultEnum.SUCCESS.getResult(), loginResult.getResult())) {
            userService.updateUserLogin(userId, ServletUtils.getClientIP());
        }
    }

    @Override
    public AuthLoginRespVO socialLogin(AuthSocialLoginReqVO reqVO) {
        // 使用 code 授权码，进行登录。然后，获得到绑定的用户编号
        SocialUserRespDTO socialUser = socialUserService.getSocialUserByCode(UserTypeEnum.ADMIN.getValue(), reqVO.getType(),
                reqVO.getCode(), reqVO.getState());
        if (socialUser == null || socialUser.getUserId() == null) {
            throw exception(AUTH_THIRD_LOGIN_NOT_BIND);
        }

        // 获得用户
        AdminUserDO user = userService.getUser(socialUser.getUserId());
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }

        // 创建 Token 令牌，记录登录日志
        return createTokenAfterLoginSuccess(user.getId(), user.getUsername(), LoginLogTypeEnum.LOGIN_SOCIAL);
    }

    @VisibleForTesting
    void validateCaptcha(AuthLoginReqVO reqVO) {
        ResponseModel response = doValidateCaptcha(reqVO);
        // 校验验证码
        if (!response.isSuccess()) {
            // 创建登录失败日志（验证码不正确)
            createLoginLog(null, reqVO.getUsername(), LoginLogTypeEnum.LOGIN_USERNAME, LoginResultEnum.CAPTCHA_CODE_ERROR);
            throw exception(AUTH_LOGIN_CAPTCHA_CODE_ERROR, response.getRepMsg());
        }
    }

    private ResponseModel doValidateCaptcha(CaptchaVerificationReqVO reqVO) {
        // 如果验证码关闭，则不进行校验
        if (!captchaEnable) {
            return ResponseModel.success();
        }
        ValidationUtils.validate(validator, reqVO, CaptchaVerificationReqVO.CodeEnableGroup.class);
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaVerification(reqVO.getCaptchaVerification());
        return captchaService.verification(captchaVO);
    }

    private AuthLoginRespVO createTokenAfterLoginSuccess(Long userId, String username, LoginLogTypeEnum logType) {
        // 插入登陆日志
        createLoginLog(userId, username, logType, LoginResultEnum.SUCCESS);
        // 创建访问令牌
        OAuth2AccessTokenDO accessTokenDO = oauth2TokenService.createAccessToken(userId, getUserType().getValue(),
                OAuth2ClientConstants.CLIENT_ID_DEFAULT, null);
        // 构建返回结果
        return BeanUtils.toBean(accessTokenDO, AuthLoginRespVO.class);
    }

    @Override
    public AuthLoginRespVO refreshToken(String refreshToken) {
        OAuth2AccessTokenDO accessTokenDO = oauth2TokenService.refreshAccessToken(refreshToken, OAuth2ClientConstants.CLIENT_ID_DEFAULT);
        return BeanUtils.toBean(accessTokenDO, AuthLoginRespVO.class);
    }

    @Override
    public void logout(String token, Integer logType) {
        // 删除访问令牌
        OAuth2AccessTokenDO accessTokenDO = oauth2TokenService.removeAccessToken(token);
        if (accessTokenDO == null) {
            return;
        }
        // 删除成功，则记录登出日志
        createLogoutLog(accessTokenDO.getUserId(), accessTokenDO.getUserType(), logType);
    }

    private void createLogoutLog(Long userId, Integer userType, Integer logType) {
        LoginLogCreateReqDTO reqDTO = new LoginLogCreateReqDTO();
        reqDTO.setLogType(logType);
        reqDTO.setTraceId(TracerUtils.getTraceId());
        reqDTO.setUserId(userId);
        reqDTO.setUserType(userType);
        if (ObjectUtil.equal(getUserType().getValue(), userType)) {
            reqDTO.setUsername(getUsername(userId));
        } else {
            reqDTO.setUsername(memberService.getMemberUserMobile(userId));
        }
        reqDTO.setUserAgent(ServletUtils.getUserAgent());
        reqDTO.setUserIp(ServletUtils.getClientIP());
        reqDTO.setResult(LoginResultEnum.SUCCESS.getResult());
        loginLogService.createLoginLog(reqDTO);
    }

    private String getUsername(Long userId) {
        if (userId == null) {
            return null;
        }
        AdminUserDO user = userService.getUser(userId);
        return user != null ? user.getUsername() : null;
    }

    private UserTypeEnum getUserType() {
        return UserTypeEnum.ADMIN;
    }

    @Override
    public AuthLoginRespVO register(AuthRegisterReqVO registerReqVO) {
        // 1. 校验验证码
        validateCaptcha(registerReqVO);

        // 2. 校验用户名是否已存在
        Long userId = userService.registerUser(registerReqVO);

        // 3. 创建 Token 令牌，记录登录日志
        return createTokenAfterLoginSuccess(userId, registerReqVO.getUsername(), LoginLogTypeEnum.LOGIN_USERNAME);
    }

    @VisibleForTesting
    void validateCaptcha(AuthRegisterReqVO reqVO) {
        ResponseModel response = doValidateCaptcha(reqVO);
        // 验证不通过
        if (!response.isSuccess()) {
            throw exception(AUTH_REGISTER_CAPTCHA_CODE_ERROR, response.getRepMsg());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(AuthResetPasswordReqVO reqVO) {
        AdminUserDO userByMobile = userService.getUserByMobile(reqVO.getMobile());
        if (userByMobile == null) {
            throw exception(USER_MOBILE_NOT_EXISTS);
        }

        smsCodeApi.useSmsCode(new SmsCodeUseReqDTO()
                .setCode(reqVO.getCode())
                .setMobile(reqVO.getMobile())
                .setScene(SmsSceneEnum.ADMIN_MEMBER_RESET_PASSWORD.getScene())
                .setUsedIp(getClientIP())
        ).checkError();

        userService.updateUserPassword(userByMobile.getId(), reqVO.getPassword());
    }

    // ========== 新增：飞书免登的业务逻辑 ==========
    @Override
    public AuthLoginRespVO feishuSsoLogin(String code) {
        // 1. 获取飞书应用配置
        OauthPropertiesConfig.FeiShu feishuConfig = oauthPropertiesConfig.getFeishu();
        String appId = feishuConfig.getClientId();
        String appSecret = feishuConfig.getClientSecret();
        String appAccessTokenUrl = feishuConfig.getAppAccessTokenUrl();
        String userInfoUrl = feishuConfig.getUserInfoUrl();
        String userAccessTokenUrl = feishuConfig.getUserAccessTokenUrl();
//        String feishuHost = "https://open.feishu.cn";

        try {
            // 2. 获取 app_access_token
            Map<String, String> appTokenReq = new HashMap<>();
            appTokenReq.put("app_id", appId);
            appTokenReq.put("app_secret", appSecret);
            ResponseEntity<Map> appTokenResp = restTemplate.postForEntity(
                    appAccessTokenUrl,
                    appTokenReq,
                    Map.class
            );
            Map appTokenData = appTokenResp.getBody();
            String appAccessToken = (String) appTokenData.get("app_access_token");

            // 3. 使用 code 获取 user_access_token
            Map<String, String> userTokenReq = new HashMap<>();
            userTokenReq.put("grant_type", "authorization_code");
            userTokenReq.put("code", code);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(appAccessToken);
            HttpEntity<Map<String, String>> userTokenEntity = new HttpEntity<>(userTokenReq, headers);
            ResponseEntity<Map> userTokenResp = restTemplate.postForEntity(
                    userAccessTokenUrl,
                    userTokenEntity,
                    Map.class
            );
            Map userTokenData = (Map) userTokenResp.getBody().get("data");
            String userAccessToken = (String) userTokenData.get("access_token");

            // 4. 获取用户信息
            HttpHeaders userInfoHeaders = new HttpHeaders();
            userInfoHeaders.setBearerAuth(userAccessToken);
            HttpEntity<Void> userInfoEntity = new HttpEntity<>(userInfoHeaders);
            ResponseEntity<Map> userInfoResp = restTemplate.exchange(
                    userInfoUrl,
                    HttpMethod.GET,
                    userInfoEntity,
                    Map.class
            );
            Map<String, Object> userInfo = (Map) userInfoResp.getBody().get("data");
            String employeeId = "";
            Object ei = userInfo.get("employee_no");
            if(ei != null && !"".equals(ei.toString())) {
                employeeId = ei.toString();
            }

            // 6. 获得用户信息
            AdminUserDO user = userService.getUserByUsername(employeeId);
            if (user == null) {
                throw exception(USER_NOT_EXISTS);
            }

            // 7. 复用原有的Token创建方法！
            return createTokenAfterLoginSuccess(user.getId(), user.getUsername(), LoginLogTypeEnum.LOGIN_SOCIAL);

        } catch (Exception e) {
            log.info("飞书免登失败", e);
            throw exception(AUTH_THIRD_LOGIN_NOT_BIND);
        }
    }

    // ========== 新增：外部系统单点登录（RSA 密文直接登录） ==========
    @Override
    public AuthLoginRespVO ssoLogin(String data) {
        // 旧协议只有加密、没有可信签名；任何取得公钥的人都能伪造 staffId。
        // 服务层同样强制关闭，防止未来从其他 Controller 绕过入口保护。
        throw exception(AUTH_SSO_PARAM_ERROR);
    }

    /**
     * 仅保留用于迁移时理解旧协议，禁止从业务入口调用。
     */
    @Deprecated(forRemoval = true)
    private AuthLoginRespVO legacyInsecureSsoLogin(String data) {
        // 1. 从参数配置获取 RSA 私钥（配置管理页面，参数键名 login.private）
        String privateKey = configApi.getConfigValueByKey(SSO_LOGIN_PRIVATE_KEY).getCheckedData();
        if (StrUtil.isBlank(privateKey)) {
            throw exception(AUTH_SSO_CONFIG_ERROR, SSO_LOGIN_PRIVATE_KEY);
        }

        // 2. 使用 RSA 解密，得到明文 JSON。
        // 先按标准模式（公钥加密、私钥解密）；对方实际协议是"私钥加密"，失败时回退为用派生公钥反向解密
        String plainText;
        try {
            RSA rsa = SecureUtil.rsa(cleanPemKey(privateKey), null);
            plainText = rsa.decryptStr(data, KeyType.PrivateKey);
        } catch (Exception ex) {
            try {
                plainText = decryptByPublicKey(privateKey, data);
                log.info("[ssoLogin][密文为对方私钥加密，已用配对公钥反向解密成功]");
            } catch (Exception ex2) {
                // 两种模式都失败，输出密钥自检信息，方便定位是私钥配置问题还是密文不匹配问题
                logRsaDiagnostics(privateKey);
                log.error("[ssoLogin][RSA 解密失败，密文 Base64 长度：{}]", data != null ? data.length() : 0, ex);
                throw exception(AUTH_SSO_DECRYPT_ERROR);
            }
        }

        // 3. 解析明文，获取登录账号 staffId 与时间戳 timeStamp
        JSONObject json;
        try {
            json = JSONUtil.parseObj(plainText);
        } catch (Exception ex) {
            log.error("[ssoLogin][解密内容解析失败]", ex);
            throw exception(AUTH_SSO_DECRYPT_ERROR);
        }
        String staffId = json.getStr("staffId");
        if (StrUtil.isBlank(staffId)) {
            throw exception(AUTH_SSO_DECRYPT_ERROR);
        }

        // 3.1 校验登录凭证：报文中缺少 timeStamp 时拒绝登录；当前时间超过 timeStamp 则视为过期（兼容秒/毫秒、数字/字符串）
        String timeStamp = json.getStr("timeStamp");
        if (StrUtil.isBlank(timeStamp)) {
            log.info("[ssoLogin][接收的参数缺少 timeStamp，拒绝登录，info：{}]", plainText);
            throw exception(AUTH_SSO_PARAM_ERROR);
        }
        validateSsoTimestamp(timeStamp);

        // 4. 校验用户存在且未被禁用
        AdminUserDO user = userService.getUserByUsername(staffId);
        if (user == null) {
            createLoginLog(null, staffId, LoginLogTypeEnum.LOGIN_SOCIAL, LoginResultEnum.BAD_CREDENTIALS);
            throw exception(USER_NOT_EXISTS);
        }
        if (CommonStatusEnum.isDisable(user.getStatus())) {
            createLoginLog(user.getId(), staffId, LoginLogTypeEnum.LOGIN_SOCIAL, LoginResultEnum.USER_DISABLED);
            throw exception(AUTH_LOGIN_USER_DISABLED);
        }

        // 5. 直接创建 Token 令牌，记录登录日志（无需密码校验）
        return createTokenAfterLoginSuccess(user.getId(), user.getUsername(), LoginLogTypeEnum.LOGIN_SOCIAL);
    }

    /**
     * 清理私钥内容：兼容带 PEM 头尾、换行的密钥格式，只保留 Base64 主体
     */
    private static String cleanPemKey(String key) {
        return key.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s+", "");
    }

    /**
     * 对方"私钥加密"协议的反向解密：从私钥（CRT 结构）派生出配对公钥，用公钥还原密文。
     */
    private String decryptByPublicKey(String privateKey, String data) throws Exception {
        java.security.PrivateKey pk = KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(Base64.decode(cleanPemKey(privateKey))));
        RSAPrivateCrtKey crt = (RSAPrivateCrtKey) pk;
        PublicKey publicKey = KeyFactory.getInstance("RSA")
                .generatePublic(new RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent()));
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, publicKey);
        return new String(cipher.doFinal(Base64.decode(data)), java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 校验 SSO 登录凭证的时间戳有效期：当前时间超过 timeStamp 则视为登录已过期。
     * 兼容秒（10 位）与毫秒（13 位）两种精度，数字或字符串均可。
     */
    private void validateSsoTimestamp(String timeStamp) {
        long ts;
        try {
            ts = Long.parseLong(timeStamp.trim());
        } catch (NumberFormatException ex) {
            log.error("[ssoLogin][timeStamp 格式不正确：{}]", timeStamp);
            throw exception(AUTH_SSO_PARAM_ERROR);
        }
        // 自动识别秒/毫秒：小于阈值（12 位以下）视为秒级时间戳，转换为毫秒后再比较。
        // 参考：2001-09-09 之后秒级时间戳为 10 位（约 10 亿量级），毫秒级为 13 位（约万亿量级）
        if (ts < 100_000_000_000L) {
            ts *= 1000L;
        }
        long now = System.currentTimeMillis();
        if (now > ts) {
            log.warn("[ssoLogin][登录凭证已过期，timeStamp：{}，当前时间：{}]", ts, now);
            throw exception(AUTH_SSO_EXPIRED);
        }
    }

    /**
     * RSA 解密失败时的密钥自检：解析私钥并从其模数/公钥指数派生出公钥，做一次自加密自解密。
     * 自检通过 → 私钥本身可用，问题在对方密文（加密用的公钥与配置的私钥不配对）；自检失败 → 私钥配置有误。
     */
    private void logRsaDiagnostics(String privateKey) {
        try {
            java.security.PrivateKey pk = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(Base64.decode(cleanPemKey(privateKey))));
            int keyBitSize = ((java.security.interfaces.RSAPrivateKey) pk).getModulus().bitLength();
            log.warn("[ssoLogin][密钥自检] 私钥解析成功，位数：{} bit", keyBitSize);
            if (pk instanceof RSAPrivateCrtKey crt) {
                // 私钥（CRT 结构）中包含模数和公钥指数，可派生出配对的公钥
                PublicKey publicKey = KeyFactory.getInstance("RSA")
                        .generatePublic(new RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent()));
                RSA rsa = SecureUtil.rsa(cleanPemKey(privateKey), Base64.encode(publicKey.getEncoded()));
                String test = rsa.encryptBase64("sso-key-check", KeyType.PublicKey);
                String decrypted = rsa.decryptStr(test, KeyType.PrivateKey);
                log.warn("[ssoLogin][密钥自检] 自加密自解密{}，私钥本身可用；解密外部密文仍失败说明对方加密用的公钥与该私钥不配对",
                        "sso-key-check".equals(decrypted) ? "成功" : "结果不一致");
            }
        } catch (Exception e) {
            log.error("[ssoLogin][密钥自检失败，说明配置的私钥不可用（格式错误或损坏）]", e);
        }
    }

}
