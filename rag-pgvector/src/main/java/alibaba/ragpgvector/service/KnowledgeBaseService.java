package alibaba.ragpgvector.service;

import org.springframework.ai.document.Document;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/21
 * 说明:AI知识库服务接口
 * 包括创建知识库，向知识库添加数据，向知识库查询数据
 */

public interface KnowledgeBaseService {

    /**
     *将字符串插入到知识库中
     */
    void insertText(String content);


    /**
     * 将文件插入到知识库中
     */
    String loadFileByType(MultipartFile[] file);

    /**
     * 搜索相似文档
     * @param query 查询字符串
     * @param topK 返回的相似文档数量
     * @return 相似文档列表
     */
    List<Document> searchSimilar(String query, int topK);

    /**
     *
     * @param query 用户查询的问题
     * @param topK 检索的相似文档数量
     * @return 返回LLM的回答
     */
    String chatWithKnowledge(String query,int topK);

    /**
     *
     * @param query 用户查询的问题
     * @param topK 检索的相似文档数量
     * @return 响应流
     */
    Flux<String> streamChatWithKnowledge(String query,int topK);

}
