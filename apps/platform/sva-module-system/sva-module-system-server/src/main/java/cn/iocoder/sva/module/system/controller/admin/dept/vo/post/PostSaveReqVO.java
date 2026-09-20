package cn.iocoder.sva.module.system.controller.admin.dept.vo.post;

import cn.iocoder.sva.framework.common.enums.CommonStatusEnum;
import cn.iocoder.sva.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 岗位创建/修改 Request VO")
@Data
public class PostSaveReqVO {

    @Schema(description = "岗位编号", example = "1024")
    private Long id;

    @Schema(description = "岗位名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "小土豆")
    @NotBlank(message = "岗位名称不能为空")
    @Size(max = 50, message = "岗位名称长度不能超过 50 个字符")
    private String name;

    @Schema(description = "岗位编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "sva")
    @NotBlank(message = "岗位编码不能为空")
    @Size(max = 64, message = "岗位编码长度不能超过64个字符")
    private String code;

    @Schema(description = "显示顺序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "显示顺序不能为空")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @InEnum(CommonStatusEnum.class)
    private Integer status;

    @Schema(description = "备注", example = "快乐的备注")
    private String remark;

    // ========== 新增字段 ==========

    @Schema(description = "唯一ID", example = "550e8400-e29b-41d4-a716-446655440000")
    @Size(max = 64, message = "唯一ID长度不能超过 64 个字符")
    private String guid;

    @Schema(description = "岗位简称", example = "研发组长")
    @Size(max = 64, message = "岗位简称长度不能超过 64 个字符")
    private String descrshort;

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

    @Schema(description = "直接汇报岗位ID", example = "10001")
    @Size(max = 8, message = "直接汇报岗位ID长度不能超过 8 个字符")
    private String reportsTo;

    @Schema(description = "直接汇报岗位", example = "技术总监")
    @Size(max = 50, message = "直接汇报岗位长度不能超过 50 个字符")
    private String reportsToDescr;

    @Schema(description = "岗位英文名称", example = "Software Engineer")
    @Size(max = 128, message = "岗位英文名称长度不能超过 128 个字符")
    private String dcPositionDescra;

    @Schema(description = "部门ID", example = "100")
    private Long deptid;

    @Schema(description = "部门名称", example = "技术研发部")
    @Size(max = 128, message = "部门名称长度不能超过 128 个字符")
    private String dcDeptDescr50;

    @Schema(description = "业务单位ID", example = "BU001")
    @Size(max = 5, message = "业务单位ID长度不能超过 5 个字符")
    private String businessUnit;

    @Schema(description = "业务单位", example = "中国区业务部")
    @Size(max = 60, message = "业务单位长度不能超过 60 个字符")
    private String businessDescr;
}