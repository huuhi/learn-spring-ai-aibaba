package alibaba.ragalibabademo.service;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/23
 * 说明:
 */
public interface RagService {
    Flux<ChatResponse> retrieve(String query);

    String importDocuments(MultipartFile[] files);
}
