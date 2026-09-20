package cn.iocoder.sva.module.system.api.temp;

import cn.iocoder.sva.framework.common.pojo.CommonResult;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;
import cn.iocoder.sva.module.system.api.temp.dto.TempFileRespDTO;
import cn.iocoder.sva.module.system.dal.dataobject.temp.TempFileDO;
import cn.iocoder.sva.module.system.service.temp.TempFileService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.sva.framework.common.pojo.CommonResult.success;

/**
 * 模板文件 API 实现类
 *
 * @author like
 */
@RestController // 提供 RESTful API 接口，给 Feign 调用
@Validated
public class TempFileApiImpl implements TempFileApi {

    @Resource
    private TempFileService tempFileService;

    @Override
    public CommonResult<TempFileRespDTO> getTempFileByTempName(String tempName) {
        TempFileDO tempFile = tempFileService.getTempFileByTempName(tempName);
        return success(BeanUtils.toBean(tempFile, TempFileRespDTO.class));
    }

}
