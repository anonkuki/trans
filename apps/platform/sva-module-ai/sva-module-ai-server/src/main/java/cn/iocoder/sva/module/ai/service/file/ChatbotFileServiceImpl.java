package cn.iocoder.sva.module.ai.service.file;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import cn.iocoder.sva.module.ai.controller.admin.file.vo.*;
import cn.iocoder.sva.module.ai.dal.dataobject.file.ChatbotFileDO;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.pojo.PageParam;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;

import cn.iocoder.sva.module.ai.dal.mysql.file.ChatbotFileMapper;

import static cn.iocoder.sva.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.sva.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.sva.framework.common.util.collection.CollectionUtils.diffList;
import static cn.iocoder.sva.module.ai.enums.ErrorCodeConstants.*;

/**
 * 对话文件记录 Service 实现类
 *
 * @author like
 */
@Service
@Validated
public class ChatbotFileServiceImpl implements ChatbotFileService {

    @Resource
    private ChatbotFileMapper chatbotFileMapper;

    @Override
    public Long createChatbotFile(ChatbotFileSaveReqVO createReqVO) {
        // 插入
        ChatbotFileDO chatbotFile = BeanUtils.toBean(createReqVO, ChatbotFileDO.class);
        chatbotFileMapper.insert(chatbotFile);

        // 返回
        return chatbotFile.getId();
    }

    @Override
    public void updateChatbotFile(ChatbotFileSaveReqVO updateReqVO) {
        // 校验存在
        validateChatbotFileExists(updateReqVO.getId());
        // 更新
        ChatbotFileDO updateObj = BeanUtils.toBean(updateReqVO, ChatbotFileDO.class);
        chatbotFileMapper.updateById(updateObj);
    }

    @Override
    public void deleteChatbotFile(Long id) {
        // 校验存在
        validateChatbotFileExists(id);
        // 删除
        chatbotFileMapper.deleteById(id);
    }

    @Override
        public void deleteChatbotFileListByIds(List<Long> ids) {
        // 删除
        chatbotFileMapper.deleteByIds(ids);
        }


    private void validateChatbotFileExists(Long id) {
        if (chatbotFileMapper.selectById(id) == null) {
            throw exception(CHATBOT_FILE_NOT_EXISTS);
        }
    }

    @Override
    public ChatbotFileDO getChatbotFile(Long id) {
        return chatbotFileMapper.selectById(id);
    }

    @Override
    public PageResult<ChatbotFileDO> getChatbotFilePage(ChatbotFilePageReqVO pageReqVO) {
        return chatbotFileMapper.selectPage(pageReqVO);
    }

}