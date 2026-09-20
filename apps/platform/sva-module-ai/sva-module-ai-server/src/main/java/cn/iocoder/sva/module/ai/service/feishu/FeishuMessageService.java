package cn.iocoder.sva.module.ai.service.feishu;

import java.util.*;

import cn.iocoder.sva.module.ai.dal.dataobject.feishu.FeishuMessageDTO;
import jakarta.validation.*;
import cn.iocoder.sva.module.ai.controller.admin.feishu.vo.*;
import cn.iocoder.sva.module.ai.dal.dataobject.feishu.FeishuMessageDO;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.pojo.PageParam;
/**
 * 飞书消息 Service 接口
 *
 * @author like
 */
public interface FeishuMessageService {

    // 处理飞书接收到的消息
    void handleMessage(FeishuMessageDO message);

    /**
     * 处理飞书文件消息
     *
     * @param messageDTO 文件消息 DTO
     */
    void handleFileMessage(FeishuMessageDTO messageDTO);

    /**
     * 处理飞书富文本消息（文字+图片）
     *
     * @param messageDTO 富文本消息 DTO
     */
    void handlePostMessage(FeishuMessageDTO messageDTO);

    /**
     * 发送飞书文本消息
     *
     * @param receiveId 接收者ID
     * @param receiveIdType 接收者ID类型
     * @param text 文本内容
     * @return 消息ID
     * @throws Exception 发送异常
     */
    String sendTextMessage(String receiveId, String receiveIdType, String text) throws Exception;


    /**
     * 回复飞书文本消息（默认非话题回复，不去重）
     *
     * @param messageId 待回复的消息ID
     * @param text 回复的文本内容
     * @return 新回复消息的ID
     * @throws Exception 回复异常
     */
    String replyTextMessage(String messageId, String text) throws Exception;

    /**
     * 回复飞书文本消息（支持自定义话题回复、去重UUID）
     *
     * @param messageId 待回复的消息ID
     * @param text 回复的文本内容
     * @param replyInThread 是否以话题形式回复
     * @param uuid 自定义去重UUID，1小时内相同UUID仅成功回复一次
     * @return 新回复消息的ID
     * @throws Exception 回复异常
     */
    String replyTextMessage(String messageId, String text, Boolean replyInThread, String uuid) throws Exception;

    /**
     * 流式回复飞书消息（使用卡片）
     *
     * @param messageId 待回复的消息ID
     * @param text 回复的文本内容
     * @return 新回复消息的ID
     * @throws Exception 回复异常
     */
    String replyStreamMessage(String messageId, String text) throws Exception;


    /**
     * 创建飞书消息接收记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createFeishuMessage(@Valid FeishuMessageSaveReqVO createReqVO);

    /**
     * 更新飞书消息接收记录
     *
     * @param updateReqVO 更新信息
     */
    void updateFeishuMessage(@Valid FeishuMessageSaveReqVO updateReqVO);

    /**
     * 删除飞书消息接收记录
     *
     * @param id 编号
     */
    void deleteFeishuMessage(Long id);

    /**
     * 批量删除飞书消息接收记录
     *
     * @param ids 编号
     */
    void deleteFeishuMessageListByIds(List<Long> ids);

    /**
     * 获得飞书消息接收记录
     *
     * @param id 编号
     * @return 飞书消息接收记录
     */
    FeishuMessageDO getFeishuMessage(Long id);

    /**
     * 获得飞书消息接收记录分页
     *
     * @param pageReqVO 分页查询
     * @return 飞书消息接收记录分页
     */
    PageResult<FeishuMessageDO> getFeishuMessagePage(FeishuMessagePageReqVO pageReqVO);
}