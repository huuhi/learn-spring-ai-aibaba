package alibaba.chatmemory.controller;

import com.alibaba.cloud.ai.memory.jdbc.SQLiteChatMemoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/21
 * 说明:
 */
@RestController
@Slf4j
public class MemoryChatController {

    private final ChatClient chatClient;

    private final int maxMessages = 100;
//    设置 聊天内存仓库
    private final MessageWindowChatMemory messageWindowChatMemory;

    public MemoryChatController(ChatClient.Builder builder, SQLiteChatMemoryRepository sqliteChatMemoryRepository) {
        this.messageWindowChatMemory=MessageWindowChatMemory.builder()
                .chatMemoryRepository(sqliteChatMemoryRepository)
                .maxMessages(maxMessages)
                .build();
        this.chatClient = builder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(messageWindowChatMemory)
                                .build()
                ).build();
    }
    @GetMapping("/call")
    public Flux<String>  call(@RequestParam(defaultValue = "你好，我叫小胡") String query,
                              @RequestParam(defaultValue = "xiaohu")String conversationId){
        return chatClient.prompt(query)
                .advisors(
                        a->a.param(CONVERSATION_ID,conversationId)
                ).stream().content();
    }

    @GetMapping("/messages")
    public List<Message> messages(@RequestParam(defaultValue = "xiaohu")String conversationId){
        return messageWindowChatMemory.get(conversationId);
    }
    @GetMapping("/media")
    public Flux<String> media(@RequestParam(defaultValue = "xiaohu")String conversationId){
        return chatClient.prompt()
                .user(u->u.text("Explain what do you see on this picture?"))
                
                .advisors(
                        a->a.param(CONVERSATION_ID,conversationId)
                ).stream().content();
    }

}
