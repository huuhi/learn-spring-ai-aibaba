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
    private static final String prefix="ID_";
    public RagUtils(MilvusVectorStoreUtils milvusVectorStoreUtils, Function<String, MilvusVectorStore> dynamicVectorStoreFactory) {
        this.milvusVectorStoreUtils = milvusVectorStoreUtils;
        this.dynamicVectorStoreFactory = dynamicVectorStoreFactory;
    }

    public static String getCollectionName(Integer userId,String collectionName){
        return prefix+userId+"_"+collectionName;
    }

    public MilvusVectorStore getVectorStore(String collectionName) {
        return dynamicVectorStoreFactory.apply(collectionName);
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
        double scoreThreshold = 0.5;
        StringBuilder stringBuilder = new StringBuilder();
//        MultiQueryExpander.builder()
        if(ragSearchConfigDTO!=null){
            searchRequest= SearchRequest.builder().query(query).topK(ragSearchConfigDTO.getTopK()).build();

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
            // 只有当相似度分数大于等于阈值时才添加到结果中
            if (score>=scoreThreshold) {
                assert doc.getText() != null;
                log.info("分数：{} 添加文档: {}", score,doc.getText().substring(0, 20));
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
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
//            如果前20个字符是简中并且目标语言是中文繁体，则进行转换
        if (language.equals(CHINESE_TW)) {
            log.info("知识库为中文繁体字，查看是否需要转换");
            return documents.stream().map(document -> {
                String text = document.getText();
                if (text!=null&&isSimpleChinese(text)) {
                    log.info("文档为简体中文，将文档转换为繁体中文");
                    text = ZhConverterUtil.toTraditional(text);
                }
                assert text != null;
                return new Document(document.getId(), text, document.getMetadata());
            }).toList();
//                如果前20个字符是中文繁体并且目标语言是简体中文，则进行转换
        } else if (language.equals(CHINESE)) {
            log.info("知识库为中文简体字，查看是否需要转换");
            return documents.stream().map(document -> {
                String text = document.getText();
                if (text!=null&&isTraditionalChinese(text)) {
                    log.info("文档为中文繁体字，将文档转换为简体中文：{}",text);
                }
                assert text != null;
                return new Document(document.getId(), text, document.getMetadata());
            }).toList();
        }
//            TODO 更多语言转换逻辑
        return documents;
    }


    /**
     * 判断文本是否为简体中文（预处理文本后再判断）
     * @param text 原始文本
     * @return 是否为简体中文
     */
    private boolean isSimpleChinese(String text) {
        // 提取前100个非空白字符进行判断，避免换行符等影响
        String processedText = extractChineseCharacters(text);
        log.info("判断文本是否为简体中文：{}",processedText);
        return !processedText.isEmpty() && ZhConverterUtil.isSimple(processedText);
    }

    /**
     * 判断文本是否为繁体中文（预处理文本后再判断）
     * @param text 原始文本
     * @return 是否为繁体中文
     */
    private boolean isTraditionalChinese(String text) {
        // 提取前100个非空白字符进行判断，避免换行符等影响
        String processedText = extractChineseCharacters(text);
        log.info("判断文本是否为繁体中文,{}",processedText);
        return !processedText.isEmpty() && ZhConverterUtil.isTraditional(processedText);
    }

    /**
     * 从文本中提取指定数量的中文字符，忽略空白字符和非中文字符
     *
     * @param text 原始文本
     * @return 提取的中文字符
     */
    private String extractChineseCharacters(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length() && sb.length() < 50; i++) {
            char c = text.charAt(i);
            // 判断是否为中文字符（基本汉字范围）
            if (c >= 0x4E00 && c <= 0x9FFF) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

}
