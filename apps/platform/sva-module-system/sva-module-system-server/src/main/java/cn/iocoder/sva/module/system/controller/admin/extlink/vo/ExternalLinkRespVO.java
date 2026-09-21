package cn.iocoder.sva.module.system.controller.admin.extlink.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 系统外链 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ExternalLinkRespVO {

    @Schema(description = "外链编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "12979")
    @ExcelProperty("外链编号")
    private Long id;

    @Schema(description = "外链名称（卡片标题）", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @ExcelProperty("外链名称（卡片标题）")
    private String name;

    @Schema(description = "外链地址（跳转目标网站）", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://www.iocoder.cn")
    @ExcelProperty("外链地址（跳转目标网站）")
    private String url;

    @Schema(description = "图标（卡片图标，如 ep:link）")
    @ExcelProperty("图标（卡片图标，如 ep:link）")
    private String icon;

    @Schema(description = "图标链接（上传的外链图标图片地址）")
    @ExcelProperty("图标链接")
    private String iconUrl;

    @Schema(description = "外链描述（卡片副标题）", example = "你猜")
    @ExcelProperty("外链描述（卡片副标题）")
    private String description;

    @Schema(description = "分类（首页卡片分组，如 办公系统）")
    @ExcelProperty("分类（首页卡片分组，如 办公系统）")
    private String category;

    @Schema(description = "显示顺序（首页卡片排序）", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("显示顺序（首页卡片排序）")
    private Integer sort;

    @Schema(description = "状态（0开启 1关闭，CommonStatusEnum）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("状态（0开启 1关闭，CommonStatusEnum）")
    private Integer status;

    @Schema(description = "打开方式（0当前窗口 1新窗口）", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("打开方式（0当前窗口 1新窗口）")
    private Integer openTarget;

    @Schema(description = "点击次数", requiredMode = Schema.RequiredMode.REQUIRED, example = "11747")
    @ExcelProperty("点击次数")
    private Long clickCount;

    @Schema(description = "关联的菜单编号", example = "2048")
    private Long menuId;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}