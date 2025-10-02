package alibaba.datafilter.service;

import alibaba.datafilter.model.dto.RequestDTO;
import alibaba.datafilter.model.dto.StreamResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/25
 * 说明:
 */
@Service
public interface ChatService {

//    创建会话
    ResponseEntity<String> createConversation();

//    聊天
    Flux<StreamResponse> chat(RequestDTO requestDTO);

    Flux<StreamResponse> dataFilterSearch(String query, String conversationId);
}
