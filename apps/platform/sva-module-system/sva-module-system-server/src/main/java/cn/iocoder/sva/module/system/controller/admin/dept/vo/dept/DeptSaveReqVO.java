package cn.iocoder.sva.module.system.controller.admin.dept.vo.dept;

import cn.iocoder.sva.framework.common.enums.CommonStatusEnum;
import cn.iocoder.sva.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - 部门创建/修改 Request VO")
@Data
public class DeptSaveReqVO {

    @Schema(description = "部门编号", example = "1024")
    private Long id;

    @Schema(description = "部门名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "科兴")
    @NotBlank(message = "部门名称不能为空")
    @Size(max = 30, message = "部门名称长度不能超过 30 个字符")
    private String name;

    @Schema(description = "父部门 ID", example = "1024")
    private Long parentId;

    @Schema(description = "显示顺序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "显示顺序不能为空")
    private Integer sort;

    @Schema(description = "负责人的用户编号", example = "2048")
    private Long leaderUserId;

    @Schema(description = "联系电话", example = "15601691000")
    @Size(max = 11, message = "联系电话长度不能超过11个字符")
    private String phone;

    @Schema(description = "邮箱", example = "kx@iocoder.cn")
    @Email(message = "邮箱格式不正确")
    @Size(max = 50, message = "邮箱长度不能超过 50 个字符")
    private String email;

    @Schema(description = "状态,见 CommonStatusEnum 枚举", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "修改状态必须是 {value}")
    private Integer status;

    // ========== 新增字段 ==========

    @Schema(description = "唯一ID", example = "550e8400-e29b-41d4-a716-446655440000")
    @Size(max = 36, message = "唯一ID长度不能超过 36 个字符")
    private String guid;

    @Schema(description = "生效日期", example = "2024-01-01")
    private LocalDate effdt;

    @Schema(description = "生效状态", example = "A")
    @Size(max = 1, message = "生效状态长度不能超过 1 个字符")
    private String effStatus;

    @Schema(description = "部门简称", example = "研发部")
    @Size(max = 20, message = "部门简称长度不能超过 20 个字符")
    private String descrshort;

    @Schema(description = "所属公司ID", example = "COMP001")
    @Size(max = 12, message = "所属公司ID长度不能超过 12 个字符")
    private String company;

    @Schema(description = "所属公司名称", example = "科兴科技有限公司")
    @Size(max = 64, message = "所属公司名称长度不能超过 64 个字符")
    private String dcCompanyDescr;

    @Schema(description = "组织类型", example = "001")
    @Size(max = 3, message = "组织类型长度不能超过 3 个字符")
    private String dcOrgType;

    @Schema(description = "组织类型名称", example = "职能部门")
    @Size(max = 12, message = "组织类型名称长度不能超过 12 个字符")
    private String dcOrgTypeDescr;

    @Schema(description = "组织类别", example = "001")
    @Size(max = 3, message = "组织类别长度不能超过 3 个字符")
    private String dcOrgLevel;

    @Schema(description = "组织类别名称", example = "一级部门")
    @Size(max = 12, message = "组织类别名称长度不能超过 12 个字符")
    private String dcOrgLevelDescr;

    @Schema(description = "上级部门名称", example = "总公司")
    @Size(max = 64, message = "上级部门名称长度不能超过 64 个字符")
    private String dcParDeptDescr;

    @Schema(description = "地点ID", example = "LOC001")
    @Size(max = 10, message = "地点ID长度不能超过 10 个字符")
    private String location;

    @Schema(description = "地点名称", example = "北京总部")
    @Size(max = 128, message = "地点名称长度不能超过 128 个字符")
    private String dcLocationDescr;

    @Schema(description = "部门职责", example = "负责公司技术研发工作")
    @Size(max = 255, message = "部门职责长度不能超过 255 个字符")
    private String dcDeptRespon;

    @Schema(description = "部门全路径", example = "/总公司/研发部/后端组")
    @Size(max = 255, message = "部门全路径长度不能超过 255 个字符")
    private String dcDeptFullCode;

    @Schema(description = "部门全路径名称", example = "总公司-研发部-后端组")
    @Size(max = 255, message = "部门全路径名称长度不能超过 255 个字符")
    private String dcDeptFullDescr;
}