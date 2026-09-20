package cn.iocoder.sva.module.system.service.auth;

import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.sva.module.system.config.OauthPropertiesConfig;
import cn.iocoder.sva.module.system.service.auth.dto.FeishuUserDetailDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static cn.iocoder.sva.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.sva.module.system.enums.ErrorCodeConstants.*;

/**
 * 飞书 Service 实现-wy-
 */
@Service
@Slf4j
public class FeiShuServiceImpl implements FeiShuService{

    /**
     * 获取飞书用户详细信息（包含工号）-wy-
     */
    @Override
    public FeishuUserDetailDTO getFeishuUserDetail(String accessToken, String userId, String userIdType) {
        try {
            // 1. 构建请求 URL
            String url = "https://open.feishu.cn/open-apis/contact/v3/users/" + userId;

            // 2. 创建 HTTP 请求
            HttpRequest request = HttpRequest.get(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json");

            // 3. 添加查询参数（可选）
            if (userIdType != null && !userIdType.isEmpty()) {
                request.form("user_id_type", userIdType);
            }
            // 指定返回部门 ID 类型
            request.form("department_id_type", "open_department_id");

            // 4. 执行请求
            String responseBody = request.execute().body();
            log.info("[getFeishuUserDetail][请求URL: {}, 响应: {}]", url, responseBody);

            // 5. 解析响应
            JSONObject response = JSONUtil.parseObj(responseBody);
            Integer code = response.getInt("code");

            if (code != 0) {
                String msg = response.getStr("msg");
                log.error("[getFeishuUserDetail][获取飞书用户信息失败: code={}, msg={}]", code, msg);
                throw exception(SOCIAL_USER_AUTH_FAILURE, "获取飞书用户信息失败: " + msg);
            }

            // 6. 提取用户数据
            JSONObject data = response.getJSONObject("data");
            JSONObject user = data.getJSONObject("user");

            // 7. 转换为 DTO
            FeishuUserDetailDTO dto = new FeishuUserDetailDTO();
            dto.setUnionId(user.getStr("union_id"));
            dto.setUserId(user.getStr("user_id"));
            dto.setOpenId(user.getStr("open_id"));
            dto.setName(user.getStr("name"));
            dto.setEnName(user.getStr("en_name"));
            dto.setNickname(user.getStr("nickname"));
            dto.setEmail(user.getStr("email"));
            dto.setMobile(user.getStr("mobile"));
            dto.setGender(user.getInt("gender"));
            dto.setEmployeeNo(user.getStr("employee_no"));  // 工号
            dto.setEmployeeType(user.getInt("employee_type"));
            dto.setJobTitle(user.getStr("job_title"));
            dto.setEnterpriseEmail(user.getStr("enterprise_email"));
            dto.setCity(user.getStr("city"));
            dto.setCountry(user.getStr("country"));
            dto.setJoinTime(user.getLong("join_time"));
            dto.setIsTenantManager(user.getBool("is_tenant_manager"));
            dto.setWorkStation(user.getStr("work_station"));

            // 用户状态
            JSONObject status = user.getJSONObject("status");
            if (status != null) {
                FeishuUserDetailDTO.UserStatus userStatus = new FeishuUserDetailDTO.UserStatus();
                userStatus.setIsFrozen(status.getBool("is_frozen"));
                userStatus.setIsResigned(status.getBool("is_resigned"));
                userStatus.setIsActivated(status.getBool("is_activated"));
                userStatus.setIsExited(status.getBool("is_exited"));
                userStatus.setIsUnjoin(status.getBool("is_unjoin"));
                dto.setStatus(userStatus);
            }

            // 头像
            JSONObject avatar = user.getJSONObject("avatar");
            if (avatar != null) {
                FeishuUserDetailDTO.Avatar avatarDto = new FeishuUserDetailDTO.Avatar();
                avatarDto.setAvatar72(avatar.getStr("avatar_72"));
                avatarDto.setAvatar240(avatar.getStr("avatar_240"));
                avatarDto.setAvatar640(avatar.getStr("avatar_640"));
                avatarDto.setAvatarOrigin(avatar.getStr("avatar_origin"));
                dto.setAvatar(avatarDto);
            }

            // 部门ID列表
            dto.setDepartmentIds(user.getBeanList("department_ids", String.class));

            // 直接主管
            dto.setLeaderUserId(user.getStr("leader_user_id"));

            log.info("[getFeishuUserDetail][获取飞书用户信息成功: openId={}, employeeNo={}, name={}]",
                    dto.getOpenId(), dto.getEmployeeNo(), dto.getName());

            return dto;

        } catch (Exception e) {
            log.error("[getFeishuUserDetail][获取飞书用户信息异常]", e);
            throw exception(SOCIAL_USER_AUTH_FAILURE, "获取飞书用户信息失败: " + e.getMessage());
        }
    }

}
