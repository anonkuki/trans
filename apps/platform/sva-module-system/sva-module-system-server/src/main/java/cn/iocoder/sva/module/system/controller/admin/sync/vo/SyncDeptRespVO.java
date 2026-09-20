package cn.iocoder.sva.module.system.controller.admin.sync.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 部门信息同步 Response VO")
@Data
@ExcelIgnoreUnannotated
public class SyncDeptRespVO {

    @Schema(description = "自增主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "17941")
    @ExcelProperty("自增主键")
    private Long id;

    @Schema(description = "唯一ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "6171")
    @ExcelProperty("唯一ID")
    private String guid;

    @Schema(description = "数据状态：A-新增，U-更新，D-删除", example = "1")
    @ExcelProperty("数据状态：A-新增，U-更新，D-删除")
    private String dcInfDtStatus;

    @Schema(description = "集合ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "99")
    @ExcelProperty("集合ID")
    private String setid;

    @Schema(description = "部门ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "22710")
    @ExcelProperty("部门ID")
    private String deptid;

    @Schema(description = "生效日期")
    @ExcelProperty("生效日期")
    private LocalDate effdt;

    @Schema(description = "生效状态：A-有效，I-无效", example = "1")
    @ExcelProperty("生效状态：A-有效，I-无效")
    private String effStatus;

    @Schema(description = "生效状态描述")
    @ExcelProperty("生效状态描述")
    private String effStatusDescr;

    @Schema(description = "部门名称")
    @ExcelProperty("部门名称")
    private String descr;

    @Schema(description = "部门简称")
    @ExcelProperty("部门简称")
    private String descrshort;

    @Schema(description = "地点集合ID")
    @ExcelProperty("地点集合ID")
    private String setidLocation;

    @Schema(description = "地点ID")
    @ExcelProperty("地点ID")
    private String location;

    @Schema(description = "地点")
    @ExcelProperty("地点")
    private String dcLocationDescr;

    @Schema(description = "所属公司ID")
    @ExcelProperty("所属公司ID")
    private String company;

    @Schema(description = "所属公司名称")
    @ExcelProperty("所属公司名称")
    private String dcCompanyDescr;

    @Schema(description = "组织类型：10-公司,20-体系，30-部门，40-项目，50-班组", example = "2")
    @ExcelProperty("组织类型：10-公司,20-体系，30-部门，40-项目，50-班组")
    private String dcOrgType;

    @Schema(description = "组织类型描述")
    @ExcelProperty("组织类型描述")
    private String dcOrgTypeDescr;

    @Schema(description = "组织类别：10-一级组织，20-二级组织，30-三级组织")
    @ExcelProperty("组织类别：10-一级组织，20-二级组织，30-三级组织")
    private String dcOrgLevel;

    @Schema(description = "组织类别描述")
    @ExcelProperty("组织类别描述")
    private String dcOrgLevelDescr;

    @Schema(description = "上级部门ID")
    @ExcelProperty("上级部门ID")
    private String partDeptidChn;

    @Schema(description = "上级部门")
    @ExcelProperty("上级部门")
    private String dcParDeptDescr;

    @Schema(description = "组织负责人岗位ID")
    @ExcelProperty("组织负责人岗位ID")
    private String managerPosn;

    @Schema(description = "部门分管总监岗位ID")
    @ExcelProperty("部门分管总监岗位ID")
    private String dcDirectorPosn;

    @Schema(description = "部门分管副总经理岗位ID")
    @ExcelProperty("部门分管副总经理岗位ID")
    private String dcManagerPosn;

    @Schema(description = "成立日期")
    @ExcelProperty("成立日期")
    private LocalDate dcSetupDate;

    @Schema(description = "成立文号")
    @ExcelProperty("成立文号")
    private String dcSetupNum;

    @Schema(description = "成本中心ID")
    @ExcelProperty("成本中心ID")
    private String dcCostCenter;

    @Schema(description = "体系ID")
    @ExcelProperty("体系ID")
    private String dcOrgBranch;

    @Schema(description = "体系")
    @ExcelProperty("体系")
    private String dcOrgBranchDescr;

    @Schema(description = "成立原因", example = "不对")
    @ExcelProperty("成立原因")
    private String dcSetupReason;

    @Schema(description = "部门职责")
    @ExcelProperty("部门职责")
    private String dcDeptRespon;

    @Schema(description = "部门全路径")
    @ExcelProperty("部门全路径")
    private String dcDeptFullCode;

    @Schema(description = "部门全路径名称")
    @ExcelProperty("部门全路径名称")
    private String dcDeptFullDescr;

    @Schema(description = "红海转换ID", example = "23680")
    @ExcelProperty("红海转换ID")
    private String dcHonghaiDeptid;

    @Schema(description = "SAP公司代码", example = "19443")
    @ExcelProperty("SAP公司代码")
    private String dcSapCompanyid;

    @Schema(description = "一级部门")
    @ExcelProperty("一级部门")
    private String dcDeptidLv01;

    @Schema(description = "二级部门")
    @ExcelProperty("二级部门")
    private String dcDeptidLv02;

    @Schema(description = "三级部门")
    @ExcelProperty("三级部门")
    private String dcDeptidLv03;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}