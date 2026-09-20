package cn.iocoder.sva.module.ai.config;

import cn.iocoder.sva.module.ai.dal.dataobject.feishu.FeishuMessageDO;
import cn.iocoder.sva.module.ai.dal.dataobject.feishu.FeishuMessageDTO;
import cn.iocoder.sva.module.ai.dal.mysql.feishu.FeishuMessageMapper;
import cn.iocoder.sva.module.ai.service.feishu.FeishuMessageService;
import cn.iocoder.sva.module.ai.tool.function.FeishuAiAssistantTools;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.service.im.ImService;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1Data;
import com.lark.oapi.ws.Client;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class FeishuBotConfig {

    @Resource
    private OauthPropertiesConfig oauthPropertiesConfig;

    @Resource
    private FeishuMessageMapper feishuMessageMapper;

    @Resource
    private FeishuMessageService feishuMessageService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private Client wsClient;

    // 用于解析JSON
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // ===================== 【新增】Redis Key 前缀：messageId -> receiveId =====================
    private static final String REDIS_KEY_MESSAGE_RECEIVE_ID = "feishu:message:receive_id:";

    // ===================== 【新增】Redis Key 前缀：线程ID -> 飞书userId =====================
    private static final String REDIS_KEY_CURRENT_FEISHU_USER_ID = "feishu:user:current:";

    // 缓存过期时间：2小时（与飞书消息有效期一致）
    private static final long CACHE_EXPIRE_HOURS = 2;

    // ===================== 注入并行处理线程池 =====================
    @Resource(name = "feishuMessageExecutor")
    private Executor feishuMessageExecutor;

    // ===================== 【核心】高并发线程池配置 =====================
    @Bean(name = "feishuMessageExecutor")
    public Executor feishuMessageExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);     // 核心线程
        executor.setMaxPoolSize(30);      // 最大并发线程
        executor.setQueueCapacity(200);   // 缓冲队列
        executor.setThreadNamePrefix("feishu-msg-");
        executor.initialize();
        return executor;
    }

    /**
     * 创建飞书AI助手工具回调提供者
     * <p>
     * 使用 MethodToolCallbackProvider 将 FeishuAiAssistantTools 中的所有 @Tool 方法
     * 注册为可被 ToolCallbackResolver 解析的工具
     *
     * @return 工具回调提供者
     */
    @Bean("feishuAiAssistantToolCallbackProvider")
    public MethodToolCallbackProvider feishuAiAssistantToolCallbackProvider(FeishuAiAssistantTools tools) {
        log.info("开始注册飞书AI助手工具回调提供者");
        if (tools == null) {
            log.warn("FeishuAiAssistantTools 未找到，跳过工具注册");
            return null;
        }
        MethodToolCallbackProvider provider = MethodToolCallbackProvider.builder()
                .toolObjects(tools)
                .build();
        log.info("飞书AI助手工具回调提供者注册成功");
        return provider;
    }

    @Bean
    public EventDispatcher eventDispatcher() {
        return EventDispatcher.newBuilder("", "")
                .onP2MessageReceiveV1(new ImService.P2MessageReceiveV1Handler() {
                    @Override
                    public void handle(P2MessageReceiveV1 event) {
                        // ===================== 并行处理：不阻塞、不排队 =====================
                        feishuMessageExecutor.execute(() -> {
                            handleMessage(event);
                        });
                    }
                })
                .build();
    }

    // 将业务逻辑单独封装
    private void handleMessage(P2MessageReceiveV1 event) {
        try {
            FeishuMessageDTO messageDTO = parseFeishuMessage(event);

            // ===================== 【新增】保存 messageId -> receiveId 映射到 Redis =====================
            saveReceiveIdMapping(messageDTO.getMessageId(), messageDTO.getUserId());

            // ===================== 【新增】保存当前线程的飞书userId到Redis =====================
            saveCurrentFeishuUserId(messageDTO.getUserId());

            // ===================== 【新增】打印接收到的完整用户信息 =====================
//            log.info("========== 飞书消息接收详情 ==========");
//            log.info("[消息基础信息] messageId={}, messageType={}, chatType={}, chatId={}",
//                    messageDTO.getMessageId(),
//                    messageDTO.getMessageType(),
//                    messageDTO.getChatType(),
//                    messageDTO.getChatId());
//
//            log.info("[发送者信息] userId={}, openId={}, unionId={}, tenantKey={}, senderType={}",
//                    messageDTO.getUserId(),
//                    messageDTO.getOpenId(),
//                    messageDTO.getUnionId(),
//                    messageDTO.getTenantKey(),
//                    messageDTO.getSenderType());
//
//            log.info("[消息内容] userMessage={}", messageDTO.getUserMessage());
//
//            if ("file".equals(messageDTO.getMessageType())) {
//                log.info("[文件信息] fileName={}, fileKey={}, fileType={}, fileSize={}",
//                        messageDTO.getFileName(),
//                        messageDTO.getFileKey(),
//                        messageDTO.getFileType(),
//                        messageDTO.getFileSize());
//            }
//
//            if (messageDTO.getPostImageKeys() != null && !messageDTO.getPostImageKeys().isEmpty()) {
//                log.info("[富文本图片] imageCount={}, imageKeys={}",
//                        messageDTO.getPostImageKeys().size(),
//                        messageDTO.getPostImageKeys());
//            }
//
//            log.info("======================================");

            // 判断消息类型
            if ("file".equals(messageDTO.getMessageType())) {
                // 文件消息：直接调用服务处理，不保存到数据库
                log.info("[消息分发] 处理文件消息");
                feishuMessageService.handleFileMessage(messageDTO);
            } else if ("post".equals(messageDTO.getMessageType())) {
                // 富文本消息：包含文字+图片，调用服务处理
                log.info("[消息分发] 处理富文本消息");
                feishuMessageService.handlePostMessage(messageDTO);
            } else {
                // 文本消息和其他消息：保存到数据库并处理
                log.info("[消息分发] 处理文本消息，准备保存到数据库");
                FeishuMessageDO feishuMessageDO = convertToFeishuMessageDO(messageDTO);
                
                log.info("[数据库插入] 即将插入 FeishuMessageDO: userId={}, userMessage={}, messageType={}", 
                        feishuMessageDO.getUserId(), 
                        feishuMessageDO.getUserMessage(), 
                        feishuMessageDO.getMessageType());
                
                feishuMessageMapper.insert(feishuMessageDO);
                
                log.info("[数据库插入] 插入成功, id={}, createTime={}", 
                        feishuMessageDO.getId(), 
                        feishuMessageDO.getCreateTime());
                
                log.info("[消息分发] 调用 feishuMessageService.handleMessage");
                feishuMessageService.handleMessage(feishuMessageDO);
            }

        } catch (Exception e) {
            log.error("消息处理异常", e);
        } finally {
            // ===================== 【新增】清理当前线程的飞书userId =====================
            clearCurrentFeishuUserId();
        }
    }

    // ===================== 原来的解析方法（完全不动） =====================
    private FeishuMessageDTO parseFeishuMessage(P2MessageReceiveV1 event) throws Exception {
        FeishuMessageDTO dto = new FeishuMessageDTO();

        // 1. 原始JSON
        String originalJson = Jsons.DEFAULT.toJson(event.getEvent());
        dto.setOriginalJson(originalJson);

        // 2. 三个用户ID
        P2MessageReceiveV1Data eventData = event.getEvent();
        
        String userId = eventData.getSender().getSenderId().getUserId();
        String openId = eventData.getSender().getSenderId().getOpenId();
        String unionId = eventData.getSender().getSenderId().getUnionId();
        
        dto.setUserId(userId);
        dto.setOpenId(openId);
        dto.setUnionId(unionId);

        String messageType = eventData.getMessage().getMessageType();
        String messageId = eventData.getMessage().getMessageId();
        String chatType = eventData.getMessage().getChatType();
        String chatId = eventData.getMessage().getChatId();
        String tenantKey = eventData.getSender().getTenantKey();
        String senderType = eventData.getSender().getSenderType();
        
        dto.setMessageType(messageType);
        dto.setMessageId(messageId);
        dto.setChatType(chatType);
        dto.setChatId(chatId);
        dto.setTenantKey(tenantKey);
        dto.setSenderType(senderType);
        
        log.debug("[消息解析] 开始解析飞书消息: messageId={}, messageType={}, userId={}", 
                messageId, messageType, userId);

        // 3. 根据消息类型解析不同的内容
        String contentJson = eventData.getMessage().getContent();

        if ("text".equals(messageType)) {
            // 文本消息
            Map<String, String> contentMap = objectMapper.readValue(
                    contentJson,
                    new TypeReference<Map<String, String>>() {}
            );
            String text = contentMap.get("text");
            dto.setUserMessage(text);
            
            log.debug("[消息解析] 文本消息解析完成: text={}", text);

        } else if ("file".equals(messageType)) {
            // 文件消息
            Map<String, Object> contentMap = objectMapper.readValue(
                    contentJson,
                    new TypeReference<Map<String, Object>>() {}
            );

            // 提取文件信息
            String fileKey = (String) contentMap.get("file_key");
            String fileName = (String) contentMap.get("file_name");
            String fileType = (String) contentMap.get("file_type");
            
            dto.setFileKey(fileKey);
            dto.setFileName(fileName);

            // 文件大小可能是 Integer 或 Long
            Object fileSizeObj = contentMap.get("file_size");
            if (fileSizeObj instanceof Number) {
                dto.setFileSize(((Number) fileSizeObj).longValue());
            }

            dto.setFileType(fileType);

            // 设置提示文本
            dto.setUserMessage(String.format("[文件] %s (%s)",
                    dto.getFileName(),
                    formatFileSize(dto.getFileSize())));

            log.info("收到文件消息: fileName={}, fileKey={}, fileSize={}",
                    dto.getFileName(), dto.getFileKey(), dto.getFileSize());

        } else if ("post".equals(messageType)) {
            // 富文本消息（可能包含文字+图片）
            Map<String, Object> contentMap = objectMapper.readValue(
                    contentJson,
                    new TypeReference<Map<String, Object>>() {}
            );

            // 打印原始 JSON 用于调试
            log.info("富文本消息原始 JSON: {}", contentJson);

            // 解析富文本内容
            parsePostContent(contentMap, dto);

            log.info("收到富文本消息: userMessage={}, imageCount={}",
                    dto.getUserMessage(),
                    dto.getPostImageKeys() != null ? dto.getPostImageKeys().size() : 0);
        }

        log.debug("[消息解析] 消息解析完成: messageType={}, userMessageLength={}", 
                messageType, 
                dto.getUserMessage() != null ? dto.getUserMessage().length() : 0);
        
        return dto;
    }

    /**
     * 解析富文本消息内容
     */
    private void parsePostContent(Map<String, Object> contentMap, FeishuMessageDTO dto) {
        StringBuilder textBuilder = new StringBuilder();
        List<String> imageKeys = new ArrayList<>();

        try {
            // 飞书富文本结构（实际格式）：
            // {"title":"", "content":[[{"tag":"img",...}, {"tag":"text",...}]]}

            // 提取标题
            Object title = contentMap.get("title");
            if (title != null && !title.toString().isEmpty()) {
                textBuilder.append(title.toString()).append("\n");
            }

            // 提取内容数组
            Object contentObj = contentMap.get("content");
            if (contentObj instanceof List) {
                List<?> contentArray = (List<?>) contentObj;

                for (Object rowObj : contentArray) {
                    if (!(rowObj instanceof List)) {
                        continue;
                    }

                    List<?> row = (List<?>) rowObj;
                    for (Object elementObj : row) {
                        if (!(elementObj instanceof Map)) {
                            continue;
                        }

                        Map<String, Object> element = (Map<String, Object>) elementObj;
                        String tag = (String) element.get("tag");

                        if ("text".equals(tag)) {
                            // 文本元素
                            Object text = element.get("text");
                            if (text != null) {
                                textBuilder.append(text.toString());
                            }
                        } else if ("img".equals(tag)) {
                            // 图片元素
                            Object imageKey = element.get("image_key");
                            if (imageKey != null) {
                                imageKeys.add(imageKey.toString());
                                textBuilder.append("[图片]");
                            }
                        } else if ("a".equals(tag)) {
                            // 链接元素
                            Object text = element.get("text");
                            if (text != null) {
                                textBuilder.append(text.toString());
                            }
                        }
                    }
                    textBuilder.append("\n");
                }
            }
        } catch (Exception e) {
            log.error("解析富文本消息失败", e);
            textBuilder.append("[富文本解析失败]");
        }

        dto.setUserMessage(textBuilder.toString().trim());
        dto.setPostImageKeys(imageKeys);
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

    @PostConstruct
    public void startWsClient() {
        OauthPropertiesConfig.FeiShu feishu = oauthPropertiesConfig.getFeishu();
        String appId = feishu.getClientId();
        String appSecret = feishu.getClientSecret();

        wsClient = new Client.Builder(appId, appSecret)
                .eventHandler(eventDispatcher())
                .build();

        new Thread(() -> {
            try {
                wsClient.start();
                log.info("飞书长连接启动成功");
            } catch (Exception e) {
                log.error("飞书长连接启动失败", e);
            }
        }).start();
    }

    /**
     * ===================== 【新增】保存当前线程的飞书userId到Redis =====================
     */
    private void saveCurrentFeishuUserId(String feishuUserId) {
        if (feishuUserId == null) {
            return;
        }

        try {
            String threadId = String.valueOf(Thread.currentThread().getId());
            String redisKey = REDIS_KEY_CURRENT_FEISHU_USER_ID + threadId;
            stringRedisTemplate.opsForValue().set(redisKey, feishuUserId, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            log.debug("保存当前线程的飞书userId: threadId={}, feishuUserId={}", threadId, feishuUserId);
        } catch (Exception e) {
            log.error("保存当前线程的飞书userId失败", e);
        }
    }

    /**
     * ===================== 【新增】清理当前线程的飞书userId =====================
     */
    private void clearCurrentFeishuUserId() {
        try {
            String threadId = String.valueOf(Thread.currentThread().getId());
            String redisKey = REDIS_KEY_CURRENT_FEISHU_USER_ID + threadId;
            stringRedisTemplate.delete(redisKey);
            log.debug("清理当前线程的飞书userId: threadId={}", threadId);
        } catch (Exception e) {
            log.error("清理当前线程的飞书userId失败", e);
        }
    }

    /**
     * ===================== 【新增】保存 messageId -> receiveId 映射 =====================
     */
    private void saveReceiveIdMapping(String messageId, String receiveId) {
        if (messageId == null || receiveId == null) {
            return;
        }

        try {
            String redisKey = REDIS_KEY_MESSAGE_RECEIVE_ID + messageId;
            stringRedisTemplate.opsForValue().set(redisKey, receiveId, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            log.debug("保存 messageId -> receiveId 映射: {} -> {}", messageId, receiveId);
        } catch (Exception e) {
            log.error("保存 receiveId 映射失败: messageId={}", messageId, e);
        }
    }

    /**
     * 将 FeishuMessageDTO 转换为 FeishuMessageDO
     */
    private FeishuMessageDO convertToFeishuMessageDO(FeishuMessageDTO dto) {
        if (dto == null) {
            return null;
        }

        FeishuMessageDO messageDO = new FeishuMessageDO();

        // 1. 用户信息
        messageDO.setUserId(dto.getUserId());
        messageDO.setOpenId(dto.getOpenId());
        messageDO.setUnionId(dto.getUnionId());

        // 2. 消息内容
        messageDO.setUserMessage(dto.getUserMessage());
        messageDO.setOriginalJson(dto.getOriginalJson());

        // 3. 消息元数据
        messageDO.setMessageType(dto.getMessageType());
        messageDO.setMessageId(dto.getMessageId());
        messageDO.setChatType(dto.getChatType());
        messageDO.setChatId(dto.getChatId());
        messageDO.setTenantKey(dto.getTenantKey());
        messageDO.setSenderType(dto.getSenderType());

        // 4. 回复相关（默认未回复）
        messageDO.setReplyContent(null);
        messageDO.setReplySuccess(false);

        // 5. 创建时间（自动赋值当前时间）
        messageDO.setCreateTime(LocalDateTime.now());

        return messageDO;
    }
}
