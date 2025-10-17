package alibaba.ragpgvector.service.impl;

import alibaba.ragpgvector.service.KnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/21
 * 说明:
 */
@Slf4j
@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    /**
     * VectorStore：存储向量
     * ChatClient：聊天AI客户端
     */
    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    @Autowired
    public KnowledgeBaseServiceImpl(@Qualifier("openAiChatModel") ChatModel chatModel, VectorStore vectorStore) {
        this.vectorStore = vectorStore;
//      创建AI聊天客户端，使用OpenAiChatModel
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultOptions(OpenAiChatOptions.builder().temperature(0.8).build())
                .build();

    }

    @Override
    public void insertText(String content) {
        Assert.hasText(content,"文本内容不能为空");

        log.info("插入文本到向量存储：文本长度：{}",content.length());

        Document document = new Document(content);
        // 使用TokenTextSplitter进行文本分割，控制每个片段的token数量
        TokenTextSplitter textSplitter = new TokenTextSplitter();
        List<Document> splitDocuments = textSplitter.apply(List.of(document));
        vectorStore.add(splitDocuments);
        log.info("文本插入成功。文档片段数：{}",splitDocuments.size());
    }

    @Override
    public String loadFileByType(MultipartFile[] files) {
        if(files==null||files.length==0){
            return "上传文件为空！";
        }
        int errorCount = 0;
        int successCount = 0;

        for (MultipartFile file:files){
            if (processingType(file)){
                successCount++;
            }else{
                errorCount++;
            }
        }
        //????
        return successCount+"个文件处理成功，失败"+errorCount+"个文件处理失败";
    }

    @Override
    public List<Document> searchSimilar(String query, int topK) {
        Assert.hasText(query,"查询不能为空");
        log.info("开始搜索相似文本：query:{},topK:{}",query,topK);
        SearchRequest searchRequest = SearchRequest.builder().query(query).topK(topK).build();

        List<Document> results = vectorStore.similaritySearch(searchRequest);

        log.info("相似性搜索完成，找到 {} 个相关文档", results.size());
        return results;

    }

    @Override
    public String chatWithKnowledge(String query, int topK) {
        Assert.hasText(query,"查询问题不能为空");
        log.info("开始知识库对话，查询: '{}'", query);
        // 限制检索结果数量和总长度，避免token超限
        List<Document> relevantDocs = searchSimilar(query, Math.min(topK, 5));

        if(relevantDocs.isEmpty()) {
            log.warn("未找到与查询相关的文档");
            return "没有找到与问题相关的内容。请重新提问。";
        }
        
        // 控制上下文总长度，避免超过token限制
        StringBuilder contextBuilder = new StringBuilder();
        int totalChars = 0;
        for (Document doc : relevantDocs) {
            String docText = doc.getText();
            // 如果加上当前文档会超过字符限制，则停止添加
            if (totalChars + docText.length() > 15000) { // 限制总字符数
                log.info("为避免token超限，截断部分文档内容");
                break;
            }
            log.info("知识库内容：{}", docText);
            contextBuilder.append(docText).append("\n\n");
            totalChars += docText.length();
        }
        
        String context = contextBuilder.toString();
        String prompt = String.format("基于以下知识库内容回答用户问题。如果知识库内容无法回答问题，请明确说明。\n\n" + "知识库内容：\n%s\n\n" + "用户问题：%s\n\n" + "请基于上述知识库内容给出准确、有用的回答：", context, query);

        return chatClient.prompt(prompt).call().content();
    }

    @Override
    public Flux<String> streamChatWithKnowledge(String query, int topK) {
        Assert.hasText(query, "查询问题不能为空");
        log.info("开始流式知识库对话，查询: '{}'", query);

        try {
            // 限制检索结果数量和总长度，避免token超限
            List<Document> relevantDocs = searchSimilar(query, Math.min(topK, 5));

            if (relevantDocs.isEmpty()) {
                log.warn("未找到与查询相关的文档");
                return Flux.just("抱歉，我在知识库中没有找到相关信息来回答您的问题。");
            }

            // 控制上下文总长度，避免超过token限制
            StringBuilder contextBuilder = new StringBuilder();
            int totalChars = 0;
            for (Document doc : relevantDocs) {
                String docText = doc.getText();
                // 如果加上当前文档会超过字符限制，则停止添加
                if (totalChars + docText.length() > 15000) { // 限制总字符数
                    log.info("为避免token超限，截断部分文档内容");
                    break;
                }
                log.info("知识库内容：{}", docText);
                contextBuilder.append(docText).append("\n\n");
                totalChars += docText.length();
            }

            String context = contextBuilder.toString();

            // 构建提示词
            String prompt = String.format("基于以下知识库内容回答用户问题。如果知识库内容无法回答问题，请明确说明。\n\n" + "知识库内容：\n%s\n\n" + "用户问题：%s\n\n" + "请基于上述知识库内容给出准确、有用的回答：", context, query);

            // 调用LLM生成流式回答
            return chatClient.prompt(prompt).stream().content();

        } catch (Exception e) {
            log.error("流式知识库对话失败，查询: '{}'", query, e);
            return Flux.just("对话过程中发生错误: " + e.getMessage());
        }
    }

//        处理文件保存到向量数据库中
private Boolean processingType(MultipartFile file) {
    Assert.notNull(file, "文件为空！");
    log.info("开始处理文件：fileName:{},fileSize:{}", file.getOriginalFilename(), file.getSize());

    try {
        // 创建临时文件
        Path tempFile = Files.createTempFile("upload_", "-" + file.getOriginalFilename());
        Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

        List<Document> documents;
        String fileName = file.getOriginalFilename();

        if (fileName.toLowerCase().endsWith(".pdf")) {
            // 使用pdf读取器
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(tempFile.toUri().toString());
            documents = pdfReader.get();
            log.info("使用PD处理文件：{}", fileName);
        } else {
            // 使用Tika处理其他类型文件
            TikaDocumentReader tikaReader = new TikaDocumentReader(tempFile.toUri().toString());
            documents = tikaReader.get();
            log.info("使用tika处理文件：{}", fileName);
        }
        
        // 对文档进行分块处理，避免单个文档过大导致token超限
        TokenTextSplitter textSplitter = new TokenTextSplitter();
        List<Document> splitDocuments = new ArrayList<>();
        for (Document document : documents) {
            List<Document> splits = textSplitter.apply(List.of(document));
            splitDocuments.addAll(splits);
        }
        vectorStore.add(splitDocuments);
        Files.deleteIfExists(tempFile);
        log.info("文件处理完毕：fileName:{},原始文档数:{},分割后文档数:{}",
                fileName, documents.size(), splitDocuments.size());
        return true;

    } catch (IOException e) {
        log.error("文件处理失败：fileName:{},error:{}", file.getOriginalFilename(), e.getMessage());
        return false;
    }
}


}
