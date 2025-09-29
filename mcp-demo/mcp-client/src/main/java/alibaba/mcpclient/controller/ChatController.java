package alibaba.mcpclient.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/28
 * 说明:
 */
@RestController
@RequiredArgsConstructor
public class ChatController {
    private final ChatClient chatClient;

    @GetMapping("/chat")
    public Flux<String> chat(@RequestParam String question) {
        return chatClient.prompt(question)
                .stream()
                .content();
    }

}
