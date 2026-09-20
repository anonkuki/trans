package cn.iocoder.sva.module.system.api.temp;

import cn.iocoder.sva.framework.common.pojo.CommonResult;
import cn.iocoder.sva.module.system.api.temp.dto.TempFileRespDTO;
import cn.iocoder.sva.module.system.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 模板文件 API 接口
 *
 * @author like
 */
@FeignClient(name = ApiConstants.NAME) // TODO 芋艿：fallbackFactory =
@Tag(name = "RPC 服务 - 模板文件")
public interface TempFileApi {

    String PREFIX = ApiConstants.PREFIX + "/temp-file";

    /**
     * 根据模板标识查询模板文件
     *
     * @param tempName 模板标识
     * @return 模板文件信息
     */
    @GetMapping(PREFIX + "/get-by-temp-name")
    @Operation(summary = "根据模板标识查询模板文件")
    CommonResult<TempFileRespDTO> getTempFileByTempName(@RequestParam("tempName") String tempName);

}
