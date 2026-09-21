package cn.iocoder.sva.module.system.controller.admin.extlink.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 系统外链新增/修改 Request VO")
@Data
public class ExternalLinkSaveReqVO {

    @Schema(description = "外链编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "12979")
    private Long id;

    @Schema(description = "外链名称（卡片标题）", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @NotEmpty(message = "外链名称（卡片标题）不能为空")
    private String name;

    @Schema(description = "外链地址（跳转目标网站）", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://www.iocoder.cn")
    @NotEmpty(message = "外链地址（跳转目标网站）不能为空")
    private String url;

    @Schema(description = "图标（卡片图标，如 ep:link）")
    private String icon;

    @Schema(description = "图标链接（上传的外链图标图片地址）")
    private String iconUrl;

    @Schema(description = "外链描述（卡片副标题）", example = "你猜")
    private String description;

    @Schema(description = "分类（首页卡片分组，如 办公系统）")
    private String category;

    @Schema(description = "显示顺序（首页卡片排序）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "显示顺序（首页卡片排序）不能为空")
    private Integer sort;

    @Schema(description = "状态（0开启 1关闭，CommonStatusEnum）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态（0开启 1关闭，CommonStatusEnum）不能为空")
    private Integer status;

    @Schema(description = "打开方式（0当前窗口 1新窗口）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "打开方式（0当前窗口 1新窗口）不能为空")
    private Integer openTarget;

    @Schema(description = "点击次数", example = "11747")
    private Long clickCount;

    @Schema(description = "是否同步添加到菜单", example = "true")
    private Boolean addMenuFlag;

}