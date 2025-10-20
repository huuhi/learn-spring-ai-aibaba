package alibaba.datafilter.controller;

import alibaba.datafilter.model.vo.ConversationVO;
import alibaba.datafilter.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/20
 * 说明:
 */
@RestController
@RequestMapping("/conversation")
@RequiredArgsConstructor
public class ConversationController {
    private final MessageWindowChatMemory messageWindowChatMemory;
    private final ConversationService conversationService;


    @GetMapping("/get-list")
    public ResponseEntity<List<ConversationVO>> getList() {
        return conversationService.getListByUserId();
    }

    @GetMapping("/messages")
    public List<Message> messages(@RequestParam String conversationId) {
        return messageWindowChatMemory.get(conversationId);
    }

}
