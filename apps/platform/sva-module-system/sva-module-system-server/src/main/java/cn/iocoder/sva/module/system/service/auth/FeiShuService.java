package cn.iocoder.sva.module.system.service.auth;

import cn.iocoder.sva.module.system.service.auth.dto.FeishuUserDetailDTO;

/**
 * 飞书 Service -wy-
 */
public interface FeiShuService {

    /**
     * 获取飞书用户详细信息（包含工号、邮箱、手机号等）
     * @param accessToken 访问令牌（user_access_token 或 tenant_access_token）
     * @param userId 用户ID（open_id、union_id 或 user_id）
     * @param userIdType 用户ID类型（open_id、union_id、user_id），默认 open_id
     * @return 飞书用户详细信息
     */
    FeishuUserDetailDTO getFeishuUserDetail(String accessToken, String userId, String userIdType);

}
