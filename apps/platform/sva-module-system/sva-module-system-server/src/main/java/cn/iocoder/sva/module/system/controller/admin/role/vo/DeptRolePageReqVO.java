package cn.iocoder.sva.module.system.controller.admin.role.vo;

import cn.iocoder.sva.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.sva.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 部门和角色关联分页 Request VO")
@Data
public class DeptRolePageReqVO extends PageParam {

    @Schema(description = "部门ID", example = "20893")
    private Long deptId;

    @Schema(description = "角色ID", example = "32602")
    private Long roleId;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}