package alibaba.datafilter.service;

import org.springframework.ai.document.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/26
 * 说明:
 */
@Service
public interface KnowledgeBaseService {
    Boolean insertText(String content, String collectionName);

    String loadFileByType(MultipartFile[] files, String collectionName, String description);

    List<Document> searchSimilar(String query, int topK, String collectionName);

    ResponseEntity<String> createCollection(String collectionName, String description, Boolean isSystem);
}
