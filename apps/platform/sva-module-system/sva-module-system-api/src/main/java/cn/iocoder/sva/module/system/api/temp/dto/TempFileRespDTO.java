package cn.iocoder.sva.module.system.api.temp.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 模板文件 Response DTO
 *
 * @author like
 */
@Data
public class TempFileRespDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文件编号
     */
    private Long id;

    /**
     * 模板标识
     */
    private String tempName;

    /**
     * 文件名
     */
    private String name;

    /**
     * 文件 URL
     */
    private String url;

    /**
     * 文件类型
     */
    private String type;

    /**
     * 文件大小
     */
    private Integer size;

}
