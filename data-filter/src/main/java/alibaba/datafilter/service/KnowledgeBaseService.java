package alibaba.datafilter.service;

import alibaba.datafilter.model.dto.CreateCollectionDTO;
import alibaba.datafilter.model.dto.UploadFileConfigDTO;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/26
 * 说明:
 */
@Service
public interface KnowledgeBaseService {
    void insertText(String content, String collectionName);

    void importFilesToCollection( UploadFileConfigDTO uploadFileConfig);

    List<Document> searchSimilar(String query, int topK, String collectionName);

    void createCollection(CreateCollectionDTO createCollectionDTO);
    List<?> getCollection();
}
