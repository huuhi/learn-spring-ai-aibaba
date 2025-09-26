package alibaba.ragalibabademo.service;


import com.alibaba.cloud.ai.advisor.DocumentRetrievalAdvisor;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/23
 * 说明:
 */
@Service
@Slf4j
public class CloudRagService implements RagService{
    private static final String indexName="test";
    private final ChatClient chatClient;
    private final DashScopeApi dashScopeApi;
    private final VectorStore vectorStore;
    private static final String retrievalSystemTemplate = """
			Context information is below.
			---------------------
			{question_answer_context}
			---------------------
			Given the context and provided history information and not prior knowledge,
			reply to the user comment. If the answer is not in the context, inform
			the user that you can't answer the question.
			""";
    public CloudRagService(ChatClient.Builder builder, DashScopeApi dashScopeApi) {
        DashScopeDocumentRetriever retriever = new DashScopeDocumentRetriever(dashScopeApi,
                DashScopeDocumentRetrieverOptions.builder().withIndexName(indexName).build());
        this.dashScopeApi=dashScopeApi;
        this.vectorStore = new DashScopeCloudStore(this.dashScopeApi, new DashScopeStoreOptions(indexName));
        this.chatClient=builder
                .defaultAdvisors(
                        new DocumentRetrievalAdvisor(retriever,new SystemPromptTemplate(retrievalSystemTemplate))
                ).build();
    }

    @Override
    public String importDocuments(MultipartFile[] files) {
        StringBuilder result = new StringBuilder();
        for (MultipartFile file : files) {
            Path tempFilePath = null;
            try {
                // 检查文件是否为空
                if (file.isEmpty()) {
                    String errorMsg = "文件为空: " + file.getOriginalFilename();
                    result.append(errorMsg).append("\n");
                    log.warn(errorMsg);
                    continue;
                }
                
                // 检查文件格式是否支持
                String originalFilename = file.getOriginalFilename();

                // 创建临时文件，使用固定前缀和原始文件扩展名
                String fileExtension = getFileExtension(originalFilename);
                tempFilePath = Files.createTempFile("upload_", fileExtension);
                
                // 将上传的文件内容写入临时文件
                file.transferTo(tempFilePath);
                
                // 验证文件是否存在且可读
                if (!Files.exists(tempFilePath) || !Files.isReadable(tempFilePath)) {
                    String errorMsg = "文件创建失败或不可读: " + originalFilename;
                    result.append(errorMsg).append("\n");
                    log.warn(errorMsg);
                    continue;
                }
                
                // 检查文件大小
                long fileSize = Files.size(tempFilePath);
                if (fileSize == 0) {
                    String errorMsg = "文件大小为0: " + originalFilename;
                    result.append(errorMsg).append("\n");
                    log.warn(errorMsg);
                    continue;
                }
                
                log.info("处理文件: {}, 大小: {} bytes, 临时路径: {}", 
                    originalFilename, fileSize, tempFilePath);
                
                // 1. import and split documents
                DocumentReader reader = new DashScopeDocumentCloudReader(
                    tempFilePath.toString(), dashScopeApi, null);
                List<Document> documentList = reader.get();
                
                if (documentList.isEmpty()) {
                    String errorMsg = "未能从文件中读取到内容: " + originalFilename;
                    result.append(errorMsg).append("\n");
                    log.warn(errorMsg);
                    continue;
                }
                
                log.info("{} documents loaded and split", documentList.size());

                // 2. add documents to DashScope cloud storage
                vectorStore.add(documentList);
                
                // 等待文档处理完成
//                try {
//                    TimeUnit.SECONDS.sleep(3);
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                }
                
                result.append("成功导入文件: ").append(originalFilename)
                    .append("，文档数量: ").append(documentList.size()).append("\n");
                log.info("{} documents added to dashscope cloud vector store", documentList.size());
                
            } catch (com.alibaba.cloud.ai.dashscope.common.DashScopeException e) {
                String errorMsg = "DashScope处理文件失败: " + file.getOriginalFilename() + " 错误: " + e.getMessage();
                result.append(errorMsg).append("\n");
                log.error(errorMsg, e);
                
                // 提供更具体的错误信息和建议
                if (e.getMessage().contains("ParseFileError")) {
                    result.append("  建议检查文件格式和内容是否正确，或尝试使用PDF格式文件\n");
                }
            } catch (Exception e) {
                String errorMsg = "导入文件失败: " + file.getOriginalFilename() + " 错误: " + e.getMessage();
                result.append(errorMsg).append("\n");
                log.error(errorMsg, e);
            } finally {
                // 清理临时文件
                if (tempFilePath != null && Files.exists(tempFilePath)) {
                    try {
                        Files.delete(tempFilePath);
                    } catch (IOException e) {
                        log.warn("Failed to delete temporary file: {}", tempFilePath, e);
                    }
                }
            }
        }
        return result.toString();
    }

    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
    
    @Override
    public Flux<ChatResponse> retrieve(String query) {
        return chatClient.prompt(query).stream().chatResponse();
    }
}
