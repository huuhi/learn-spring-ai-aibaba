package alibaba.datafilter.service;

import alibaba.datafilter.model.dto.DebateDTO;
import alibaba.datafilter.model.dto.StreamResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/11/16
 * 说明:
 */
@Service
public interface DebateService {
    String startDebate(String topic);

    Flux<StreamResponse> debate(DebateDTO debateDTO);

    void judgeSpeaks(String conversationId, String message);
}
