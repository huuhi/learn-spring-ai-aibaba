package alibaba.datafilter.controller;


import alibaba.datafilter.model.domain.ResearchPlanStep;
import alibaba.datafilter.model.domain.ResearchQuestionDTO;
import alibaba.datafilter.model.dto.QuestionDTO;
import alibaba.datafilter.model.dto.RequestDTO;
import alibaba.datafilter.model.dto.StreamResponse;
import alibaba.datafilter.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.validation.Valid;
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
    private final ChatService chatService;

//    垃圾接口

    // 1. 修改返回类型为 Flux<String>，并指定 produces 为流式类型
    // 1. 修改 produces，强制使用 UTF-8
    @PostMapping(value = "/chat-stream", produces = "text/event-stream;charset=UTF-8")
    public ResponseEntity<Flux<StreamResponse>> chatStream(@RequestBody @Valid RequestDTO requestDTO) {
        return ResponseEntity.ok(chatService.chat(requestDTO));
    }
//    创建一个会话
//    @GetMapping("/conversation")
//    public ResponseEntity<String> createConversation() {
//        return chatService.createConversation();
//    }
//    获取聊天记录  ->移到会话控制器 中
    @PostMapping(value="/data-filter",produces = "text/event-stream;charset=UTF-8")
    public ResponseEntity<Flux<StreamResponse>> dataFilter(@RequestBody @Valid QuestionDTO questionDTO) {
        return ResponseEntity.ok(chatService.dataFilterSearch(questionDTO.getQuestion(), questionDTO.getConversationId()));
    }
    @GetMapping("/develop-plan")
    public ResponseEntity<List<ResearchPlanStep>> developPlan(@RequestBody @Valid QuestionDTO question) {
        List<ResearchPlanStep> researchPlanSteps = chatService.developPlan(question);
        return ResponseEntity.ok(researchPlanSteps);
    }
//    开始研究！
    @PostMapping(value = "/research",produces = "text/event-stream;charset=UTF-8")
    public ResponseEntity<Flux<StreamResponse>> research(@RequestBody @Valid ResearchQuestionDTO researchQuestionDTO) {
        return ResponseEntity.ok(chatService.research(researchQuestionDTO));
    }
}
