package cn.iocoder.sva.module.system.controller.admin.user.vo.user;

import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.sva.framework.common.validation.Mobile;
import cn.iocoder.sva.module.system.framework.operatelog.core.DeptParseFunction;
import cn.iocoder.sva.module.system.framework.operatelog.core.PostParseFunction;
import cn.iocoder.sva.module.system.framework.operatelog.core.SexParseFunction;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mzt.logapi.starter.annotation.DiffLogField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;
import java.util.Set;

@Schema(description = "管理后台 - 用户创建/修改 Request VO")
@Data
public class UserSaveReqVO {

    @Schema(description = "用户编号", example = "1024")
    private Long id;

    @Schema(description = "用户账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "sva")
    @NotBlank(message = "用户账号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{4,30}$", message = "用户账号由 数字、字母 组成")
    @Size(min = 4, max = 30, message = "用户账号长度为 4-30 个字符")
    @DiffLogField(name = "用户账号")
    private String username;

    @Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋艿")
    @Size(max = 30, message = "用户昵称长度不能超过30个字符")
    @DiffLogField(name = "用户昵称")
    private String nickname;

    @Schema(description = "备注", example = "我是一个用户")
    @DiffLogField(name = "备注")
    private String remark;

    @Schema(description = "部门编号", example = "我是一个用户")
    @DiffLogField(name = "部门", function = DeptParseFunction.NAME)
    private Long deptId;

    @Schema(description = "岗位编号数组", example = "1")
    @DiffLogField(name = "岗位", function = PostParseFunction.NAME)
    private Set<Long> postIds;

    @Schema(description = "用户邮箱", example = "kx@iocoder.cn")
    @Email(message = "邮箱格式不正确")
    @Size(max = 50, message = "邮箱长度不能超过 50 个字符")
    @DiffLogField(name = "用户邮箱")
    private String email;

    @Schema(description = "手机号码", example = "15601691300")
    @Mobile
    @DiffLogField(name = "手机号码")
    private String mobile;

    @Schema(description = "用户性别，参见 SexEnum 枚举类", example = "1")
    @DiffLogField(name = "用户性别", function = SexParseFunction.NAME)
    private Integer sex;

    @Schema(description = "用户头像", example = "https://www.iocoder.cn/xxx.png")
    @DiffLogField(name = "用户头像")
    private String avatar;

    // ========== 仅【创建】时，需要传递的字段 ==========

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @Length(min = 4, max = 16, message = "密码长度为 4-16 位")
    private String password;

    @AssertTrue(message = "密码不能为空")
    @JsonIgnore
    public boolean isPasswordValid() {
        return id != null // 修改时，不需要传递
                || (ObjectUtil.isAllNotEmpty(password)); // 新增时，必须都传递 password
    }

    // ========== 新增字段 ==========

    @Schema(description = "唯一ID", example = "550e8400-e29b-41d4-a716-446655440000")
    @Size(max = 36, message = "唯一ID长度不能超过 36 个字符")
    private String guid;

    @Schema(description = "生效日期", example = "2024-01-01")
    private LocalDate effdt;

    @Schema(description = "员工类型ID", example = "001")
    @Size(max = 3, message = "员工类型ID长度不能超过 3 个字符")
    private String emplClass;

    @Schema(description = "员工类型", example = "正式员工")
    @Size(max = 128, message = "员工类型长度不能超过 128 个字符")
    private String dcEmplClsDescr;

    @Schema(description = "HR状态", example = "A")
    @Size(max = 1, message = "HR状态长度不能超过 1 个字符")
    private String hrStatus;

    @Schema(description = "正式/临时", example = "1")
    @Size(max = 1, message = "正式/临时长度不能超过 1 个字符")
    private String regTemp;

    @Schema(description = "直接上级岗位ID", example = "10001")
    @Size(max = 8, message = "直接上级岗位ID长度不能超过 8 个字符")
    private String reportsTo;

    @Schema(description = "主/兼岗", example = "1")
    @Size(max = 1, message = "主/兼岗长度不能超过 1 个字符")
    private String jobIndicator;

    @Schema(description = "主/兼岗描述", example = "主岗")
    @Size(max = 128, message = "主/兼岗描述长度不能超过 128 个字符")
    private String jobIndicatorDescr;

    @Schema(description = "岗位ID", example = "10001")
    @Size(max = 8, message = "岗位ID长度不能超过 8 个字符")
    private String positionNbr;

    @Schema(description = "岗位", example = "Java开发工程师")
    @Size(max = 200, message = "岗位长度不能超过 200 个字符")
    private String dcPositionDescr;

    @Schema(description = "部门", example = "技术研发部")
    @Size(max = 128, message = "部门长度不能超过 128 个字符")
    private String dcDeptDescr50;

    @Schema(description = "部门负责人岗位ID", example = "20001")
    @Size(max = 8, message = "部门负责人岗位ID长度不能超过 8 个字符")
    private String managerPosn;

    @Schema(description = "部门总监岗位ID", example = "30001")
    @Size(max = 8, message = "部门总监岗位ID长度不能超过 8 个字符")
    private String dcDirectorPosn;

    @Schema(description = "转正日期", example = "2024-07-01")
    private LocalDate probationDt;

    @Schema(description = "职级", example = "P7")
    @Size(max = 4, message = "职级长度不能超过 4 个字符")
    private String dcJobLevel;

    @Schema(description = "职级描述", example = "高级工程师")
    @Size(max = 128, message = "职级描述长度不能超过 128 个字符")
    private String dcJobLevelDescr;

    @Schema(description = "职等", example = "3")
    @Size(max = 4, message = "职等长度不能超过 4 个字符")
    private String dcJobGrade;

    @Schema(description = "职等描述", example = "中级")
    @Size(max = 128, message = "职等描述长度不能超过 128 个字符")
    private String dcJobGradeDescr;

    @Schema(description = "职层", example = "M")
    @Size(max = 4, message = "职层长度不能超过 4 个字符")
    private String dcJobStage;

    @Schema(description = "职层描述", example = "管理层")
    @Size(max = 128, message = "职层描述长度不能超过 128 个字符")
    private String dcJobStageDescr;

    @Schema(description = "入职时间", example = "2024-01-01")
    private LocalDate lastHireDt;

    @Schema(description = "公司ID", example = "001")
    @Size(max = 3, message = "公司ID长度不能超过 3 个字符")
    private String company;

    @Schema(description = "公司", example = "科兴科技有限公司")
    @Size(max = 128, message = "公司长度不能超过 128 个字符")
    private String dcCompanyDescr;

    @Schema(description = "业务单位ID", example = "BU001")
    @Size(max = 5, message = "业务单位ID长度不能超过 5 个字符")
    private String businessUnit;

    @Schema(description = "业务单位", example = "中国区业务部")
    @Size(max = 64, message = "业务单位长度不能超过 64 个字符")
    private String businessDescr;
}