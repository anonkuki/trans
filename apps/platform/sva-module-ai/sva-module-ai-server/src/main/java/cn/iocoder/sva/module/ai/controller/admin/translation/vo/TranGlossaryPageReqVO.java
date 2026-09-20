package cn.iocoder.sva.module.ai.controller.admin.translation.vo;

import cn.iocoder.sva.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.sva.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 术语库管理分页 Request VO")
@Data
public class TranGlossaryPageReqVO extends PageParam {

    @Schema(description = "术语库名称", example = "张三")
    private String glossaryName;

    @Schema(description = "源语言")
    private String sourceLanguage;

    @Schema(description = "目标语言")
    private String targetLanguage;

    @Schema(description = "语言方向")
    private String languageDirection;

    @Schema(description = "条目数量", example = "23091")
    private Integer itemCount;

    @Schema(description = "所属角色", example = "2273")
    private Long roleId;

    @Schema(description = "所属人员", example = "")
    private String username;

    @Schema(description = "所属部门", example = "13831")
    private Long deptId;

    @Schema(description = "是否启用 1=启用 0=禁用")
    private Integer isEnabled;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}