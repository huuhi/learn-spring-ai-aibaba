package alibaba.datafilter.service;

import alibaba.datafilter.model.dto.CreateCollectionDTO;
import alibaba.datafilter.model.dto.UploadFileConfigDTO;
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

    String importFilesToCollection( UploadFileConfigDTO uploadFileConfig);

    List<Document> searchSimilar(String query, int topK, String collectionName);

    ResponseEntity<String> createCollection(CreateCollectionDTO createCollectionDTO);
}
