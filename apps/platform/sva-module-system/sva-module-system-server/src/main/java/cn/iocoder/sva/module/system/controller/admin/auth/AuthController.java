package cn.iocoder.sva.module.system.controller.admin.auth;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.sva.framework.common.enums.CommonStatusEnum;
import cn.iocoder.sva.framework.common.enums.UserTypeEnum;
import cn.iocoder.sva.framework.common.pojo.CommonResult;
import cn.iocoder.sva.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.sva.framework.security.config.SecurityProperties;
import cn.iocoder.sva.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.sva.module.system.config.OauthPropertiesConfig;
import cn.iocoder.sva.module.system.controller.admin.auth.vo.*;
import cn.iocoder.sva.module.system.convert.auth.AuthConvert;
import cn.iocoder.sva.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.sva.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.sva.module.system.dal.dataobject.role.DeptRoleDO;
import cn.iocoder.sva.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.sva.module.system.enums.logger.LoginLogTypeEnum;
import cn.iocoder.sva.module.system.framework.justauth.core.AuthRequestFactory;
import cn.iocoder.sva.module.system.service.auth.AdminAuthService;
import cn.iocoder.sva.module.system.service.permission.MenuService;
import cn.iocoder.sva.module.system.service.permission.PermissionService;
import cn.iocoder.sva.module.system.service.permission.RoleService;
import cn.iocoder.sva.module.system.service.role.DeptRoleService;
import cn.iocoder.sva.module.system.service.social.SocialClientService;
import cn.iocoder.sva.module.system.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.cache.AuthDefaultStateCache;
import me.zhyd.oauth.cache.AuthStateCache;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static cn.iocoder.sva.framework.common.pojo.CommonResult.success;
import static cn.iocoder.sva.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.sva.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 认证")
@RestController
@RequestMapping("/system/auth")
@Validated
@Slf4j
public class AuthController {

    @Resource
    private AdminAuthService authService;
    @Resource
    private AdminUserService userService;
    @Resource
    private RoleService roleService;
    @Resource
    private MenuService menuService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private SocialClientService socialClientService;
    @Resource
    private DeptRoleService deptRoleService;

    @Resource
    private SecurityProperties securityProperties;

    @Resource
    private OauthPropertiesConfig oauthPropertiesConfig;


    @Resource
    private AuthRequestFactory authRequestFactory;

    // JustAuth 默认的状态缓存（内存缓存）
    private final AuthStateCache authStateCache = AuthDefaultStateCache.INSTANCE;

    @PostMapping("/login")
    @PermitAll
    @Operation(summary = "使用账号密码登录")
    public CommonResult<AuthLoginRespVO> login(@RequestBody @Valid AuthLoginReqVO reqVO) {
        return success(authService.login(reqVO));
    }

    @PostMapping("/logout")
    @PermitAll
    @Operation(summary = "登出系统")
    public CommonResult<Boolean> logout(HttpServletRequest request) {
        String token = SecurityFrameworkUtils.obtainAuthorization(request,
                securityProperties.getTokenHeader(), securityProperties.getTokenParameter());
        if (StrUtil.isNotBlank(token)) {
            authService.logout(token, LoginLogTypeEnum.LOGOUT_SELF.getType());
        }
        return success(true);
    }

    @PostMapping("/refresh-token")
    @PermitAll
    @Operation(summary = "刷新令牌")
    @Parameter(name = "refreshToken", description = "刷新令牌", required = true)
    public CommonResult<AuthLoginRespVO> refreshToken(@RequestParam("refreshToken") String refreshToken) {
        return success(authService.refreshToken(refreshToken));
    }

    @GetMapping("/get-permission-info")
    @Operation(summary = "获取登录用户的权限信息")
    @DataPermission(enable = false) // 忽略数据权限，避免因为过滤，导致无法查询用户。类似：https://t.zsxq.com/LHnrp
    public CommonResult<AuthPermissionInfoRespVO> getPermissionInfo() {
        // 1.1 获得用户信息
        AdminUserDO user = userService.getUser(getLoginUserId());
        if (user == null) {
            return success(null);
        }

        // 1.2 获得角色列表
        Set<Long> roleIds = permissionService.getUserRoleIdListByUserId(getLoginUserId());

        // 获取用户部门角色列表
        Long deptId = user.getDeptId();
        List<DeptRoleDO> deptRoleList = deptRoleService.getDeptRoleListByDeptId(deptId);
        Set<Long> deptRoleIds = convertSet(deptRoleList, DeptRoleDO::getRoleId);
        roleIds.addAll(deptRoleIds);

        if (CollUtil.isEmpty(roleIds)) {
            return success(AuthConvert.INSTANCE.convert(user, Collections.emptyList(), Collections.emptyList()));
        }
        List<RoleDO> roles = roleService.getRoleList(roleIds);
        roles.removeIf(role -> !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus())); // 移除禁用的角色

        // 1.3 获得菜单列表
        Set<Long> menuIds = permissionService.getRoleMenuListByRoleId(convertSet(roles, RoleDO::getId));
        List<MenuDO> menuList = menuService.getMenuList(menuIds);
        menuList = menuService.filterDisableMenus(menuList);

        // 2. 拼接结果返回
        return success(AuthConvert.INSTANCE.convert(user, roles, menuList));
    }

    @PostMapping("/register")
    @PermitAll
    @Operation(summary = "注册用户")
    public CommonResult<AuthLoginRespVO> register(@RequestBody @Valid AuthRegisterReqVO registerReqVO) {
        return success(authService.register(registerReqVO));
    }

    // ========== 短信登录相关 ==========

    @PostMapping("/sms-login")
    @PermitAll
    @Operation(summary = "使用短信验证码登录")
    // 可按需开启限流：https://github.com/YunaiV/ruoyi-vue-pro/issues/851
    // @RateLimiter(time = 60, count = 6, keyResolver = ExpressionRateLimiterKeyResolver.class, keyArg = "#reqVO.mobile")
    public CommonResult<AuthLoginRespVO> smsLogin(@RequestBody @Valid AuthSmsLoginReqVO reqVO) {
        return success(authService.smsLogin(reqVO));
    }

    @PostMapping("/send-sms-code")
    @PermitAll
    @Operation(summary = "发送手机验证码")
    public CommonResult<Boolean> sendLoginSmsCode(@RequestBody @Valid AuthSmsSendReqVO reqVO) {
        authService.sendSmsCode(reqVO);
        return success(true);
    }

    @PostMapping("/reset-password")
    @PermitAll
    @Operation(summary = "重置密码")
    public CommonResult<Boolean> resetPassword(@RequestBody @Valid AuthResetPasswordReqVO reqVO) {
        authService.resetPassword(reqVO);
        return success(true);
    }

    // ========== 社交登录相关 ==========

    @GetMapping("/social-auth-redirect")
    @PermitAll
    @Operation(summary = "社交授权的跳转")
    @Parameters({
            @Parameter(name = "type", description = "社交类型", required = true),
            @Parameter(name = "redirectUri", description = "回调路径")
    })
    public CommonResult<String> socialLogin(@RequestParam("type") Integer type,
                                            @RequestParam("redirectUri") String redirectUri) {
        return success(socialClientService.getAuthorizeUrl(
                type, UserTypeEnum.ADMIN.getValue(), redirectUri));
    }

    @PostMapping("/social-login")
    @PermitAll
    @Operation(summary = "社交快捷登录，使用 code 授权码", description = "适合未登录的用户，但是社交账号已绑定用户")
    public CommonResult<AuthLoginRespVO> socialQuickLogin(@RequestBody @Valid AuthSocialLoginReqVO reqVO) {
        return success(authService.socialLogin(reqVO));
    }

    @GetMapping("/feishu-auth-url")
    @Operation(summary = "获取飞书授权参数")
    @PermitAll
    public CommonResult<FeishuAuthVO> getFeishuAuthUrl(@RequestParam String origin) {
        // 获取飞书配置
        OauthPropertiesConfig.FeiShu feishuConfig = oauthPropertiesConfig.getFeishu();

        // 生成随机 state
        String state = RandomUtil.randomString(32);

        // ✅ 使用 JustAuth 默认的缓存单例
        AuthStateCache authStateCache = authRequestFactory.getAuthStateCache();
        log.info("[getFeishuAuthUrl][缓存实例 hashCode: {}]", System.identityHashCode(authStateCache));

        // 存储到 JustAuth 的缓存中（5分钟过期）
        authStateCache.cache(state, state, 300000);

        // ✅ 验证是否存储成功
        boolean exists = authStateCache.containsKey(state);
        log.info("[getFeishuAuthUrl][生成 state: {}, 缓存存储成功: {}, 缓存中是否存在: {}]",
                state, authStateCache.get(state), exists);

        // 返回前端需要的参数
        FeishuAuthVO vo = new FeishuAuthVO();
        vo.setClientId(feishuConfig.getClientId());
        vo.setState(state);

        return success(vo);
    }

    // 新增 VO 类
    @Data
    public static class FeishuAuthVO {
        private String clientId;
        private String state;
    }

    // ========== 新增：飞书免登的接口，仅做请求接收和结果返回 ==========
    @GetMapping("/feishu-sso-login")
    @PermitAll
    @Operation(summary = "飞书网页应用免登登录接口")
    public CommonResult<AuthLoginRespVO> feishuSsoLogin(@RequestParam("code") String code) {
        return success(authService.feishuSsoLogin(code));
    }

}
