package alibaba.datafilter.config;

import com.alibaba.cloud.ai.memory.jdbc.SQLiteChatMemoryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/25
 * 说明:
 */
@Configuration
public class ChatClientConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder, SQLiteChatMemoryRepository sqliteChatMemoryRepository) {
        int maxMessage = 100;
        MessageWindowChatMemory messageWindowChatMemory = MessageWindowChatMemory.builder()
                .maxMessages(maxMessage)
                .chatMemoryRepository(sqliteChatMemoryRepository)
                .build();

//        添加记忆功能，默认保存到 sqlite数据库
        return builder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(messageWindowChatMemory)
                                .build()
                )
                .build();
    }
    @Bean
    MessageWindowChatMemory messageWindowChatMemory(SQLiteChatMemoryRepository sqliteChatMemoryRepository) {
        int maxMessage = 100;
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(sqliteChatMemoryRepository)
                .maxMessages(maxMessage)
                .build();
    }
}
