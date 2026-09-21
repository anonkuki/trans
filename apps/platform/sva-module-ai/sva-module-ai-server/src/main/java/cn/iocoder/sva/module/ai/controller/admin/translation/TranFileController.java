package cn.iocoder.sva.module.ai.controller.admin.translation;

import cn.iocoder.sva.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.sva.framework.common.pojo.CommonResult;
import cn.iocoder.sva.framework.common.pojo.PageParam;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;
import cn.iocoder.sva.framework.excel.core.util.ExcelUtils;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranFilePageReqVO;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranFileRespVO;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranFileSaveReqVO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranFileDO;
import cn.iocoder.sva.module.ai.service.file.FileService;
import cn.iocoder.sva.module.ai.service.translation.TranFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static cn.iocoder.sva.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.sva.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AI翻译文件信息")
@Slf4j
@RestController
@RequestMapping("/ai/tran-file")
@Validated
public class TranFileController {

    @Resource
    private TranFileService tranFileService;

    @Resource
    private FileService fileService;

    @PostMapping("/create")
    @Operation(summary = "创建AI翻译文件信息")
    @PreAuthorize("@ss.hasPermission('ai:tran-file:create')")
    public CommonResult<Long> createTranFile(@Valid @RequestBody TranFileSaveReqVO createReqVO) {
        return success(tranFileService.createTranFile(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新AI翻译文件信息")
    @PreAuthorize("@ss.hasPermission('ai:tran-file:update')")
    public CommonResult<Boolean> updateTranFile(@Valid @RequestBody TranFileSaveReqVO updateReqVO) {
        tranFileService.updateTranFile(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除AI翻译文件信息")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('ai:tran-file:delete')")
    public CommonResult<Boolean> deleteTranFile(@RequestParam("id") Long id) throws Exception {
        tranFileService.deleteTranFileWithMinioFiles(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除AI翻译文件信息")
    @PreAuthorize("@ss.hasPermission('ai:tran-file:delete')")
    public CommonResult<Boolean> deleteTranFileList(@RequestParam("ids") List<Long> ids) throws Exception {
        tranFileService.deleteTranFileListWithMinioFiles(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得AI翻译文件信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('ai:tran-file:query')")
    public CommonResult<TranFileRespVO> getTranFile(@RequestParam("id") Long id) {
        TranFileDO tranFile = tranFileService.getTranFileForCurrentUser(id);
        return success(BeanUtils.toBean(tranFile, TranFileRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得AI翻译文件信息分页")
    @PreAuthorize("@ss.hasPermission('ai:tran-file:query')")
    public CommonResult<PageResult<TranFileRespVO>> getTranFilePage(@Valid TranFilePageReqVO pageReqVO) {
        PageResult<TranFileDO> pageResult = tranFileService.getTranFilePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TranFileRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出AI翻译文件信息 Excel")
    @PreAuthorize("@ss.hasPermission('ai:tran-file:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportTranFileExcel(@Valid TranFilePageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TranFileDO> list = tranFileService.getTranFilePage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "AI翻译文件信息.xls", "数据", TranFileRespVO.class,
                        BeanUtils.toBean(list, TranFileRespVO.class));
    }

    /**
     * 下载翻译文件（通过后端从 MinIO 读取，避免前端直接请求 MinIO 导致文件名特殊字符编码问题）
     *
     * @param id 文件记录ID
     * @param type 文件类型：file(译文，默认), source(原文件), compare(对照表), qc(QC报告), contrast(双语版)
     * @return 文件流
     */
    @GetMapping("/download/{id}")
    @Operation(summary = "下载翻译文件")
    @PreAuthorize("@ss.hasPermission('ai:tran-file:query')")
    public ResponseEntity<org.springframework.core.io.Resource> downloadFile(
            @Parameter(description = "文件记录ID") @PathVariable Long id,
            @Parameter(description = "文件类型(file/source/compare/qc/contrast)") @RequestParam(defaultValue = "file") String type) {
        // 查询文件记录
        TranFileDO fileDO = tranFileService.getTranFileForCurrentUser(id);
        if (fileDO == null) {
            log.warn("[downloadFile] 文件记录不存在, id={}", id);
            return ResponseEntity.notFound().build();
        }

        // 根据文件类型获取对应的 MinIO URL
        String fileUrl = getFileUrlByType(fileDO, type);
        if (fileUrl == null || fileUrl.isEmpty()) {
            log.error("[downloadFile] 文件类型 {} 的 URL 为空, id={}", type, id);
            return ResponseEntity.notFound().build();
        }

        // 根据文件类型生成下载文件名
        String fileName = getDownloadFileName(fileDO, type);

        try {
            // 从 MinIO 读取文件内容
            log.info("[downloadFile] 开始从 MinIO 读取文件, id={}, type={}, url={}", id, type, fileUrl);
            byte[] fileContent = fileService.getFileContentByUrl(fileUrl);
            log.info("[downloadFile] 文件读取成功, 大小={} bytes, 文件名={}", fileContent.length, fileName);

            // 编码文件名
            String encodedFilename = java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename)
                    .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileContent.length))
                    .body(new org.springframework.core.io.ByteArrayResource(fileContent));
        } catch (Exception e) {
            log.error("[downloadFile] 下载文件失败, id={}, type={}, url={}", id, type, fileUrl, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 根据文件类型获取对应的 MinIO URL
     */
    private String getFileUrlByType(TranFileDO fileDO, String type) {
        return switch (type) {
            case "source" -> fileDO.getSourceFileUrl();
            case "compare" -> fileDO.getCompareFileUrl();
            case "qc" -> fileDO.getQcFileUrl();
            case "contrast" -> fileDO.getContrastFileUrl();
            default -> fileDO.getFileUrl(); // file
        };
    }

    /**
     * 根据文件类型生成下载文件名
     */
    private String getDownloadFileName(TranFileDO fileDO, String type) {
        // 清理文件名：去除_strict和时间戳
        String cleanName = fileDO.getFileName() != null ? fileDO.getFileName() : "未命名文件";
        cleanName = cleanName.replace("_strict", "").replaceAll("_\\d{10,}", "");
        String ext = cleanName.contains(".") ? cleanName.substring(cleanName.lastIndexOf(".")) : "";
        String nameWithoutExt = cleanName.contains(".") ? cleanName.substring(0, cleanName.lastIndexOf(".")) : cleanName;

        // 译文和双语版的输出扩展名：xlsx/xlsm 翻译后仍为 xlsx，pdf 翻译后转为 docx，docx 保持不变
        String outputExt = ext;
        if (".pdf".equalsIgnoreCase(ext)) {
            outputExt = ".docx"; // PDF 翻译后转换为 DOCX
        }

        return switch (type) {
            case "source" -> nameWithoutExt + "_原文档" + ext; // 原文件保持原始格式
            case "compare" -> nameWithoutExt + "_对照表.xlsx"; // 对照表固定为 xlsx 格式
            case "qc" -> nameWithoutExt + "_QC报告.txt"; // QC报告固定为 txt 格式
            case "contrast" -> nameWithoutExt + "_双语版" + outputExt; // 双语版：xlsx→xlsx, docx→docx, pdf→docx
            default -> nameWithoutExt + "_译文" + outputExt; // 译文：xlsx→xlsx, docx→docx, pdf→docx
        };
    }

}
