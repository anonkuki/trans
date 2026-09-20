package cn.iocoder.sva.module.ai.service.feishu;

import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;
import cn.iocoder.sva.module.ai.config.OauthPropertiesConfig;
import cn.iocoder.sva.module.ai.controller.admin.feishu.vo.FeishuMessagePageReqVO;
import cn.iocoder.sva.module.ai.controller.admin.feishu.vo.FeishuMessageSaveReqVO;
import cn.iocoder.sva.module.ai.controller.admin.file.vo.ChatbotFileSaveReqVO;
import cn.iocoder.sva.module.ai.dal.dataobject.feishu.FeishuMessageDO;
import cn.iocoder.sva.module.ai.dal.dataobject.feishu.FeishuMessageDTO;
import cn.iocoder.sva.module.ai.dal.dataobject.file.ChatbotFileDO;
import cn.iocoder.sva.module.ai.dal.mysql.feishu.FeishuMessageMapper;
import cn.iocoder.sva.module.ai.service.file.ChatbotFileService;
import cn.iocoder.sva.module.ai.service.translation.helper.FileHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.sva.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.sva.module.ai.enums.ErrorCodeConstants.FEISHU_MESSAGE_NOT_EXISTS;

/**
 * 飞书消息 Service 实现类
 *
 * @author 科兴源码
 */
@Service
@Slf4j
public class FeishuMessageServiceImpl implements FeishuMessageService {

    @Resource
    private OauthPropertiesConfig oauthPropertiesConfig;

    @Resource
    private FeishuMessageMapper feishuMessageMapper;

    @Resource
    private FileHelper fileHelper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ChatbotFileService chatbotFileService;

    @Resource
    private FeishuAiUtil feishuAiUtil;

    private String tenantAccessToken;
    private long tokenExpireTime;

    // ===================== 【新增】Redis Key 前缀 =====================
    private static final String REDIS_KEY_MESSAGE_RECEIVE_ID = "feishu:message:receive_id:";
    private static final String REDIS_KEY_CARD_ID = "feishu:card:id:";
    
    // Redis Key 前缀：飞书用户ID -> 对话ID（与 FeishuAiUtil 保持一致）
    private static final String REDIS_KEY_CONVERSATION = "feishu:conversation:";

    @Override
    public void handleMessage(FeishuMessageDO message) {
        try {
            String messageId = message.getMessageId();
            String userMessage = message.getUserMessage();
            
            String result = feishuAiUtil.handleMessageAi(userMessage, message.getUserId());
            replyMessage(message.getMessageId(), result);
        } catch (Exception e) {
            log.error("消息处理异常:{}", e);
        }
    }

    @Override
    public void handleFileMessage(FeishuMessageDTO messageDTO) {
        try {
            log.info("开始处理文件消息: userId={}, fileName={}", 
                    messageDTO.getUserId(), messageDTO.getFileName());
            
            // 1. 校验文件信息
            if (messageDTO.getFileKey() == null || messageDTO.getFileKey().isEmpty()) {
                log.error("文件消息缺少 fileKey");
                return;
            }
            
            // 2. 从飞书下载文件（需要 messageId 和 fileKey）
            log.info("正在从飞书下载文件: fileKey={}, messageId={}", 
                    messageDTO.getFileKey(), messageDTO.getMessageId());
            byte[] fileBytes = downloadFile(messageDTO.getMessageId(), messageDTO.getFileKey());
            
            if (fileBytes == null || fileBytes.length == 0) {
                log.error("下载文件为空: fileKey={}", messageDTO.getFileKey());
                return;
            }
            
            log.info("文件下载成功: fileKey={}, size={} bytes", messageDTO.getFileKey(), fileBytes.length);
            
            // 3. 上传到 MinIO
            String minioDirectory = "feishu/files";
            String minioUrl = fileHelper.uploadToMinio(fileBytes, messageDTO.getFileName(), minioDirectory);
            
            // 4. 确保会话ID存在（如果不存在则创建）
            ensureConversationExists(messageDTO.getUserId());
            
            // 5. 保存到 ChatbotFile 表
            saveFileToChatbotFile(messageDTO, fileBytes, minioUrl);
            
            // 6. 先回复用户收到文件
            String replyText = String.format("收到文件\"%s\"", messageDTO.getFileName());
            replyMessage(messageDTO.getMessageId(), replyText);
            
            // TODO: 后续逻辑处理（例如：调用翻译服务、OCR、AI分析等）
            
        } catch (Exception e) {
            log.error("处理文件消息失败", e);
        }
    }

    @Override
    public void handlePostMessage(FeishuMessageDTO messageDTO) {
        try {
            log.info("开始处理富文本消息: userId={}, text={}, imageCount={}", 
                    messageDTO.getUserId(), 
                    messageDTO.getUserMessage(),
                    messageDTO.getPostImageKeys() != null ? messageDTO.getPostImageKeys().size() : 0);
            
            StringBuilder aiInputBuilder = new StringBuilder();
            
            // 1. 添加文字内容
            if (messageDTO.getUserMessage() != null && !messageDTO.getUserMessage().isEmpty()) {
                aiInputBuilder.append(messageDTO.getUserMessage()).append("\n\n");
            }
            
            // 2. 确保会话ID存在（如果不存在则创建）
            ensureConversationExists(messageDTO.getUserId());
            
            // 3. 处理图片：下载并上传到 MinIO
            if (messageDTO.getPostImageKeys() != null && !messageDTO.getPostImageKeys().isEmpty()) {
                log.info("开始处理 {} 张图片", messageDTO.getPostImageKeys().size());
                
                for (int i = 0; i < messageDTO.getPostImageKeys().size(); i++) {
                    String imageKey = messageDTO.getPostImageKeys().get(i);
                    
                    try {
                        // 下载图片
                        log.info("正在下载图片 [{}/{}]: imageKey={}", 
                                i + 1, messageDTO.getPostImageKeys().size(), imageKey);
                        byte[] imageBytes = downloadFile(messageDTO.getMessageId(), imageKey);
                        
                        if (imageBytes != null && imageBytes.length > 0) {
                            // 上传到 MinIO
                            String imageName = "image_" + System.currentTimeMillis() + "_" + i + ".png";
                            String minioDirectory = "feishu/images";
                            String imageUrl = fileHelper.uploadToMinio(imageBytes, imageName, minioDirectory);
                            
                            // 保存到 ChatbotFile 表
                            saveImageToChatbotFile(messageDTO, imageBytes, imageUrl, imageName);
                            
                            // 添加到 AI 输入
                            aiInputBuilder.append(String.format("[图片%d](%s)\n", i + 1, imageUrl));
                            
                            log.info("图片 [{}/{}] 已上传到 MinIO: {}", 
                                    i + 1, messageDTO.getPostImageKeys().size(), imageUrl);
                        }
                    } catch (Exception e) {
                        log.error("处理图片失败: imageKey={}", imageKey, e);
                        aiInputBuilder.append(String.format("[图片%d 处理失败]\n", i + 1));
                    }
                }
            }
            
            // 4. 调用 AI 处理（文字+图片URL）
            String aiResponse = feishuAiUtil.handleMessageAi(aiInputBuilder.toString().trim(), messageDTO.getUserId());
            
            // 5. 回复用户
            replyMessage(messageDTO.getMessageId(), aiResponse);
            
            log.info("富文本消息处理完成: userId={}", messageDTO.getUserId());
            
        } catch (Exception e) {
            log.error("处理富文本消息失败", e);
            try {
                replyMessage(messageDTO.getMessageId(), "消息处理失败，请稍后重试");
            } catch (Exception ex) {
                log.error("回复失败", ex);
            }
        }
    }

    /**
     * 保存文件信息到 ChatbotFile 表
     */
    private void saveFileToChatbotFile(FeishuMessageDTO messageDTO, byte[] fileBytes, String minioUrl) {
        try {
            ChatbotFileDO chatbotFile = new ChatbotFileDO();
            
            // 文件名称
            chatbotFile.setFileName(messageDTO.getFileName());
            
            // 文件路径（MinIO URL）
            chatbotFile.setFilePath(minioUrl);
            
            // 用户问题（使用文件消息的提示文本）
            chatbotFile.setUserQuestion(messageDTO.getUserMessage());
            
            // 文件大小
            chatbotFile.setFileSize((long) fileBytes.length);
            
            // 文件类型
            chatbotFile.setFileType("feishuBot");
            
            // 文件后缀（从文件名提取）
            String fileName = messageDTO.getFileName();
            if (fileName != null && fileName.contains(".")) {
                String extension = fileName.substring(fileName.lastIndexOf("."));
                chatbotFile.setFileExtension(extension);
            }
            
            // 上传时间
            chatbotFile.setUploadTime(LocalDateTime.now());
            
            // 用户工号（使用 userId）
            chatbotFile.setUserJobNumber(messageDTO.getUserId());
            
            // 会话ID（从 Redis 获取 AI 对话的 conversationId）
            String conversationId = getConversationIdFromUserId(messageDTO.getUserId());
            chatbotFile.setSessionId(conversationId);
            
            // 保存到数据库
            ChatbotFileSaveReqVO saveReqVO = new ChatbotFileSaveReqVO();
            BeanUtils.copyProperties(chatbotFile, saveReqVO);
            chatbotFileService.createChatbotFile(saveReqVO);
            
            log.info("文件信息已保存到 ChatbotFile 表: fileName={}, fileId={}, sessionId={}", 
                    chatbotFile.getFileName(), chatbotFile.getId(), conversationId);
        } catch (Exception e) {
            log.error("保存文件信息到 ChatbotFile 表失败", e);
        }
    }

    /**
     * 保存图片信息到 ChatbotFile 表
     */
    private void saveImageToChatbotFile(FeishuMessageDTO messageDTO, byte[] imageBytes, String minioUrl, String imageName) {
        try {
            ChatbotFileDO chatbotFile = new ChatbotFileDO();
            
            // 文件名称
            chatbotFile.setFileName(imageName);
            
            // 文件路径（MinIO URL）
            chatbotFile.setFilePath(minioUrl);
            
            // 用户问题（使用富文本消息的文字内容）
            chatbotFile.setUserQuestion(messageDTO.getUserMessage());
            
            // 文件大小
            chatbotFile.setFileSize((long) imageBytes.length);
            
            // 文件类型（图片）
            chatbotFile.setFileType("feishuBot");
            
            // 文件后缀（从文件名中提取）
            if (imageName != null && imageName.contains(".")) {
                String extension = imageName.substring(imageName.lastIndexOf("."));
                chatbotFile.setFileExtension(extension);
            } else {
                chatbotFile.setFileExtension(".png"); // 默认后缀
            }
            
            // 上传时间
            chatbotFile.setUploadTime(LocalDateTime.now());
            
            // 用户工号（使用 userId）
            chatbotFile.setUserJobNumber(messageDTO.getUserId());
            
            // 会话ID（从 Redis 获取 AI 对话的 conversationId）
            String conversationId = getConversationIdFromUserId(messageDTO.getUserId());
            chatbotFile.setSessionId(conversationId);
            
            // 保存到数据库
            ChatbotFileSaveReqVO saveReqVO = new ChatbotFileSaveReqVO();
            BeanUtils.copyProperties(chatbotFile, saveReqVO);
            chatbotFileService.createChatbotFile(saveReqVO);
            
            log.info("图片信息已保存到 ChatbotFile 表: imageName={}, fileId={}, sessionId={}", 
                    chatbotFile.getFileName(), chatbotFile.getId(), conversationId);
        } catch (Exception e) {
            log.error("保存图片信息到 ChatbotFile 表失败", e);
        }
    }

    /**
     * 智能回复：根据配置选择回复方式
     */
    private void replyMessage(String messageId, String text) {
        try {
            String replyMode = oauthPropertiesConfig.getFeishu().getReplyMode();
            
            if ("stream".equalsIgnoreCase(replyMode)) {
                // 流式卡片回复
                replyStreamMessage(messageId, text);
            } else {
                // 普通文本回复
                replyTextMessage(messageId, text);
            }
        } catch (Exception e) {
            log.error("回复消息失败: messageId={}", messageId, e);
        }
    }

    @Override
    public String replyStreamMessage(String messageId, String text) throws Exception {
        OauthPropertiesConfig.FeiShu feishu = oauthPropertiesConfig.getFeishu();
        
        // 1. 从 Redis 获取 receive_id
        String receiveId = getReceiveIdFromMessage(messageId);
        if (receiveId == null) {
            log.warn("无法获取 receive_id，降级为文本回复: messageId={}", messageId);
            return replyTextMessage(messageId, text);
        }
        
        // 2. 创建流式卡片实体
        String cardId = createStreamingCard(feishu);
        if (cardId == null) {
            log.warn("创建卡片实体失败，降级为文本回复: messageId={}", messageId);
            return replyTextMessage(messageId, text);
        }
        
        // 3. 发送卡片消息
        boolean sendSuccess = sendCardMessage(feishu, receiveId, cardId);
        if (!sendSuccess) {
            log.warn("发送卡片消息失败，降级为文本回复: messageId={}", messageId);
            return replyTextMessage(messageId, text);
        }
        
        // 4. 流式更新卡片内容
        updateCardContent(feishu, cardId, text);
        
        log.info("流式卡片回复成功: messageId={}, cardId={}", messageId, cardId);
        return cardId;
    }

    /**
     * ===================== 【新增】从 Redis 获取 receive_id =====================
     */
    private String getReceiveIdFromMessage(String messageId) {
        if (messageId == null) {
            return null;
        }
        
        try {
            String redisKey = REDIS_KEY_MESSAGE_RECEIVE_ID + messageId;
            return stringRedisTemplate.opsForValue().get(redisKey);
        } catch (Exception e) {
            log.error("获取 receive_id 失败: messageId={}", messageId, e);
            return null;
        }
    }

    /**
     * ===================== 【新增】从 Redis 获取 conversation_id =====================
     */
    private String getConversationIdFromUserId(String userId) {
        if (userId == null) {
            return null;
        }
        
        try {
            String redisKey = REDIS_KEY_CONVERSATION + userId;
            String conversationId = stringRedisTemplate.opsForValue().get(redisKey);
            return conversationId;
        } catch (Exception e) {
            log.error("获取 conversationId 失败: userId={}", userId, e);
            return null;
        }
    }

    /**
     * ===================== 【新增】确保会话ID存在（如果不存在则创建） =====================
     */
    private void ensureConversationExists(String userId) {
        try {
            // 1. 检查 Redis 中是否已有 conversationId
            String conversationId = getConversationIdFromUserId(userId);
            if (conversationId != null) {
                return;
            }
            
            // 2. 不存在，通过调用 AI 创建一个空对话来初始化
            feishuAiUtil.handleMessageAi("你好", userId);
            
            // 3. 验证是否创建成功
            conversationId = getConversationIdFromUserId(userId);
            if (conversationId != null) {
                log.info("成功为用户 {} 创建会话: conversationId={}", userId, conversationId);
            } else {
                log.warn("为用户 {} 创建会话失败", userId);
            }
        } catch (Exception e) {
            log.error("确保会话存在时发生异常: userId={}", userId, e);
        }
    }

    /**
     * ===================== 【新增】创建流式卡片实体 =====================
     */
    private String createStreamingCard(OauthPropertiesConfig.FeiShu feishu) throws Exception {
        String url = feishu.getCreateCardUrl();

        // 构造卡片 JSON 2.0（开启流式模式）
        Map<String, Object> cardJson = new HashMap<>();
        cardJson.put("schema", "2.0");

        // Header
        Map<String, Object> header = new HashMap<>();
        header.put("title", new HashMap<String, Object>() {{
            put("tag", "plain_text");
            put("content", "AI 智能助手");
        }});
        header.put("template", "blue");
        cardJson.put("header", header);

        // Config（开启流式模式）
        Map<String, Object> config = new HashMap<>();
        config.put("streaming_mode", true);
        config.put("wide_screen_mode", true);

        // 流式配置
        Map<String, Object> streamingConfig = new HashMap<>();
        streamingConfig.put("print_frequency_ms", new HashMap<String, Object>() {{
            put("default", 70);
        }});
        streamingConfig.put("print_step", new HashMap<String, Object>() {{
            put("default", 1);
        }});
        streamingConfig.put("print_strategy", "fast");
        config.put("streaming_config", streamingConfig);

        cardJson.put("config", config);

        // Body（初始为空）
        Map<String, Object> body = new HashMap<>();
        body.put("elements", new ArrayList<Map<String, Object>>() {{
            add(new HashMap<String, Object>() {{
                put("tag", "markdown");
                put("content", "");
                put("element_id", "markdown_content");
            }});
        }});
        cardJson.put("body", body);

        // 转换为 JSON 字符串
        String cardData = new ObjectMapper().writeValueAsString(cardJson);

        // 构造请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("type", "card_json");
        requestBody.put("data", cardData);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getTenantAccessToken());

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                if (Integer.valueOf(0).equals(responseBody.get("code"))) {
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                    String cardId = (String) data.get("card_id");
                    log.info("创建卡片实体成功: cardId={}", cardId);
                    return cardId;
                } else {
                    log.error("创建卡片实体失败: code={}, msg={}", responseBody.get("code"), responseBody.get("msg"));
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("创建卡片实体异常", e);
        }

        return null;
    }

    /**
     * ===================== 【新增】发送卡片消息 =====================
     */
    private boolean sendCardMessage(OauthPropertiesConfig.FeiShu feishu, String receiveId, String cardId) throws Exception {
        String url = feishu.getSendMessageUrl();

        // 构造 content
        Map<String, Object> contentData = new HashMap<>();
        contentData.put("type", "card");
        contentData.put("data", new HashMap<String, Object>() {{
            put("card_id", cardId);
        }});
        String contentJson = new ObjectMapper().writeValueAsString(contentData);

        // 构造请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("receive_id", receiveId);
        requestBody.put("msg_type", "interactive");
        requestBody.put("content", contentJson);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getTenantAccessToken());

        // 设置查询参数
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("receive_id_type", "user_id");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                if (Integer.valueOf(0).equals(responseBody.get("code"))) {
                    log.info("发送卡片消息成功: receiveId={}, cardId={}", receiveId, cardId);
                    return true;
                } else {
                    log.error("发送卡片消息失败: code={}, msg={}", responseBody.get("code"), responseBody.get("msg"));
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("发送卡片消息异常", e);
        }

        return false;
    }

    /**
     * ===================== 【新增】流式更新卡片内容 =====================
     */
    private void updateCardContent(OauthPropertiesConfig.FeiShu feishu, String cardId, String text) throws Exception {
        String url = feishu.getUpdateCardContentUrl();

        // 替换 URL 中的占位符
        url = url.replace("{card_id}", cardId)
                 .replace("{element_id}", "markdown_content");

        // 构造请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("content", text);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getTenantAccessToken());

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                if (Integer.valueOf(0).equals(responseBody.get("code"))) {
                    log.info("更新卡片内容成功: cardId={}, textLength={}", cardId, text.length());
                } else {
                    log.error("更新卡片内容失败: code={}, msg={}", responseBody.get("code"), responseBody.get("msg"));
                }
            }
        } catch (Exception e) {
            log.error("更新卡片内容异常", e);
        }
    }

    /**
     * 从飞书下载文件
     */
    private byte[] downloadFile(String messageId, String fileKey) throws Exception {
        OauthPropertiesConfig.FeiShu feishu = oauthPropertiesConfig.getFeishu();
        String url = feishu.getDownloadFileUrl();

        // 构建 URL，替换占位符
        String downloadUrl = url.replace("{message_id}", messageId)
                                .replace("{file_key}", fileKey);

        // 设置请求头，携带认证令牌
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getTenantAccessToken());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    downloadUrl,
                    HttpMethod.GET,
                    requestEntity,
                    byte[].class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                log.error("文件下载失败, 状态码: {}", response.getStatusCode());
                throw new RuntimeException("文件下载失败: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("文件下载发生异常, messageId={}, fileKey={}", messageId, fileKey, e);
            throw e;
        }
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(Long size) {
        if (size == null || size == 0) {
            return "0 B";
        }
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        double fileSize = size.doubleValue();

        while (fileSize >= 1024 && unitIndex < units.length - 1) {
            fileSize /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", fileSize, units[unitIndex]);
    }

    @Override
    public String sendTextMessage(String receiveId, String receiveIdType, String text) throws Exception {
        OauthPropertiesConfig.FeiShu feishu = oauthPropertiesConfig.getFeishu();
        String url = feishu.getSendMessageUrl();

        // 构造消息内容
        Map<String, String> contentMap = new HashMap<>();
        contentMap.put("text", text);
        String contentJson = new ObjectMapper().writeValueAsString(contentMap);

        // 构造请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("receive_id", receiveId);
        requestBody.put("msg_type", "text");
        requestBody.put("content", contentJson);

        // 设置请求头，携带认证令牌
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getTenantAccessToken());

        // 设置查询参数，指定接收者ID类型
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("receive_id_type", receiveIdType);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            // 发送请求
            ResponseEntity<Map> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            // 处理响应
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> body = response.getBody();
                if (Integer.valueOf(0).equals(body.get("code"))) {
                    Map<String, Object> data = (Map<String, Object>) body.get("data");
                    return (String) data.get("message_id");
                } else {
                    log.error("发送飞书消息失败，错误码: {}, 错误信息: {}", body.get("code"), body.get("msg"));
                    throw new RuntimeException("发送飞书消息失败: " + body.get("msg"));
                }
            }
        } catch (HttpClientErrorException e) {
            log.error("飞书消息发送HTTP客户端错误，状态码: {}, 响应体: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("飞书消息发送发生未知异常", e);
            throw e;
        }
        return null;
    }

    @Override
    public String replyTextMessage(String messageId, String text) throws Exception {
        return replyTextMessage(messageId, text, null, null);
    }

    @Override
    public String replyTextMessage(String messageId, String text, Boolean replyInThread, String uuid) throws Exception {
        OauthPropertiesConfig.FeiShu feishu = oauthPropertiesConfig.getFeishu();
        String url = feishu.getReplyMessageUrl();

        // 构造消息内容
        Map<String, String> contentMap = new HashMap<>();
        contentMap.put("text", text);
        String contentJson = new ObjectMapper().writeValueAsString(contentMap);

        // 构造请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("content", contentJson);
        requestBody.put("msg_type", "text");
        // 可选参数：话题回复
        if (replyInThread != null) {
            requestBody.put("reply_in_thread", replyInThread);
        }
        // 可选参数：去重UUID
        if (uuid != null) {
            requestBody.put("uuid", uuid);
        }

        // 设置请求头，携带认证令牌
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getTenantAccessToken());

        // 构建URL，替换路径中的message_id占位符
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .uriVariables(Map.of("message_id", messageId));

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            // 发送请求
            ResponseEntity<Map> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            // 处理响应
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> body = response.getBody();
                if (Integer.valueOf(0).equals(body.get("code"))) {
                    Map<String, Object> data = (Map<String, Object>) body.get("data");
                    return (String) data.get("message_id");
                } else {
                    log.error("回复飞书消息失败，错误码: {}, 错误信息: {}", body.get("code"), body.get("msg"));
                    throw new RuntimeException("回复飞书消息失败: " + body.get("msg"));
                }
            }
        } catch (HttpClientErrorException e) {
            log.error("飞书消息回复HTTP客户端错误，状态码: {}, 响应体: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("飞书消息回复发生未知异常", e);
            throw e;
        }
        return null;
    }

    /**
     * 获取 tenant_access_token (带缓存，有效期2小时，提前5分钟刷新)
     */
    private String getTenantAccessToken() throws Exception {
        // 缓存未过期，直接返回
        if (tenantAccessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return tenantAccessToken;
        }
        OauthPropertiesConfig.FeiShu feishu = oauthPropertiesConfig.getFeishu();
        String appId = feishu.getClientId();
        String appSecret = feishu.getClientSecret();

        // 请求飞书接口获取新令牌
        String url = feishu.getTenantAccessTokenUrl();
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("app_id", appId);
        requestBody.put("app_secret", appSecret);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.postForEntity(url, requestBody, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            if (Integer.valueOf(0).equals(body.get("code"))) {
                tenantAccessToken = (String) body.get("tenant_access_token");
                // 令牌有效期通常为7200秒，这里提前5分钟刷新，避免过期
                tokenExpireTime = System.currentTimeMillis() + 6600 * 1000;
                return tenantAccessToken;
            } else {
                log.error("获取飞书tenant_access_token失败: {}", body);
                throw new RuntimeException("获取飞书访问令牌失败");
            }
        }
        throw new RuntimeException("请求飞书认证接口失败");
    }



    @Override
    public Long createFeishuMessage(FeishuMessageSaveReqVO createReqVO) {
        // 插入
        FeishuMessageDO feishuMessage = BeanUtils.toBean(createReqVO, FeishuMessageDO.class);
        feishuMessageMapper.insert(feishuMessage);

        // 返回
        return feishuMessage.getId();
    }

    @Override
    public void updateFeishuMessage(FeishuMessageSaveReqVO updateReqVO) {
        // 校验存在
        validateFeishuMessageExists(updateReqVO.getId());
        // 更新
        FeishuMessageDO updateObj = BeanUtils.toBean(updateReqVO, FeishuMessageDO.class);
        feishuMessageMapper.updateById(updateObj);
    }

    @Override
    public void deleteFeishuMessage(Long id) {
        // 校验存在
        validateFeishuMessageExists(id);
        // 删除
        feishuMessageMapper.deleteById(id);
    }

    @Override
    public void deleteFeishuMessageListByIds(List<Long> ids) {
        // 删除
        feishuMessageMapper.deleteByIds(ids);
    }


    private void validateFeishuMessageExists(Long id) {
        if (feishuMessageMapper.selectById(id) == null) {
            throw exception(FEISHU_MESSAGE_NOT_EXISTS);
        }
    }

    @Override
    public FeishuMessageDO getFeishuMessage(Long id) {
        return feishuMessageMapper.selectById(id);
    }

    @Override
    public PageResult<FeishuMessageDO> getFeishuMessagePage(FeishuMessagePageReqVO pageReqVO) {
        return feishuMessageMapper.selectPage(pageReqVO);
    }

}
