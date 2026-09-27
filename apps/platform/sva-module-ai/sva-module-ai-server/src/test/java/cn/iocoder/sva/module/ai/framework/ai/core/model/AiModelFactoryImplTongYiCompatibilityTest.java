package cn.iocoder.sva.module.ai.framework.ai.core.model;

import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class AiModelFactoryImplTongYiCompatibilityTest {

    @Test
    void qwenWorkspaceCompatibleEndpointUsesOpenAiClientAndNormalizesV1Suffix() {
        try (MockedStatic<SpringUtil> spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean(ToolCallingManager.class))
                    .thenReturn(mock(ToolCallingManager.class));
            ChatModel model = ReflectionTestUtils.invokeMethod(
                    AiModelFactoryImpl.class,
                    "buildTongYiChatModel",
                    "workspace-key",
                    "https://dashscope.aliyuncs.com/compatible-mode/v1/",
                    "qwen3.8-flash",
                    0.2);

            assertThat(model).isInstanceOf(OpenAiChatModel.class);
        }
    }
}
