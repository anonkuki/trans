package cn.iocoder.sva.module.system.controller.admin.sync.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 全部人员信息同步 Response VO")
@Data
@ExcelIgnoreUnannotated
public class SyncStaffRespVO {

    @Schema(description = "自增主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "10206")
    @ExcelProperty("自增主键")
    private Long id;

    @Schema(description = "唯一ID", example = "18433")
    @ExcelProperty("唯一ID")
    private String guid;

    @Schema(description = "数据状态：A-新增，U-更新，D-删除", example = "2")
    @ExcelProperty("数据状态：A-新增，U-更新，D-删除")
    private String dcInfDtStatus;

    @Schema(description = "数据状态描述")
    @ExcelProperty("数据状态描述")
    private String dcInfDtStatusDescr;

    @Schema(description = "员工ID", example = "21234")
    @ExcelProperty("员工ID")
    private String emplid;

    @Schema(description = "姓名")
    @ExcelProperty("姓名")
    private String nameDisplay;

    @Schema(description = "性别：F-女，M-男")
    @ExcelProperty("性别：F-女，M-男")
    private String sex;

    @Schema(description = "性别描述")
    @ExcelProperty("性别描述")
    private String sexDescr;

    @Schema(description = "公司邮箱")
    @ExcelProperty("公司邮箱")
    private String emailAddr;

    @Schema(description = "手机")
    @ExcelProperty("手机")
    private String phone;

    @Schema(description = "员工类型ID：40-专家顾问,60-劳务外包,50-劳务派遣,20-实习生,10-正式员工,30-退休返聘")
    @ExcelProperty("员工类型ID：40-专家顾问,60-劳务外包,50-劳务派遣,20-实习生,10-正式员工,30-退休返聘")
    private String emplClass;

    @Schema(description = "员工类型")
    @ExcelProperty("员工类型")
    private String dcEmplClsDescr;

    @Schema(description = "HR状态：A-在职，I-离职", example = "2")
    @ExcelProperty("HR状态：A-在职，I-离职")
    private String hrStatus;

    @Schema(description = "HR状态描述")
    @ExcelProperty("HR状态描述")
    private String hrStatusDescr;

    @Schema(description = "正式/临时：R-正式，T-临时")
    @ExcelProperty("正式/临时：R-正式，T-临时")
    private String regTemp;

    @Schema(description = "正式/临时描述")
    @ExcelProperty("正式/临时描述")
    private String regTempDescr;

    @Schema(description = "直接上级岗位ID")
    @ExcelProperty("直接上级岗位ID")
    private String reportsTo;

    @Schema(description = "岗位ID")
    @ExcelProperty("岗位ID")
    private String positionNbr;

    @Schema(description = "岗位")
    @ExcelProperty("岗位")
    private String dcPositionDescr;

    @Schema(description = "管理区域ID")
    @ExcelProperty("管理区域ID")
    private String regRegion;

    @Schema(description = "公司ID")
    @ExcelProperty("公司ID")
    private String company;

    @Schema(description = "公司")
    @ExcelProperty("公司")
    private String dcCompanyDescr;

    @Schema(description = "业务单位ID")
    @ExcelProperty("业务单位ID")
    private String businessUnit;

    @Schema(description = "业务单位")
    @ExcelProperty("业务单位")
    private String businessDescr;

    @Schema(description = "部门ID", example = "32488")
    @ExcelProperty("部门ID")
    private String deptid;

    @Schema(description = "部门")
    @ExcelProperty("部门")
    private String dcDeptDescr50;

    @Schema(description = "部门负责人岗位ID")
    @ExcelProperty("部门负责人岗位ID")
    private String managerPosn;

    @Schema(description = "部门总监岗位ID")
    @ExcelProperty("部门总监岗位ID")
    private String dcDirectorPosn;

    @Schema(description = "部门副总经理岗位ID")
    @ExcelProperty("部门副总经理岗位ID")
    private String dcManagerPosn;

    @Schema(description = "成本中心ID")
    @ExcelProperty("成本中心ID")
    private String dcCostCenter;

    @Schema(description = "转正日期")
    @ExcelProperty("转正日期")
    private LocalDate probationDt;

    @Schema(description = "工作城市ID")
    @ExcelProperty("工作城市ID")
    private String location;

    @Schema(description = "工作城市")
    @ExcelProperty("工作城市")
    private String dcLocationDescr;

    @Schema(description = "是否博士后：Y-是，N-否")
    @ExcelProperty("是否博士后：Y-是，N-否")
    private String dcPdhFellowYn;

    @Schema(description = "是否博士后描述")
    @ExcelProperty("是否博士后描述")
    private String dcPdhFellowYnDescr;

    @Schema(description = "是否残疾人挂靠：Y-是，N-否")
    @ExcelProperty("是否残疾人挂靠：Y-是，N-否")
    private String dcDisabledYn;

    @Schema(description = "是否残疾人挂靠描述")
    @ExcelProperty("是否残疾人挂靠描述")
    private String dcDisabledYnDescr;

    @Schema(description = "入职时间")
    @ExcelProperty("入职时间")
    private LocalDate lastHireDt;

    @Schema(description = "实习生入职时间")
    @ExcelProperty("实习生入职时间")
    private LocalDate dcInternHireDt;

    @Schema(description = "标准岗位ID")
    @ExcelProperty("标准岗位ID")
    private String jobcode;

    @Schema(description = "标准岗位名称")
    @ExcelProperty("标准岗位名称")
    private String dcJobcodeDescr;

    @Schema(description = "岗位开始日期")
    @ExcelProperty("岗位开始日期")
    private LocalDate positionEntryDt;

    @Schema(description = "序列ID")
    @ExcelProperty("序列ID")
    private String dcJobGroup;

    @Schema(description = "序列")
    @ExcelProperty("序列")
    private String dcJobGroupDescr;

    @Schema(description = "通道ID")
    @ExcelProperty("通道ID")
    private String dcJobSequence;

    @Schema(description = "通道")
    @ExcelProperty("通道")
    private String dcJobSeqDescr;

    @Schema(description = "子序列ID")
    @ExcelProperty("子序列ID")
    private String dcFirstJobCate;

    @Schema(description = "子序列")
    @ExcelProperty("子序列")
    private String dcJobcateDescr;

    @Schema(description = "职层")
    @ExcelProperty("职层")
    private String dcJobStage;

    @Schema(description = "职层描述")
    @ExcelProperty("职层描述")
    private String dcJobStageDescr;

    @Schema(description = "职级")
    @ExcelProperty("职级")
    private String dcJobLevel;

    @Schema(description = "职级描述")
    @ExcelProperty("职级描述")
    private String dcJobLevelDescr;

    @Schema(description = "职等")
    @ExcelProperty("职等")
    private String dcJobGrade;

    @Schema(description = "职等描述")
    @ExcelProperty("职等描述")
    private String dcJobGradeDescr;

    @Schema(description = "sap公司编码", example = "1053")
    @ExcelProperty("sap公司编码")
    private String dcSapCompanyid;

    @Schema(description = "员工层级")
    @ExcelProperty("员工层级")
    private String dcEmpLevel;

    @Schema(description = "员工职层：A-高层，B-中层，C-员工")
    @ExcelProperty("员工职层：A-高层，B-中层，C-员工")
    private String dcEmplStage;

    @Schema(description = "员工职层描述")
    @ExcelProperty("员工职层描述")
    private String dcEmplStageDescr;

    @Schema(description = "证件类型：NID-身份证, HKMID-港澳居民来往内地通行证, TWMID-台胞证, PASS-护照", example = "2")
    @ExcelProperty("证件类型：NID-身份证, HKMID-港澳居民来往内地通行证, TWMID-台胞证, PASS-护照")
    private String nationalIdType;

    @Schema(description = "证件号", example = "15329")
    @ExcelProperty("证件号")
    private String nationalId;

    @Schema(description = "员工记录号")
    @ExcelProperty("员工记录号")
    private String emplRcd;

    @Schema(description = "生效日期")
    @ExcelProperty("生效日期")
    private LocalDate effdt;

    @Schema(description = "生效序号")
    @ExcelProperty("生效序号")
    private String effseq;

    @Schema(description = "人事操作ID")
    @ExcelProperty("人事操作ID")
    private String action;

    @Schema(description = "人事操作")
    @ExcelProperty("人事操作")
    private String actionDescr;

    @Schema(description = "操作原因ID", example = "不香")
    @ExcelProperty("操作原因ID")
    private String actionReason;

    @Schema(description = "操作原因")
    @ExcelProperty("操作原因")
    private String actionReasnDescr;

    @Schema(description = "名", example = "李四")
    @ExcelProperty("名")
    private String firstName;

    @Schema(description = "姓", example = "张三")
    @ExcelProperty("姓")
    private String lastName;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}