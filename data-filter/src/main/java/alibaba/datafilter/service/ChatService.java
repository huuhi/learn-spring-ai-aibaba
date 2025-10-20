package alibaba.datafilter.service;

import alibaba.datafilter.model.domain.ResearchQuestionDTO;
import alibaba.datafilter.model.dto.QuestionDTO;
import alibaba.datafilter.model.dto.RequestDTO;
import alibaba.datafilter.model.dto.StreamResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/25
 * 说明:
 */
@Service
public interface ChatService {



//    聊天
    Flux<StreamResponse> chat(RequestDTO requestDTO);

    Flux<StreamResponse> dataFilterSearch(String query, String conversationId);

    ResponseEntity<List<?>> developPlan(QuestionDTO question);


    Flux<StreamResponse> research(ResearchQuestionDTO researchQuestionDTO);
}
