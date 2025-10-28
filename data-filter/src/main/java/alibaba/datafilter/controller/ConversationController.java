package alibaba.datafilter.controller;

import alibaba.datafilter.model.vo.ConversationVO;
import alibaba.datafilter.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final ConversationService conversationService;


    @GetMapping("/get-list")
    public ResponseEntity<List<ConversationVO>> getList() {
        List<ConversationVO> list = conversationService.getListByUserId();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/messages")
    public ResponseEntity<List<Message>> messages(@RequestParam String conversationId) {
        try {
            List<Message> messages=conversationService.getMessageById(conversationId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(List.of());
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(@RequestParam String[] conversationIds) {
        if(conversationIds==null||conversationIds.length==0){
            return ResponseEntity.badRequest().body("非法会话id");
        }
        try {
            conversationService.deleteByIds(conversationIds);
            return ResponseEntity.ok("删除成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
