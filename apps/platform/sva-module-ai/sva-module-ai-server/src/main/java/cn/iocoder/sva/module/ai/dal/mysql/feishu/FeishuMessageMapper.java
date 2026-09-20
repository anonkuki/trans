package cn.iocoder.sva.module.ai.dal.mysql.feishu;

import java.util.*;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.sva.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.sva.module.ai.dal.dataobject.feishu.FeishuMessageDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.sva.module.ai.controller.admin.feishu.vo.*;

/**
 * 飞书消息接收记录 Mapper
 *
 * @author like
 */
@Mapper
public interface FeishuMessageMapper extends BaseMapperX<FeishuMessageDO> {

    default PageResult<FeishuMessageDO> selectPage(FeishuMessagePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<FeishuMessageDO>()
                .eqIfPresent(FeishuMessageDO::getUserId, reqVO.getUserId())
                .eqIfPresent(FeishuMessageDO::getOpenId, reqVO.getOpenId())
                .eqIfPresent(FeishuMessageDO::getUnionId, reqVO.getUnionId())
                .eqIfPresent(FeishuMessageDO::getUserMessage, reqVO.getUserMessage())
                .eqIfPresent(FeishuMessageDO::getOriginalJson, reqVO.getOriginalJson())
                .eqIfPresent(FeishuMessageDO::getMessageType, reqVO.getMessageType())
                .eqIfPresent(FeishuMessageDO::getMessageId, reqVO.getMessageId())
                .eqIfPresent(FeishuMessageDO::getChatType, reqVO.getChatType())
                .eqIfPresent(FeishuMessageDO::getChatId, reqVO.getChatId())
                .eqIfPresent(FeishuMessageDO::getTenantKey, reqVO.getTenantKey())
                .eqIfPresent(FeishuMessageDO::getSenderType, reqVO.getSenderType())
                .eqIfPresent(FeishuMessageDO::getReplyContent, reqVO.getReplyContent())
                .eqIfPresent(FeishuMessageDO::getReplySuccess, reqVO.getReplySuccess())
                .betweenIfPresent(FeishuMessageDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(FeishuMessageDO::getId));
    }

}