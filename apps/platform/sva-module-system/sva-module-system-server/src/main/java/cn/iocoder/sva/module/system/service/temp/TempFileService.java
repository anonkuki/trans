package cn.iocoder.sva.module.system.service.temp;

import java.util.*;
import jakarta.validation.*;
import cn.iocoder.sva.module.system.controller.admin.temp.vo.*;
import cn.iocoder.sva.module.system.dal.dataobject.temp.TempFileDO;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.pojo.PageParam;

/**
 * 模板管理 Service 接口
 *
 * @author like
 */
public interface TempFileService {

    /**
     * 创建模板管理
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createTempFile(@Valid TempFileSaveReqVO createReqVO);

    /**
     * 更新模板管理
     *
     * @param updateReqVO 更新信息
     */
    void updateTempFile(@Valid TempFileSaveReqVO updateReqVO);

    /**
     * 删除模板管理
     *
     * @param id 编号
     */
    void deleteTempFile(Long id);

    /**
    * 批量删除模板管理
    *
    * @param ids 编号
    */
    void deleteTempFileListByIds(List<Long> ids);

    /**
     * 获得模板管理
     *
     * @param id 编号
     * @return 模板管理
     */
    TempFileDO getTempFile(Long id);

    /**
     * 根据模板标识获得模板管理
     *
     * @param tempName 模板标识
     * @return 模板管理
     */
    TempFileDO getTempFileByTempName(String tempName);

    /**
     * 获得模板管理分页
     *
     * @param pageReqVO 分页查询
     * @return 模板管理分页
     */
    PageResult<TempFileDO> getTempFilePage(TempFilePageReqVO pageReqVO);

    /**
     * 上传文件到 MinIO 并保存到数据库
     *
     * @param file 上传的文件
     * @param tempName 模板标识
     * @return 文件信息（包含ID、URL等）
     */
    Map<String, Object> uploadFile(org.springframework.web.multipart.MultipartFile file, String tempName) throws Exception;

}