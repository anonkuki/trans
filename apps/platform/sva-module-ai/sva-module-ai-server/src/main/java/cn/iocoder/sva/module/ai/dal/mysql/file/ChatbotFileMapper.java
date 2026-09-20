package cn.iocoder.sva.module.ai.dal.mysql.file;

import java.util.*;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.sva.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.sva.module.ai.dal.dataobject.file.ChatbotFileDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.sva.module.ai.controller.admin.file.vo.*;

/**
 * 对话文件记录 Mapper
 *
 * @author like
 */
@Mapper
public interface ChatbotFileMapper extends BaseMapperX<ChatbotFileDO> {

    default PageResult<ChatbotFileDO> selectPage(ChatbotFilePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ChatbotFileDO>()
                .likeIfPresent(ChatbotFileDO::getFileName, reqVO.getFileName())
                .eqIfPresent(ChatbotFileDO::getFilePath, reqVO.getFilePath())
                .eqIfPresent(ChatbotFileDO::getUserQuestion, reqVO.getUserQuestion())
                .eqIfPresent(ChatbotFileDO::getFileContent, reqVO.getFileContent())
                .eqIfPresent(ChatbotFileDO::getFileType, reqVO.getFileType())
                .eqIfPresent(ChatbotFileDO::getUserJobNumber, reqVO.getUserJobNumber())
                .betweenIfPresent(ChatbotFileDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ChatbotFileDO::getId));
    }

}