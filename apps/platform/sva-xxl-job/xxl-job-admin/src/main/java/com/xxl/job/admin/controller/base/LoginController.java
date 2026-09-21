package com.xxl.job.admin.controller.base;

import com.xxl.job.admin.mapper.SystemUserMapper;
import com.xxl.job.admin.mapper.XxlJobUserMapper;
import com.xxl.job.admin.model.SystemUser;
import com.xxl.job.admin.model.XxlJobUser;
import com.xxl.job.admin.util.I18nUtil;
import com.xxl.sso.core.annotation.XxlSso;
import com.xxl.sso.core.helper.XxlSsoHelper;
import com.xxl.sso.core.model.LoginInfo;
import com.xxl.tool.core.StringTool;
import com.xxl.tool.crypto.Sha256Tool;
import com.xxl.tool.id.UUIDTool;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * index controller
 * @author xuxueli 2015-12-19 16:13:16
 */
@Controller
@RequestMapping("/auth")
public class LoginController {

	@Resource
	private XxlJobUserMapper xxlJobUserMapper;

	@Resource
	private SystemUserMapper systemUserMapper;

	@Resource
	private PasswordEncoder passwordEncoder;

	@RequestMapping("/login")
	@XxlSso(login = false)
	public ModelAndView login(HttpServletRequest request, HttpServletResponse response, ModelAndView modelAndView) {

		// xxl-sso, logincheck
		Response<LoginInfo> loginInfoResponse = XxlSsoHelper.loginCheckWithCookie(request, response);

		if (loginInfoResponse.isSuccess()) {
			modelAndView.setView(new RedirectView("/",true,false));
			return modelAndView;
		}
		return new ModelAndView("base/login");
	}

/*	@RequestMapping(value="/doLogin", method=RequestMethod.POST)
	@ResponseBody
	@XxlSso(login=false)
	public Response<String> doLogin(HttpServletRequest request, HttpServletResponse response, String userName, String password, String ifRemember){

		// param
		boolean ifRem = StringTool.isNotBlank(ifRemember) && "on".equals(ifRemember);
		if (StringTool.isBlank(userName) || StringTool.isBlank(password)){
			return Response.ofFail( I18nUtil.getString("login_param_empty") );
		}

		// valid user、status
//		XxlJobUser xxlJobUser = xxlJobUserMapper.loadByUserName(userName);
		SystemUser xxlJobUser = systemUserMapper.loadByUserName(userName);
		if (xxlJobUser == null) {
			return Response.ofFail( I18nUtil.getString("login_param_unvalid") );
		}

		// valid passowrd
//		String passwordHash = Sha256Tool.sha256(password);
//		if (!passwordHash.equals(xxlJobUser.getPassword())) {
//			return Response.ofFail( I18nUtil.getString("login_param_unvalid") );
//		}

		boolean matches = passwordEncoder.matches(password, xxlJobUser.getPassword());
		if (!matches) {
			return Response.ofFail( I18nUtil.getString("login_param_unvalid") );
		}

		// xxl-sso, do login
		LoginInfo loginInfo = new LoginInfo(String.valueOf(xxlJobUser.getId()), UUIDTool.getSimpleUUID());
		Response<String> result= XxlSsoHelper.loginWithCookie(loginInfo, response, ifRem);

		return Response.of(result.getCode(), result.getMsg());
	}*/

	@RequestMapping(value="/doLogin", method=RequestMethod.POST)
	@ResponseBody
	@XxlSso(login=false)
	public Response<String> doLogin(HttpServletRequest request, HttpServletResponse response,
									String userName, String password, String ifRemember){

		// param
		boolean ifRem = StringTool.isNotBlank(ifRemember) && "on".equals(ifRemember);
		if (StringTool.isBlank(userName) || StringTool.isBlank(password)){
			return Response.ofFail(I18nUtil.getString("login_param_empty"));
		}

		// 1. 从 system_user 表验证用户
		SystemUser systemUser = systemUserMapper.loadByUserName(userName);
		if (systemUser == null) {
			return Response.ofFail(I18nUtil.getString("login_param_unvalid"));
		}

		// 2. 验证密码
		boolean matches = passwordEncoder.matches(password, systemUser.getPassword());
		if (!matches) {
			return Response.ofFail(I18nUtil.getString("login_param_unvalid"));
		}

		// 3. 同步用户到 xxl_job_user 表（存在则更新密码，不存在则创建）
		XxlJobUser xxlJobUser = xxlJobUserMapper.loadByUserName(userName);
		if (xxlJobUser == null) {
			// 不存在则创建
			xxlJobUser = new XxlJobUser();
			xxlJobUser.setUsername(userName);
			xxlJobUser.setPassword(systemUser.getPassword());
			xxlJobUser.setRole(1); // 默认普通用户，可根据需要修改
			xxlJobUser.setPermission(""); // 默认全部权限
			xxlJobUserMapper.save(xxlJobUser);
			// 重新查询获取完整信息（包含id）
			xxlJobUser = xxlJobUserMapper.loadByUserName(userName);
		} else {
			// 存在则更新密码
			if (!xxlJobUser.getPassword().equals(systemUser.getPassword())) {
				xxlJobUser.setPassword(systemUser.getPassword());
				xxlJobUserMapper.update(xxlJobUser);
			}
		}

		if (xxlJobUser == null) {
			return Response.ofFail("用户同步失败");
		}

		// 4. SSO 登录（会自动生成并保存 token 到 xxl_job_user 表）
		LoginInfo loginInfo = new LoginInfo(String.valueOf(xxlJobUser.getId()), UUIDTool.getSimpleUUID());
		Response<String> result = XxlSsoHelper.loginWithCookie(loginInfo, response, ifRem);

		return Response.of(result.getCode(), result.getMsg());
	}

	@RequestMapping(value="/logout", method=RequestMethod.POST)
	@ResponseBody
	@XxlSso(login=false)
	public Response<String> logout(HttpServletRequest request, HttpServletResponse response){

		// xxl-sso, do logout
		Response<String> result = XxlSsoHelper.logoutWithCookie(request, response);

		return Response.of(result.getCode(), result.getMsg());
	}

	@RequestMapping("/updatePwd")
	@ResponseBody
	@XxlSso
	public Response<String> updatePwd(HttpServletRequest request, String oldPassword, String password){

		// valid
		if (oldPassword==null || oldPassword.trim().isEmpty()){
			return Response.ofFail(I18nUtil.getString("system_please_input") + I18nUtil.getString("change_pwd_field_oldpwd"));
		}
		if (password==null || password.trim().isEmpty()){
			return Response.ofFail(I18nUtil.getString("system_please_input") + I18nUtil.getString("change_pwd_field_oldpwd"));
		}
		password = password.trim();
		if (!(password.length()>=4 && password.length()<=20)) {
			return Response.ofFail(I18nUtil.getString("system_lengh_limit")+"[4-20]" );
		}

		// md5 password
		String oldPasswordHash = Sha256Tool.sha256(oldPassword);
		String passwordHash = Sha256Tool.sha256(password);

		// valid old pwd
		Response<LoginInfo> loginInfoResponse = XxlSsoHelper.loginCheckWithAttr(request);
		XxlJobUser existUser = xxlJobUserMapper.loadByUserName(loginInfoResponse.getData().getUserName());
		if (!oldPasswordHash.equals(existUser.getPassword())) {
			return Response.ofFail(I18nUtil.getString("change_pwd_field_oldpwd") + I18nUtil.getString("system_unvalid"));
		}

		// write new
		existUser.setPassword(passwordHash);
		xxlJobUserMapper.update(existUser);

		return Response.ofSuccess();
	}

}
