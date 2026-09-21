package cn.iocoder.sva.module.system.dal.dataobject.extlink;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.sva.framework.mybatis.core.dataobject.BaseDO;

/**
 * 系统外链 DO
 *
 * @author like
 */
@TableName("system_external_link")
@KeySequence("system_external_link_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalLinkDO extends BaseDO {

    /**
     * 外链编号
     */
    @TableId
    private Long id;
    /**
     * 外链名称（卡片标题）
     */
    private String name;
    /**
     * 外链地址（跳转目标网站）
     */
    private String url;
    /**
     * 图标（卡片图标，如 ep:link）
     */
    private String icon;
    /**
     * 图标链接（上传的外链图标图片地址，优先于 {@link #icon} 展示）
     */
    private String iconUrl;
    /**
     * 外链描述（卡片副标题）
     */
    private String description;
    /**
     * 分类（首页卡片分组，如 办公系统）
     */
    private String category;
    /**
     * 显示顺序（首页卡片排序）
     */
    private Integer sort;
    /**
     * 状态（0开启 1关闭，CommonStatusEnum）
     */
    private Integer status;
    /**
     * 打开方式（0当前窗口 1新窗口）
     */
    private Integer openTarget;
    /**
     * 点击次数
     */
    private Long clickCount;
    /**
     * 关联的菜单编号（同步创建的顶层菜单，用于侧边栏展示；为空表示未添加到菜单）
     */
    private Long menuId;


}