package alibaba.datafilter.controller;

import alibaba.datafilter.model.dto.DebateDTO;
import alibaba.datafilter.model.dto.StreamResponse;
import alibaba.datafilter.service.DebateService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/11/16
 * 说明:
 */
@RestController
@RequestMapping("/debate")
public class DebateController {
    private final DebateService debateService;

    public DebateController(DebateService debateService) {
        this.debateService = debateService;
    }

//    创建一个会话，然后开始辩论
    @GetMapping("/create")
    public ResponseEntity<String> createSession(@RequestParam @NotBlank(message = "主题不能为空！") String topic) {
        String conversationId = debateService.startDebate(topic);
        return ResponseEntity.ok(conversationId);
    }
    @PostMapping(value = "/speak",produces = "text/event-stream;charset=UTF-8")
    public ResponseEntity<Flux<StreamResponse>> speak(@RequestBody DebateDTO debateDTO){
        return  ResponseEntity.ok(debateService.debate(debateDTO));
    }

//    需要基于选择的模式，事已至此，先建表吧
    @PostMapping("/judge")
    public ResponseEntity<String> judgeSpeaks(@RequestParam @NotBlank(message ="会话id不能为空！") String conversationId,@RequestParam @NotBlank(message = "消息不能为空") String message){
        debateService.judgeSpeaks(conversationId,message);
        return ResponseEntity.ok("发送成功！");
    }

}
