package alibaba.datafilter.utils;

import alibaba.datafilter.common.utils.MilvusVectorStoreUtils;
import alibaba.datafilter.model.domain.Collection;
import alibaba.datafilter.model.dto.RagSearchConfigDTO;
import com.github.houbb.opencc4j.util.ZhConverterUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

import static alibaba.datafilter.common.content.LanguageContent.CHINESE;
import static alibaba.datafilter.common.content.LanguageContent.CHINESE_TW;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/20
 * 说明:
 */
@Component
@Slf4j
public class RagUtils {
    private final MilvusVectorStoreUtils milvusVectorStoreUtils;
    private final Function<String, MilvusVectorStore> dynamicVectorStoreFactory;

    public RagUtils(MilvusVectorStoreUtils milvusVectorStoreUtils, Function<String, MilvusVectorStore> dynamicVectorStoreFactory) {
        this.milvusVectorStoreUtils = milvusVectorStoreUtils;
        this.dynamicVectorStoreFactory = dynamicVectorStoreFactory;
    }


    public String ragSearch(String query, String collectionName,RagSearchConfigDTO ragSearchConfigDTO){
//        TODO 判断知识库是否存在，不存在直接返回""
        Collection collection = milvusVectorStoreUtils.isValidCollectionName(collectionName);
        if(collection==null){
            log.info("知识库不存在啦！");
            return "";
        }
//        判断语言，判断知识库是否与问题为同一种语言，目前只支持简中和繁中
        String language = collection.getLanguage();
        if(language.equals(CHINESE_TW)){
//            需要将问题转换为繁体字
//            先判断是否为简体
            if(ZhConverterUtil.isSimple(query)){
                query = ZhConverterUtil.toTraditional(query);
                log.info("知识库为繁体字，将问题转换为繁体字：{}",query);
            }
        }
//        如果存在则检索，返回
        MilvusVectorStore vectorStore = dynamicVectorStoreFactory.apply(collectionName);

        SearchRequest searchRequest= SearchRequest.builder().query(query).topK(5).build();
        double similarityThreshold = 0.6;
        double scoreThreshold = 0.4;
        StringBuilder stringBuilder = new StringBuilder();
        if(ragSearchConfigDTO!=null){
            searchRequest= SearchRequest.builder().query(query).topK(ragSearchConfigDTO.getTopK()).build();

            similarityThreshold=ragSearchConfigDTO.getInstance();
            scoreThreshold= ragSearchConfigDTO.getScore();
        }
        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        // 设置相似度阈值，只返回相似度高于此值的文档
        for (Document doc : documents) {
            // 获取文档的相似度分数

//           0-1 越高越相似，建议：0.5及以上
            assert doc.getScore() != null;
            double score = doc.getScore();
//            越低越好 0-2 建议：小于 0.5以下
            double similarityScore = Double.parseDouble(doc.getMetadata().get("distance").toString());
            // 只有当相似度分数大于等于阈值时才添加到结果中
            if (similarityScore <= similarityThreshold||score>=scoreThreshold) {
                assert doc.getText() != null;
                log.info("距离：{}，分数：{} 添加文档: {}", score,similarityScore,doc.getText().substring(0, 20));
                stringBuilder.append(doc.getMetadata().getOrDefault("source_description",""))
                        .append(doc.getText())
                        .append("\n\n");
            }
        }
        return stringBuilder.toString();
    }

    /**
     * 将文档的语言转换成知识库的语言(目前只支持简中繁中互转)
     * @param documents  文档
     * @param language 知识库的语言
     * @return 转换后的文档
     */
    public List<Document> transfer(List<Document> documents,String language){
//        先判断是否为null,防止空指针异常
        if (documents!=null) {
//            如果前20个字符是简中并且目标语言是中文繁体，则进行转换
            if (ZhConverterUtil.isSimple(documents.get(0).getText()) && language.equals(CHINESE_TW)) {
                return documents.stream().map(document -> {
                    String text = document.getText();
                    text = ZhConverterUtil.toTraditional(text);
                    return new Document(document.getId(), text, document.getMetadata());
                }).toList();
//                如果前20个字符是中文繁体并且目标语言是简体中文，则进行转换
            } else if (ZhConverterUtil.isTraditional(documents.get(0).getText()) && language.equals(CHINESE)) {
                return documents.stream().map(document -> {
                    String text = document.getText();
                    text = ZhConverterUtil.toSimple(text);
                    return new Document(document.getId(), text, document.getMetadata());
                }).toList();
            }
//            TODO 更多语言转换逻辑
            return documents;
        }
        return List.of();
    }
}
