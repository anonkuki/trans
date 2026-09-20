package cn.iocoder.sva.module.ai.controller.admin.translation.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 术语导入 VO
 *
 * @author like
 */
@Data
public class GlossaryItemImportVO {

    @ExcelProperty("source")
    private String source;

    @ExcelProperty("target")
    private String target;
}