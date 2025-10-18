package alibaba.datafilter.service.impl;

import alibaba.datafilter.common.concurrent.UserHolder;
import alibaba.datafilter.model.domain.Collection;
import alibaba.datafilter.model.dto.CreateCollectionDTO;
import alibaba.datafilter.model.dto.UploadFileConfigDTO;
import alibaba.datafilter.model.dto.UserDTO;
import alibaba.datafilter.service.CollectionService;
import alibaba.datafilter.service.KnowledgeBaseService;
import alibaba.datafilter.common.utils.MilvusVectorStoreUtils;
import alibaba.datafilter.utils.CharacterTextSplitter;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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

import static alibaba.datafilter.common.content.RedisConstant.TEMP_USER_ID;

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
        if(milvusVectorStoreUtils.isValidCollectionName(collectionName)==null){
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
    public String loadFileByType(MultipartFile[] files, String collectionName, String sourceDescription, UploadFileConfigDTO uploadFileConfig) {
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
            if(processingType(file,vectorStore,sourceDescription,uploadFileConfig)){
                successes++;
            }else failures++;
        }
        return "成功处理"+successes+"个文件，失败"+failures+"个文件";
    }

    @Override
    public List<Document> searchSimilar(String query, int topK, String collectionName) {
        Collection collection = milvusVectorStoreUtils.isValidCollectionName(collectionName);
        if(collection==null){
            log.warn("不存在的知识库:{}",collectionName);
            return new ArrayList<>();
        }
        MilvusVectorStore vectorStore = dynamicVectorStoreFactory.apply(collectionName);
        Assert.hasText(query,"查询不能为空");
        log.info("开始搜索相似文本：query:{},topK:{}",query,topK);
        SearchRequest searchRequest = SearchRequest.builder().query(query).topK(topK).build();


        List<Document> results = vectorStore.similaritySearch(searchRequest);

        log.info("相似性搜索完成，找到 {} 个相关文档", results.size());
        return results;
    }

    @Override
    public ResponseEntity<String> createCollection(CreateCollectionDTO createCollectionDTO) {
        boolean isSystem = createCollectionDTO.getIsSystem();
        String collectionName = createCollectionDTO.getCollectionName();
        String description = createCollectionDTO.getDescription();
        String language = createCollectionDTO.getLanguage();
        UserHolder.saveUser(new UserDTO(TEMP_USER_ID,"小小怪cC087z"));
        //        TODO 之后要把用户ID修改为真实的用户ID
        UserDTO user = UserHolder.getUser();
        if (user==null){
            log.warn("用户未登录:{}", user);
            return ResponseEntity.status(500).body("用户未登录");
        }
//        查看是否是系统知识库
        if(isSystem){
//            查看是否是系统管理员
            if(!user.getId().equals(TEMP_USER_ID)){
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
//        判断是否存在
        if(collectionService.isContains(collectionName)){
            log.warn("知识库已存在:{}",collectionName);
            return ResponseEntity.status(500).body("知识库已存在");
        }

        Collection.CollectionBuilder collectionBuilder = Collection.builder().collectionName(collectionName).description(description).language(language).userId(user.getId());
        if (isSystem){
            collectionBuilder.isSystem(true);
        }
        String name = createCollectionDTO.getName();
        if(!StrUtil.isBlank(name)){
            collectionBuilder.name(name);
        }else{
            collectionBuilder.name(collectionName);
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

    private Boolean processingType(MultipartFile file, MilvusVectorStore vectorStore,String sourceDescription,UploadFileConfigDTO uploadFileConfig) {
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
            // 2. 创建自定义的TextSplitter实例
            CharacterTextSplitter textSplitter = new CharacterTextSplitter(uploadFileConfig.getChunkSize(), uploadFileConfig.getChunkOverlap(), Arrays.stream(uploadFileConfig.getSeparators()).toList());

            // 3. 应用分块 (现在textSplitter.apply会返回已经处理好的所有小块)
            List<Document> splitDocuments = textSplitter.apply(documents);
            log.info("原始文档数:{},处理之后:{}", documents.size(), splitDocuments.size());

            log.debug("预览处理块：{}",splitDocuments.stream().limit(10).toList());

            List<Document> finalDocuments = getDocuments(sourceDescription, splitDocuments, fileName);
            vectorStore.add(finalDocuments);
            Files.deleteIfExists(tempFile);
            log.info("文件处理完毕：fileName:{},原始文档数:{},分割后文档数:{}",
                    fileName, documents.size(), finalDocuments.size());
            return true;

        } catch (IOException e) {
            log.error("文件处理失败：fileName:{},error:{}", file.getOriginalFilename(), e.getMessage());
            return false;
        }
    }

    @NotNull
    private static List<Document> getDocuments(String sourceDescription, List<Document> splitDocuments, String fileName) {
        List<Document> finalDocuments = new ArrayList<>();
        for (Document split : splitDocuments) {
            Map<String, Object> newMetadata = new HashMap<>(split.getMetadata());
            newMetadata.put("source_description", sourceDescription);
            newMetadata.put("file_name", fileName);
            Document enrichedDoc = new Document(
                    split.getId(),
                    Objects.requireNonNull(split.getText()),
                    newMetadata
            );
            finalDocuments.add(enrichedDoc);
        }
        return finalDocuments;
    }


}
