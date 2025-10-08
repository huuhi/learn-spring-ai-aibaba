package alibaba.datafilter.controller;


import alibaba.datafilter.model.domain.ResearchPlanStep;
import alibaba.datafilter.model.domain.ResearchQuestionDTO;
import alibaba.datafilter.model.dto.QuestionDTO;
import alibaba.datafilter.model.dto.RequestDTO;
import alibaba.datafilter.model.dto.StreamResponse;
import alibaba.datafilter.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/25
 * 说明:
 */
@RestController
@Slf4j
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {
    private final MessageWindowChatMemory messageWindowChatMemory;
    private final ChatService chatService;

//    垃圾接口

    // 1. 修改返回类型为 Flux<String>，并指定 produces 为流式类型
    // 1. 修改 produces，强制使用 UTF-8
    @PostMapping(value = "/chat-stream", produces = "text/event-stream;charset=UTF-8")
    public ResponseEntity<Flux<StreamResponse>> chatStream(@RequestBody RequestDTO requestDTO) {
        return ResponseEntity.ok(chatService.chat(requestDTO));
    }
//    创建一个会话
    @GetMapping("/conversation")
    public ResponseEntity<String> createConversation() {
        return chatService.createConversation();
    }
//    获取聊天记录
    @GetMapping("/messages")
    public List<Message> messages(@RequestParam String conversationId) {
        return messageWindowChatMemory.get(conversationId);
    }
    @PostMapping(value="/data-filter",produces = "text/event-stream;charset=UTF-8")
    public ResponseEntity<Flux<StreamResponse>> dataFilter(@RequestBody QuestionDTO questionDTO) {
        return ResponseEntity.ok(chatService.dataFilterSearch(questionDTO.getQuestion(), questionDTO.getQuestionId()));
    }
    @GetMapping("/develop-plan")
    public ResponseEntity<List<?>> developPlan(@RequestBody QuestionDTO question) {
        return chatService.developPlan(question);
    }
//    开始研究！
    @PostMapping("/research")
    public ResponseEntity<Flux<StreamResponse>> research(@RequestBody ResearchQuestionDTO researchQuestionDTO) {
        return ResponseEntity.ok(chatService.research(researchQuestionDTO));
    }
}
