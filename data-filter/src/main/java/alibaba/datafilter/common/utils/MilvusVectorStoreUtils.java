package alibaba.datafilter.common.utils;

import alibaba.datafilter.model.domain.Collection;
import alibaba.datafilter.service.impl.CollectionServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/26
 * 说明: Milvus 工具类
 */
@Component
public class MilvusVectorStoreUtils {
    @Resource
    private final CollectionServiceImpl collectionService;

    public MilvusVectorStoreUtils(CollectionServiceImpl collectionService) {
        this.collectionService =  collectionService;
    }


    /**
     *
     * @param collectionName 集合名称
     * @return 是否有效
     */
    public  boolean isValidCollectionName(String collectionName) {
//        需要判断用户是否有这个知识库
        Collection one = collectionService.query()
                .eq("name", collectionName)
                .eq("id", 1)
                .one();
        return one != null;
    }

}
