package alibaba.datafilter.service.impl;

import alibaba.datafilter.common.concurrent.UserHolder;
import alibaba.datafilter.model.domain.Collection;
import alibaba.datafilter.model.domain.CollectionFiles;
import alibaba.datafilter.model.dto.CreateCollectionDTO;
import alibaba.datafilter.model.dto.UploadFileConfigDTO;
import alibaba.datafilter.model.dto.UserDTO;
import alibaba.datafilter.model.em.FileStatus;
import alibaba.datafilter.model.vo.FileVo;
import alibaba.datafilter.service.CollectionService;
import alibaba.datafilter.service.KnowledgeBaseService;
import alibaba.datafilter.common.utils.MilvusVectorStoreUtils;
import alibaba.datafilter.service.KnowledgeFileService;
import alibaba.datafilter.utils.RagUtils;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import java.util.*;
import java.util.function.Function;

import static alibaba.datafilter.common.content.LanguageContent.LANGUAGE_LIST;
import static alibaba.datafilter.common.content.RedisConstant.TEMP_USER_ID;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/26
 * 说明:
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {
    private final Function<String, MilvusVectorStore> dynamicVectorStoreFactory;
    private final  MilvusVectorStoreUtils milvusVectorStoreUtils;
    private final CollectionService collectionService;
    private final KnowledgeFileService knowledgeFileService;
    private final AsyncFileProcessingService asyncFileProcessingService;
    private final RagUtils ragUtils;
    @Override
    public Boolean  insertText(String content, String collectionName) {
        Collection collection = milvusVectorStoreUtils.isValidCollectionName(collectionName);
        if(collection==null){
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
        List<Document> documents = ragUtils.transfer(List.of(new Document(content)),collection.getLanguage());
        // 使用TokenTextSplitter进行文本分割，控制每个片段的token数量
        TokenTextSplitter textSplitter = new TokenTextSplitter();
        List<Document> splitDocuments = textSplitter.apply(documents);


        vectorStore.add(splitDocuments);
        return true;
    }



    @Override
    public String importFilesToCollection(UploadFileConfigDTO uploadFileConfig) {
        String collectionName = uploadFileConfig.getCollectionName();
        String sourceDescription = uploadFileConfig.getDescription();
//        这里应该确认用户是否有这个知识库
        Collection collection = milvusVectorStoreUtils.isValidCollectionName(collectionName);
        if(collection==null){
            log.warn("不存在的知识库:{}",collectionName);
            return "不存在的知识库:"+collectionName;
        }

        MilvusVectorStore vectorStore = dynamicVectorStoreFactory.apply(RagUtils.getCollectionName(TEMP_USER_ID,collectionName));
        try {
            vectorStore.add(Collections.emptyList());
        } catch (Exception e) {
            // 如果初始化失败，尝试手动创建索引
            milvusVectorStoreUtils.createIndexForCollection(collectionName);
        }
//        查找用户的文件
        List<FileVo> fileVos = knowledgeFileService.getFileListByIds(uploadFileConfig.getFileIds());
        if(fileVos==null||fileVos.isEmpty()){
            log.warn("没有找到文件");
            return "没有找到文件";
        }

        for (FileVo fileVo:fileVos){
//            获取已上传的文件，将其添加到指定的知识库当中！
            if (fileVo.getStatus().equals(FileStatus.COMPLETED)) {
                // 处理已完成上传的文件
                CollectionFiles.CollectionFilesBuilder collectionFilesBuilder = CollectionFiles.builder().fileId(Long.valueOf(fileVo.getId())).collectionId(collection.getId());
                asyncFileProcessingService.processingTypeFromOss(fileVo, vectorStore, sourceDescription, uploadFileConfig,collectionFilesBuilder,collection.getLanguage());


                //                if(processingTypeFromOss(fileVo, vectorStore, sourceDescription, uploadFileConfig)){
////                    如果处理成功，需要添加到关系表中
//                    collectionFiles = collectionFilesBuilder.build();
//                    successes++;
//                }else {
//                    collectionFiles = collectionFilesBuilder.status(FileStatus.FAILED).build();
//                    failures++;
//                }
            }
        }

        return "提交成功！";
    }

    @Override
    public List<Document> searchSimilar(String query, int topK, String collectionName) {
        Collection collection = milvusVectorStoreUtils.isValidCollectionName(collectionName);
        if(collection==null){
            log.warn("不存在的知识库:{}",collectionName);
            return new ArrayList<>();
        }
        MilvusVectorStore vectorStore = dynamicVectorStoreFactory.apply(RagUtils.getCollectionName(TEMP_USER_ID,collectionName));
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
            log.warn("用户未登录");
            return ResponseEntity.status(401).body("用户未登录");
        }
//        如果语言不支持
        if(language!=null&&!LANGUAGE_LIST.contains(language)){
            return ResponseEntity.badRequest().body("不支持的语言!");
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
            return ResponseEntity.status(400).body("用户已创建10个知识库，不允许创建新的知识库");
        }
//         这里知识库名称全局只允许存在一个，之后可以考虑怎么解决这个问题:解决办法：在知识库名称前面加用户的id
//        判断是否存在
        Collection collection = collectionService.lambdaQuery()
                .eq(Collection::getCollectionName, collectionName)
                .eq(Collection::getUserId, user.getId())
                .one();
        if(collection!=null){
            log.warn("知识库已存在:{}",collectionName);
            return ResponseEntity.status(400).body("知识库已存在");
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
//        这里直接创建知识库 前缀为用户的id
//        TODO 用户id需要从登录用户中获取
        milvusVectorStoreUtils.createIndexForCollection(RagUtils.getCollectionName(user.getId(),collectionName));
        return ResponseEntity.ok("创建成功");
    }

}
