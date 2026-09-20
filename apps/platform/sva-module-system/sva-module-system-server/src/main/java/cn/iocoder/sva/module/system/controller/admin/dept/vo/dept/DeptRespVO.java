package cn.iocoder.sva.module.system.controller.admin.dept.vo.dept;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 部门信息 Response VO")
@Data
public class DeptRespVO {

    @Schema(description = "部门编号", example = "1024")
    private Long id;

    @Schema(description = "部门名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "科兴")
    private String name;

    @Schema(description = "父部门 ID", example = "1024")
    private Long parentId;

    @Schema(description = "显示顺序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Integer sort;

    @Schema(description = "负责人的用户编号", example = "2048")
    private Long leaderUserId;

    @Schema(description = "联系电话", example = "15601691000")
    private String phone;

    @Schema(description = "邮箱", example = "kx@iocoder.cn")
    private String email;

    @Schema(description = "状态,见 CommonStatusEnum 枚举", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "时间戳格式")
    private LocalDateTime createTime;

    // ========== 新增字段 ==========

    @Schema(description = "唯一ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String guid;

    @Schema(description = "生效日期", example = "2024-01-01")
    private LocalDate effdt;

    @Schema(description = "生效状态", example = "A")
    private String effStatus;

    @Schema(description = "部门简称", example = "研发部")
    private String descrshort;

    @Schema(description = "所属公司ID", example = "COMP001")
    private String company;

    @Schema(description = "所属公司名称", example = "科兴科技有限公司")
    private String dcCompanyDescr;

    @Schema(description = "组织类型", example = "001")
    private String dcOrgType;

    @Schema(description = "组织类型名称", example = "职能部门")
    private String dcOrgTypeDescr;

    @Schema(description = "组织类别", example = "001")
    private String dcOrgLevel;

    @Schema(description = "组织类别名称", example = "一级部门")
    private String dcOrgLevelDescr;

    @Schema(description = "上级部门名称", example = "总公司")
    private String dcParDeptDescr;

    @Schema(description = "地点ID", example = "LOC001")
    private String location;

    @Schema(description = "地点名称", example = "北京总部")
    private String dcLocationDescr;

    @Schema(description = "部门职责", example = "负责公司技术研发工作")
    private String dcDeptRespon;

    @Schema(description = "部门全路径", example = "/总公司/研发部/后端组")
    private String dcDeptFullCode;

    @Schema(description = "部门全路径名称", example = "总公司-研发部-后端组")
    private String dcDeptFullDescr;
}