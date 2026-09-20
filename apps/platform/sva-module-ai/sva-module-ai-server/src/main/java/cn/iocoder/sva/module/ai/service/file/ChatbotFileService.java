package cn.iocoder.sva.module.ai.service.file;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.module.ai.controller.admin.file.vo.ChatbotFilePageReqVO;
import cn.iocoder.sva.module.ai.controller.admin.file.vo.ChatbotFileSaveReqVO;
import cn.iocoder.sva.module.ai.dal.dataobject.file.ChatbotFileDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 对话文件记录 Service 接口
 *
 * @author like
 */
public interface ChatbotFileService {

    /**
     * 创建对话文件记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createChatbotFile(@Valid ChatbotFileSaveReqVO createReqVO);

    /**
     * 更新对话文件记录
     *
     * @param updateReqVO 更新信息
     */
    void updateChatbotFile(@Valid ChatbotFileSaveReqVO updateReqVO);

    /**
     * 删除对话文件记录
     *
     * @param id 编号
     */
    void deleteChatbotFile(Long id);

    /**
    * 批量删除对话文件记录
    *
    * @param ids 编号
    */
    void deleteChatbotFileListByIds(List<Long> ids);

    /**
     * 获得对话文件记录
     *
     * @param id 编号
     * @return 对话文件记录
     */
    ChatbotFileDO getChatbotFile(Long id);

    /**
     * 获得对话文件记录分页
     *
     * @param pageReqVO 分页查询
     * @return 对话文件记录分页
     */
    PageResult<ChatbotFileDO> getChatbotFilePage(ChatbotFilePageReqVO pageReqVO);

}