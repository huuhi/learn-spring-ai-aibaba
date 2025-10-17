package alibaba.datafilter.service.impl;

import alibaba.datafilter.common.concurrent.UserHolder;
import alibaba.datafilter.model.domain.Collection;
import alibaba.datafilter.model.dto.UserDTO;
import alibaba.datafilter.service.CollectionService;
import alibaba.datafilter.service.KnowledgeBaseService;
import alibaba.datafilter.common.utils.MilvusVectorStoreUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Function;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/26
 * 说明:
 */
@Service
@Slf4j
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {
    @Resource
    private Function<String, MilvusVectorStore> dynamicVectorStoreFactory;
    @Resource
    private MilvusVectorStoreUtils milvusVectorStoreUtils;
    @Resource
    private CollectionService collectionService;


    @Override
    public Boolean  insertText(String content, String collectionName) {
        if(!milvusVectorStoreUtils.isValidCollectionName(collectionName)){
            log.warn("不存在的知识库:{}",collectionName);
            return false;
        }

        MilvusVectorStore vectorStore = dynamicVectorStoreFactory.apply(collectionName);
        // 添加一个空文档以确保collection和索引被正确初始化
        try {
            vectorStore.add(Collections.emptyList());
        } catch (Exception e) {
            // 如果初始化失败，尝试手动创建索引
            milvusVectorStoreUtils.createIndexForCollection(collectionName);
        }
        Document document = new Document(content);
        // 使用TokenTextSplitter进行文本分割，控制每个片段的token数量
        TokenTextSplitter textSplitter = new TokenTextSplitter();
        List<Document> splitDocuments = textSplitter.apply(List.of(document));


        vectorStore.add(splitDocuments);
        return true;
    }



    @Override
    public String loadFileByType(MultipartFile[] files, String collectionName, String sourceDescription) {
        int successes = 0;
        int failures = 0;
        MilvusVectorStore vectorStore = dynamicVectorStoreFactory.apply(collectionName);
        try {
            vectorStore.add(Collections.emptyList());
        } catch (Exception e) {
            // 如果初始化失败，尝试手动创建索引
            milvusVectorStoreUtils.createIndexForCollection(collectionName);
        }
        for (MultipartFile file:files){
            if(processingType(file,vectorStore,sourceDescription)){
                successes++;
            }else failures++;
        }
        return "成功处理"+successes+"个文件，失败"+failures+"个文件";
    }

    @Override
    public List<Document> searchSimilar(String query, int topK, String collectionName) {
        boolean validCollectionName = milvusVectorStoreUtils.isValidCollectionName(collectionName);
        if(!validCollectionName){
            log.warn("不存在的知识库:{}",collectionName);
            return new ArrayList<>();
        }
        MilvusVectorStore vectorStore = dynamicVectorStoreFactory.apply(collectionName);
        Assert.hasText(query,"查询不能为空");
        log.info("开始搜索相似文本：query:{},topK:{}",query,topK);
        SearchRequest searchRequest = SearchRequest.builder().query(query).topK(topK).build();

        List<Document> results = vectorStore.similaritySearch(searchRequest);

        assert results != null;
        log.info("相似性搜索完成，找到 {} 个相关文档", results.size());
        return results;
    }

    @Override
    public ResponseEntity<String> createCollection(String collectionName, String description, Boolean isSystem) {
        UserHolder.saveUser(new UserDTO(1078833153,"小小怪cC087z"));
        //        TODO 之后要把用户ID修改为真实的用户ID
        UserDTO user = UserHolder.getUser();
        if (user==null){
            log.warn("用户未登录:{}", user);
            return ResponseEntity.status(500).body("用户未登录");
        }
//        查看是否是系统知识库
        if(isSystem){
//            查看是否是系统管理员
            if(!user.getId().equals(1078833153)){
                return ResponseEntity.badRequest().body("权限不足！");
            }
        }
//        获取当前用户
//        需要查看用户的知识库数量，如果>=10,不允许创建新的知识库
        int count = collectionService
                .query()
                .eq("user_id", user.getId())
                .count().intValue();
        if(count>=10){
            log.warn("用户已创建10个知识库，不允许创建新的知识库");
            return ResponseEntity.status(500).body("用户已创建10个知识库，不允许创建新的知识库");
        }
        Collection.CollectionBuilder collectionBuilder = Collection.builder().name(collectionName).description(description).userId(user.getId());
        if (isSystem){
            collectionBuilder.isSystem(true);
        }
        boolean save = collectionService.save(collectionBuilder.build());
        if(!save){
            log.warn("创建知识库失败");
            return  ResponseEntity.status(500).body("创建知识库失败");
        }
//        这里直接创建知识库
        milvusVectorStoreUtils.createIndexForCollection(collectionName);
        return ResponseEntity.ok("创建成功");
    }

    private Boolean processingType(MultipartFile file, MilvusVectorStore vectorStore,String sourceDescription) {
        Assert.notNull(file, "文件为空！");
        log.info("开始处理文件：fileName:{},fileSize:{}", file.getOriginalFilename(), file.getSize());

        try {
            // 创建临时文件
            Path tempFile = Files.createTempFile("upload_", "-" + file.getOriginalFilename());
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            List<Document> documents;
            String fileName = file.getOriginalFilename();

            assert fileName != null;
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
                for (Document split : splits) {
                    split.getMetadata();
                    Map<String, Object> metadata = split.getMetadata();
                    Map<String, Object> newMetadata = new HashMap<>(metadata);
                    newMetadata.put("source_description", sourceDescription);
                    newMetadata.put("file_name", fileName);

                    Document enrichedDoc = new Document(
                            Objects.requireNonNull(split.getText()),
                            newMetadata
                    );
                    splitDocuments.add(enrichedDoc);
                }


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
