package cn.iocoder.sva.module.system.controller.admin.user.vo.user;

import cn.iocoder.sva.framework.excel.core.annotations.DictFormat;
import cn.iocoder.sva.framework.excel.core.convert.DictConvert;
import cn.iocoder.sva.module.system.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Set;

@Schema(description = "管理后台 - 用户信息 Response VO")
@Data
@ExcelIgnoreUnannotated
public class UserRespVO{

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("用户编号")
    private Long id;

    @Schema(description = "用户账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "sva")
    @ExcelProperty("用户名称")
    private String username;

    @Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋艿")
    @ExcelProperty("用户昵称")
    private String nickname;

    @Schema(description = "备注", example = "我是一个用户")
    private String remark;

    @Schema(description = "部门ID", example = "我是一个用户")
    private Long deptId;

    @Schema(description = "部门名称", example = "IT 部")
    @ExcelProperty("部门名称")
    private String deptName;

    @Schema(description = "岗位编号数组", example = "1")
    private Set<Long> postIds;

    @Schema(description = "用户邮箱", example = "kx@iocoder.cn")
    @ExcelProperty("用户邮箱")
    private String email;

    @Schema(description = "手机号码", example = "15601691300")
    @ExcelProperty("手机号码")
    private String mobile;

    @Schema(description = "用户性别，参见 SexEnum 枚举类", example = "1")
    @ExcelProperty(value = "用户性别", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.USER_SEX)
    private Integer sex;

    @Schema(description = "用户头像", example = "https://www.iocoder.cn/xxx.png")
    private String avatar;

    @Schema(description = "状态，参见 CommonStatusEnum 枚举类", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty(value = "帐号状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.COMMON_STATUS)
    private Integer status;

    @Schema(description = "最后登录 IP", requiredMode = Schema.RequiredMode.REQUIRED, example = "192.168.1.1")
    @ExcelProperty("最后登录IP")
    private String loginIp;

    @Schema(description = "最后登录时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "时间戳格式")
    @ExcelProperty("最后登录时间")
    private LocalDateTime loginDate;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "时间戳格式")
    private LocalDateTime createTime;

    // ========== 新增字段 ==========

    @Schema(description = "唯一ID", example = "550e8400-e29b-41d4-a716-446655440000")
    @ExcelProperty("唯一ID")
    private String guid;

    @Schema(description = "生效日期", example = "2024-01-01")
    @ExcelProperty("生效日期")
    private LocalDate effdt;

    @Schema(description = "员工类型ID", example = "001")
    @ExcelProperty("员工类型ID")
    private String emplClass;

    @Schema(description = "员工类型", example = "正式员工")
    @ExcelProperty("员工类型")
    private String dcEmplClsDescr;

    @Schema(description = "HR状态", example = "A")
    @ExcelProperty("HR状态")
    private String hrStatus;

    @Schema(description = "正式/临时", example = "1")
    @ExcelProperty("正式/临时")
    private String regTemp;

    @Schema(description = "直接上级岗位ID", example = "10001")
    @ExcelProperty("直接上级岗位ID")
    private String reportsTo;

    @Schema(description = "主/兼岗", example = "1")
    @ExcelProperty("主/兼岗")
    private String jobIndicator;

    @Schema(description = "主/兼岗描述", example = "主岗")
    @ExcelProperty("主/兼岗描述")
    private String jobIndicatorDescr;

    @Schema(description = "岗位ID", example = "10001")
    @ExcelProperty("岗位ID")
    private String positionNbr;

    @Schema(description = "岗位", example = "Java开发工程师")
    @ExcelProperty("岗位")
    private String dcPositionDescr;

    @Schema(description = "部门", example = "技术研发部")
    @ExcelProperty("部门")
    private String dcDeptDescr50;

    @Schema(description = "部门负责人岗位ID", example = "20001")
    @ExcelProperty("部门负责人岗位ID")
    private String managerPosn;

    @Schema(description = "部门总监岗位ID", example = "30001")
    @ExcelProperty("部门总监岗位ID")
    private String dcDirectorPosn;

    @Schema(description = "转正日期", example = "2024-07-01")
    @ExcelProperty("转正日期")
    private LocalDate probationDt;

    @Schema(description = "职级", example = "P7")
    @ExcelProperty("职级")
    private String dcJobLevel;

    @Schema(description = "职级描述", example = "高级工程师")
    @ExcelProperty("职级描述")
    private String dcJobLevelDescr;

    @Schema(description = "职等", example = "3")
    @ExcelProperty("职等")
    private String dcJobGrade;

    @Schema(description = "职等描述", example = "中级")
    @ExcelProperty("职等描述")
    private String dcJobGradeDescr;

    @Schema(description = "职层", example = "M")
    @ExcelProperty("职层")
    private String dcJobStage;

    @Schema(description = "职层描述", example = "管理层")
    @ExcelProperty("职层描述")
    private String dcJobStageDescr;

    @Schema(description = "入职时间", example = "2024-01-01")
    @ExcelProperty("入职时间")
    private LocalDate lastHireDt;

    @Schema(description = "公司ID", example = "001")
    @ExcelProperty("公司ID")
    private String company;

    @Schema(description = "公司", example = "科兴科技有限公司")
    @ExcelProperty("公司")
    private String dcCompanyDescr;

    @Schema(description = "业务单位ID", example = "BU001")
    @ExcelProperty("业务单位ID")
    private String businessUnit;

    @Schema(description = "业务单位", example = "中国区业务部")
    @ExcelProperty("业务单位")
    private String businessDescr;
}