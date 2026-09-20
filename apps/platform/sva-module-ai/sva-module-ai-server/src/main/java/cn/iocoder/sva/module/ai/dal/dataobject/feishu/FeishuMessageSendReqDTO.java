package cn.iocoder.sva.module.ai.dal.dataobject.feishu;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 飞书文本消息发送 Request DTO
 *
 * @author like
 */
@Data
@Schema(description = "飞书文本消息发送请求")
public class FeishuMessageSendReqDTO {

    @Schema(description = "接收者ID，根据receive_id_type的不同，对应不同的标识",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "ou_123456789abcdef")
    @NotBlank(message = "接收者ID不能为空")
    private String receiveId;

    @Schema(description = "接收者ID类型，可选值：user_id(用户ID)、email(邮箱)、chat_id(群ID)等",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "user_id")
    @NotBlank(message = "接收者ID类型不能为空")
    private String receiveIdType;

    @Schema(description = "文本消息内容",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "你好，这是一条测试飞书消息")
    @NotBlank(message = "消息内容不能为空")
    private String text;

}
