package com.xxl.job.admin.model;

import java.util.Date;

/**
 * 系统用户实体类
 *
 * @author system
 * @date 2026-04-08
 */
public class SystemUser {

    // ========== 核心认证字段 ==========
    private Long id;                // 用户ID
    private String username;        // 用户账号
    private String password;        // 密码
    private String nickname;        // 用户昵称

    // ========== 基本信息字段 ==========
    private String remark;          // 备注
    private Long deptId;            // 部门ID
    private String postIds;         // 岗位编号数组
    private String email;           // 用户邮箱
    private String mobile;          // 手机号码
    private Integer sex;            // 用户性别（0男 1女 2未知）
    private String avatar;          // 头像地址
    private Integer status;         // 帐号状态（0正常 1停用）

    // ========== 登录信息字段 ==========
    private String loginIp;         // 最后登录IP
    private Date loginDate;         // 最后登录时间

    // ========== 审计字段 ==========
    private String creator;         // 创建者
    private Date createTime;        // 创建时间
    private String updater;         // 更新者
    private Date updateTime;        // 更新时间
    private Boolean deleted;        // 是否删除
    private Long tenantId;          // 租户编号

    // ========== HR扩展字段 ==========
    private String guid;            // 唯一ID
    private Date effdt;             // 生效日期
    private String emplClass;       // 员工类型ID
    private String dcEmplClsDescr;  // 员工类型
    private String hrStatus;        // HR状态
    private String regTemp;         // 正式/临时
    private String reportsTo;       // 直接上级岗位ID
    private String jobIndicator;    // 主/兼岗
    private String jobIndicatorDescr; // 主/兼岗描述
    private String positionNbr;     // 岗位ID
    private String dcPositionDescr; // 岗位
    private String dcDeptDescr50;   // 部门
    private String managerPosn;     // 部门负责人岗位ID
    private String dcDirectorPosn;  // 部门总监岗位ID
    private Date probationDt;       // 转正日期
    private String dcJobLevel;      // 职级
    private String dcJobLevelDescr; // 职级描述
    private String dcJobGrade;      // 职等
    private String dcJobGradeDescr; // 职等描述
    private String dcJobStage;      // 职层
    private String dcJobStageDescr; // 职层描述
    private Date lastHireDt;        // 入职时间
    private String company;         // 公司ID
    private String dcCompanyDescr;  // 公司
    private String businessUnit;    // 业务单位ID
    private String businessDescr;   // 业务单位

    // ========== Getters and Setters ==========

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getPostIds() {
        return postIds;
    }

    public void setPostIds(String postIds) {
        this.postIds = postIds;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public Integer getSex() {
        return sex;
    }

    public void setSex(Integer sex) {
        this.sex = sex;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getLoginIp() {
        return loginIp;
    }

    public void setLoginIp(String loginIp) {
        this.loginIp = loginIp;
    }

    public Date getLoginDate() {
        return loginDate;
    }

    public void setLoginDate(Date loginDate) {
        this.loginDate = loginDate;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdater() {
        return updater;
    }

    public void setUpdater(String updater) {
        this.updater = updater;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public Date getEffdt() {
        return effdt;
    }

    public void setEffdt(Date effdt) {
        this.effdt = effdt;
    }

    public String getEmplClass() {
        return emplClass;
    }

    public void setEmplClass(String emplClass) {
        this.emplClass = emplClass;
    }

    public String getDcEmplClsDescr() {
        return dcEmplClsDescr;
    }

    public void setDcEmplClsDescr(String dcEmplClsDescr) {
        this.dcEmplClsDescr = dcEmplClsDescr;
    }

    public String getHrStatus() {
        return hrStatus;
    }

    public void setHrStatus(String hrStatus) {
        this.hrStatus = hrStatus;
    }

    public String getRegTemp() {
        return regTemp;
    }

    public void setRegTemp(String regTemp) {
        this.regTemp = regTemp;
    }

    public String getReportsTo() {
        return reportsTo;
    }

    public void setReportsTo(String reportsTo) {
        this.reportsTo = reportsTo;
    }

    public String getJobIndicator() {
        return jobIndicator;
    }

    public void setJobIndicator(String jobIndicator) {
        this.jobIndicator = jobIndicator;
    }

    public String getJobIndicatorDescr() {
        return jobIndicatorDescr;
    }

    public void setJobIndicatorDescr(String jobIndicatorDescr) {
        this.jobIndicatorDescr = jobIndicatorDescr;
    }

    public String getPositionNbr() {
        return positionNbr;
    }

    public void setPositionNbr(String positionNbr) {
        this.positionNbr = positionNbr;
    }

    public String getDcPositionDescr() {
        return dcPositionDescr;
    }

    public void setDcPositionDescr(String dcPositionDescr) {
        this.dcPositionDescr = dcPositionDescr;
    }

    public String getDcDeptDescr50() {
        return dcDeptDescr50;
    }

    public void setDcDeptDescr50(String dcDeptDescr50) {
        this.dcDeptDescr50 = dcDeptDescr50;
    }

    public String getManagerPosn() {
        return managerPosn;
    }

    public void setManagerPosn(String managerPosn) {
        this.managerPosn = managerPosn;
    }

    public String getDcDirectorPosn() {
        return dcDirectorPosn;
    }

    public void setDcDirectorPosn(String dcDirectorPosn) {
        this.dcDirectorPosn = dcDirectorPosn;
    }

    public Date getProbationDt() {
        return probationDt;
    }

    public void setProbationDt(Date probationDt) {
        this.probationDt = probationDt;
    }

    public String getDcJobLevel() {
        return dcJobLevel;
    }

    public void setDcJobLevel(String dcJobLevel) {
        this.dcJobLevel = dcJobLevel;
    }

    public String getDcJobLevelDescr() {
        return dcJobLevelDescr;
    }

    public void setDcJobLevelDescr(String dcJobLevelDescr) {
        this.dcJobLevelDescr = dcJobLevelDescr;
    }

    public String getDcJobGrade() {
        return dcJobGrade;
    }

    public void setDcJobGrade(String dcJobGrade) {
        this.dcJobGrade = dcJobGrade;
    }

    public String getDcJobGradeDescr() {
        return dcJobGradeDescr;
    }

    public void setDcJobGradeDescr(String dcJobGradeDescr) {
        this.dcJobGradeDescr = dcJobGradeDescr;
    }

    public String getDcJobStage() {
        return dcJobStage;
    }

    public void setDcJobStage(String dcJobStage) {
        this.dcJobStage = dcJobStage;
    }

    public String getDcJobStageDescr() {
        return dcJobStageDescr;
    }

    public void setDcJobStageDescr(String dcJobStageDescr) {
        this.dcJobStageDescr = dcJobStageDescr;
    }

    public Date getLastHireDt() {
        return lastHireDt;
    }

    public void setLastHireDt(Date lastHireDt) {
        this.lastHireDt = lastHireDt;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getDcCompanyDescr() {
        return dcCompanyDescr;
    }

    public void setDcCompanyDescr(String dcCompanyDescr) {
        this.dcCompanyDescr = dcCompanyDescr;
    }

    public String getBusinessUnit() {
        return businessUnit;
    }

    public void setBusinessUnit(String businessUnit) {
        this.businessUnit = businessUnit;
    }

    public String getBusinessDescr() {
        return businessDescr;
    }

    public void setBusinessDescr(String businessDescr) {
        this.businessDescr = businessDescr;
    }
}