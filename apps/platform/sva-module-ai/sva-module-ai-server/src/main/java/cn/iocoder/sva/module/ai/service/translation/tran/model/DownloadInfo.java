package cn.iocoder.sva.module.ai.service.translation.tran.model;

import lombok.Data;

/**
 * 下载文件信息
 * <p>
 * 翻译任务完成后，生成的文件（如翻译后的文档、对比文档等）的信息，
 * 用于前端下载链接的生成。
 */
@Data
public class DownloadInfo {

    /** 服务器上的文件路径（相对路径），用于定位下载资源 */
    private String path;

    /** 下载时显示的文件名，供用户识别 */
    private String filename;

    /** MinIO 文件访问 URL */
    private String minioUrl;

    public DownloadInfo() {
    }

    public DownloadInfo(String path, String filename) {
        this.path = path;
        this.filename = filename;
    }

    public DownloadInfo(String path, String filename, String minioUrl) {
        this.path = path;
        this.filename = filename;
        this.minioUrl = minioUrl;
    }
}