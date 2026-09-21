package cn.iocoder.sva.framework.excel.core.util;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.converters.longconverter.LongStringConverter;
import cn.iocoder.sva.framework.common.exception.ServiceException;
import cn.iocoder.sva.framework.common.util.http.HttpUtils;
import cn.iocoder.sva.framework.excel.core.handler.ColumnWidthMatchStyleStrategy;
import cn.iocoder.sva.framework.excel.core.handler.SelectSheetWriteHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static cn.iocoder.sva.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;

/**
 * Excel 工具类
 *
 * @author 科兴源码
 */
@Slf4j
public class ExcelUtils {

    /**
     * 将列表以 Excel 响应给前端
     *
     * @param response  响应
     * @param filename  文件名
     * @param sheetName Excel sheet 名
     * @param head      Excel head 头
     * @param data      数据列表哦
     * @param <T>       泛型，保证 head 和 data 类型的一致性
     * @throws IOException 写入失败的情况
     */
    public static <T> void write(HttpServletResponse response, String filename, String sheetName,
                                 Class<T> head, List<T> data) throws IOException {
        // 输出 Excel
        FastExcelFactory.write(response.getOutputStream(), head)
                .autoCloseStream(false) // 不要自动关闭，交给 Servlet 自己处理
                .registerWriteHandler(new ColumnWidthMatchStyleStrategy()) // 基于 column 长度，自动适配。最大 255 宽度
                .registerWriteHandler(new SelectSheetWriteHandler(head)) // 基于固定 sheet 实现下拉框
                .registerConverter(new LongStringConverter()) // 避免 Long 类型丢失精度
                .sheet(sheetName).doWrite(data);
        // 设置 header 和 contentType。写在最后的原因是，避免报错时，响应 contentType 已经被修改了
        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(filename));
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
    }

    public static <T> List<T> read(MultipartFile file, Class<T> head) throws IOException {
        // 参考 https://t.zsxq.com/zM77F 帖子，增加 try 处理，兼容 windows 场景
        try (InputStream rawInputStream = file.getInputStream()) {
            // 使用BufferedInputStream包装，支持mark/reset操作
            BufferedInputStream inputStream = new BufferedInputStream(rawInputStream);

            // 获取文件扩展名，判断是否为CSV文件
            String fileName = file.getOriginalFilename();
            String contentType = file.getContentType();
            long fileSize = file.getSize();
            boolean isCsv = fileName != null && fileName.toLowerCase().endsWith(".csv");

            log.info("========== 开始读取文件 ==========");
            log.info("文件名: {}", fileName);
            log.info("Content-Type: {}", contentType);
            log.info("文件大小: {} bytes ({} KB)", fileSize, fileSize / 1024.0);
            log.info("文件扩展名判断为CSV: {}", isCsv);

            // 标记当前位置，以便后续重置
            inputStream.mark(8);

            // 读取文件的前几个字节，用于判断实际文件类型
            byte[] header = new byte[8];
            int bytesRead = inputStream.read(header);
            log.info("文件头字节数: {}", bytesRead);
            if (bytesRead > 0) {
                StringBuilder hexStr = new StringBuilder();
                for (int i = 0; i < bytesRead; i++) {
                    hexStr.append(String.format("%02X ", header[i]));
                }
                log.info("文件头十六进制: {}", hexStr.toString().trim());

                // Excel文件的魔术数字：D0 CF 11 E0 (旧版) 或 50 4B 03 04 (新版xlsx)
                boolean isExcelOld = bytesRead >= 4 && header[0] == (byte)0xD0 && header[1] == (byte)0xCF && header[2] == (byte)0x11 && header[3] == (byte)0xE0;
                boolean isExcelNew = bytesRead >= 4 && header[0] == (byte)0x50 && header[1] == (byte)0x4B && header[2] == (byte)0x03 && header[3] == (byte)0x04;
                log.info("文件类型检测 - 旧版Excel(.xls): {}, 新版Excel(.xlsx): {}", isExcelOld, isExcelNew);

                // 检测加密文件：如果文件头既不是Excel也不是CSV格式，可能是加密文件
                if (!isCsv && !isExcelOld && !isExcelNew) {
                    log.warn("警告：文件扩展名不是.csv但文件头也不是Excel格式，可能是加密文件或损坏的文件");
                    log.warn("文件头: {}", hexStr.toString().trim());
                    throw new ServiceException(BAD_REQUEST.getCode(), "无法解析该文件，请检查文件是否加密或损坏");
                }

                // 如果文件头显示是Excel但扩展名是CSV，或者反之，给出警告
                if (isCsv && (isExcelOld || isExcelNew)) {
                    log.warn("警告：文件扩展名是.csv但文件头显示是Excel格式！");
                }
            }

            // 重置输入流到标记位置
            inputStream.reset();

            // 不再区分CSV和Excel，统一使用默认配置，让FastExcel自己判断
            log.info("使用FastExcel默认解析配置（自动识别文件类型）");
            return FastExcelFactory.read(inputStream, head, null)
                    .autoCloseStream(false) // 不要自动关闭，交给 Servlet 自己处理
                    .doReadAllSync();
        } catch (Exception e) {
            // 如果是ServiceException（加密文件错误），直接抛出
            if (e instanceof ServiceException) {
                throw (ServiceException) e;
            }

            // 记录详细的错误信息，包括文件名、大小等（使用INFO级别以便测试环境可见）
            log.info("========== 读取文件失败 ==========");
            log.info("文件名: {}", file.getOriginalFilename());
            log.info("文件大小: {} bytes", file.getSize());
            log.info("Content-Type: {}", file.getContentType());
            log.info("错误类型: {}", e.getClass().getName());
            log.info("错误消息: {}", e.getMessage());
            log.info("详细堆栈信息: ", e);
            throw new ServiceException(BAD_REQUEST.getCode(), "读取Excel文件失败: " + file.getOriginalFilename() + ", 错误: " + e.getMessage());
        }
    }

}
