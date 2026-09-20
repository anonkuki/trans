package cn.iocoder.sva.module.system.service.auth.dto;

import lombok.Data;
import java.util.List;

/**
 * 飞书用户详细信息 DTO -wy-
 */
@Data
public class FeishuUserDetailDTO {

    /**
     * 用户的 union_id
     */
    private String unionId;

    /**
     * 用户的 user_id（租户内唯一）
     */
    private String userId;

    /**
     * 用户的 open_id（应用内唯一）
     */
    private String openId;

    /**
     * 用户名称
     */
    private String name;

    /**
     * 英文名
     */
    private String enName;

    /**
     * 别名
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 性别：0-保密，1-男，2-女，3-其他
     */
    private Integer gender;

    /**
     * 工号
     */
    private String employeeNo;

    /**
     * 员工类型：1-正式员工，2-实习生，3-外包，4-劳务，5-顾问
     */
    private Integer employeeType;

    /**
     * 职务
     */
    private String jobTitle;

    /**
     * 企业邮箱
     */
    private String enterpriseEmail;

    /**
     * 工作城市
     */
    private String city;

    /**
     * 国家/地区 Code
     */
    private String country;

    /**
     * 入职时间（秒级时间戳）
     */
    private Long joinTime;

    /**
     * 是否为租户超级管理员
     */
    private Boolean isTenantManager;

    /**
     * 用户状态
     */
    private UserStatus status;

    /**
     * 头像信息
     */
    private Avatar avatar;

    /**
     * 所属部门ID列表
     */
    private List<String> departmentIds;

    /**
     * 直接主管的用户ID
     */
    private String leaderUserId;

    /**
     * 工位
     */
    private String workStation;

    @Data
    public static class UserStatus {
        private Boolean isFrozen;      // 是否暂停
        private Boolean isResigned;    // 是否离职
        private Boolean isActivated;   // 是否激活
        private Boolean isExited;      // 是否主动退出
        private Boolean isUnjoin;      // 是否未加入
    }

    @Data
    public static class Avatar {
        private String avatar72;
        private String avatar240;
        private String avatar640;
        private String avatarOrigin;
    }
}