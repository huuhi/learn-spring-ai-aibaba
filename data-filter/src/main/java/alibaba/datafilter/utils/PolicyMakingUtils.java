package alibaba.datafilter.utils;

import alibaba.datafilter.model.em.QuestionType;
import jakarta.validation.constraints.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;

import java.util.Objects;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/24
 * 说明:
 */
public class PolicyMakingUtils {
    private final ChatClient chatClient;

    public PolicyMakingUtils(@NotNull ChatClient.Builder builder) {
        chatClient=builder.defaultSystem(
                        """
                              你是一名决策专家，你需要根据用户的问题，将问题分成这几类:
                                    // 1. 知识解答型 - 需要准确性和深度
                                    KNOWLEDGE_QUESTIONS,
                                    // 2. 操作指导型 - 需要步骤清晰和实用性
                                    GUIDANCE_QUESTIONS
                                    // 3. 创意生成型 - 需要创造性和启发性
                                    CREATIVE_QUESTIONS,
                                    // 4. 个人交互型 - 需要温度和同理心
                                    PERSONAL_QUESTIONS
                              最终输出问题类型
                              """)
                .defaultAdvisors(SimpleLoggerAdvisor.builder().build())
//                使用qwen-turbo模型
                .defaultOptions(ChatOptions.builder().model("qwen-turbo").build())
                .build();
    }


    public String getPrompt(String question) {
        return Objects.requireNonNull(chatClient.prompt()
                .user(question)
                .call()
                .entity(QuestionType.class)).getValue();
//        TODO 之后可以考虑，只返回枚举，然后在chat实现类中，根据不同的类型，调整温度等参数


    }

}
