package alibaba.datafilter.tools;


import alibaba.datafilter.model.domain.Collection;
import alibaba.datafilter.model.dto.RagSearchConfigDTO;
import alibaba.datafilter.service.CollectionService;
import alibaba.datafilter.service.UserService;
import alibaba.datafilter.utils.RagUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/20
 * 说明: 知识库工具
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagTool {
    private final  CollectionService collectionService;
    private final UserService userService;
    private final Function<String, MilvusVectorStore> vectorStoreFactory;
    private final ChatClient.Builder chatClientBuilder;



    @Tool(name="get_collection_list",description = "根据用户ID获取知识库基本信息")
    public String getCollectionList(@ToolParam(description = "用户的id") Integer userId){
        if (userId==null||userService.getById(userId)==null){
            return "用户ID为空或用户不存在！";
        }
        List<Collection> list = collectionService.lambdaQuery()
                .eq(Collection::getUserId, userId)
                .or()
                .eq(Collection::getIsSystem, true)
                .list();
        return list.stream().map(collection -> "知识库名称："+collection.getCollectionName() + "知识库介绍:" + collection.getDescription()+"知识库语言:"+collection.getLanguage()).toList().toString();
    }


    @Tool(name = "rag_search", description = "按照传递的问题和参数执行知识库检索")
    public String ragSearch(@ToolParam(description = "知识库名称") String collectionName,
                            @ToolParam(description = "用户的问题，最好与知识库的语言一样") String query,
                            @ToolParam(description = "知识库检索参数" ,required = false) RagSearchConfigDTO ragSearchConfigDTO) {
        Collection collection = collectionService.isContains(collectionName);
        if(collection==null){
            log.warn("知识库不存在");
            return "错误：指定的知识库 '" + collectionName + "' 不存在。请先调用 get_collection_list 获取正确的知识库列表。";
        }
        MilvusVectorStore vectorStore = vectorStoreFactory.apply(RagUtils.getCollectionName(collection.getUserId(),collectionName));
        TranslationQueryTransformer queryTransformer = TranslationQueryTransformer.builder().chatClientBuilder(chatClientBuilder).targetLanguage(collection.getLanguage()).build();
        Query q = Query.builder().text(query).build();
//        将用户问题转换为知识库语言
        Query transform = queryTransformer.transform(q);
        VectorStoreDocumentRetriever vectorStoreDocumentRetriever = new VectorStoreDocumentRetriever(vectorStore, ragSearchConfigDTO.getScore(), ragSearchConfigDTO.getTopK().intValue(), null);
        List<Document> documents = vectorStoreDocumentRetriever.retrieve(transform);
        if(documents.isEmpty()){
            return "在知识库 " + collectionName + "中没有找到与问题相关的内容。";
        }
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("从知识库中检索到一下内容：");
        for (Document document : documents) {
            contextBuilder.append("来源描述:").append(document.getMetadata().getOrDefault("source_description","N/A")).append("\n");
            contextBuilder.append("内容:").append(document.getText()).append("\n\n");
        }
        return contextBuilder.toString();
    }
}