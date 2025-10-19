package alibaba.datafilter.common.utils;

import alibaba.datafilter.model.domain.Collection;
import alibaba.datafilter.service.impl.CollectionServiceImpl;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.ShowCollectionsResponse;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.index.CreateIndexParam;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static alibaba.datafilter.common.content.RedisConstant.TEMP_USER_ID;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/26
 * 说明: Milvus 工具类
 */
@Component
@Slf4j
public class MilvusVectorStoreUtils {
    @Resource
    private final CollectionServiceImpl collectionService;
    @Value("${spring.ai.vectorstore.milvus.client.host}")
    private String milvusHost;

    @Value("${spring.ai.vectorstore.milvus.client.port}")
    private int milvusPort;

    @Value("${spring.ai.vectorstore.milvus.database-name:default}")
    private String databaseName;

    @Value("${spring.ai.vectorstore.milvus.embedding-dimension:1536}")
    private int embeddingDimension;

    public MilvusVectorStoreUtils(CollectionServiceImpl collectionService) {
        this.collectionService =  collectionService;
    }


    /**
     *
     * @param collectionName 集合名称
     * @return 是否有效
     */
    public  Collection isValidCollectionName(String collectionName) {
//        需要判断用户是否有这个知识库
        return collectionService.lambdaQuery()
                .eq(Collection::getCollectionName, collectionName)
                .eq(Collection::getUserId, TEMP_USER_ID)
                .one();
    }
    public void createIndexForCollection(String collectionName) {
        MilvusServiceClient milvusClient = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost(milvusHost)
                        .withPort(milvusPort)
                        .build()
        );

        // 检查集合是否已存在
        ShowCollectionsResponse showCollectionsResponse = milvusClient.showCollections(
                io.milvus.param.collection.ShowCollectionsParam.newBuilder()
                        .withDatabaseName(databaseName)
                        .build()
        ).getData();

        if (showCollectionsResponse.getCollectionNamesList().contains(collectionName)) {
            log.info("集合 {} 已存在，跳过创建", collectionName);
            return;
        }


        log.info("正在手动创建collection: {}", collectionName);

        // 定义字段
        FieldType docIdField = FieldType.newBuilder()
                .withName("doc_id")
                .withDataType(DataType.VarChar)
                .withPrimaryKey(true)
                .withMaxLength(65535)
                .build();

        FieldType embeddingField = FieldType.newBuilder()
                .withName("embedding")
                .withDataType(DataType.FloatVector)
                .withDimension(embeddingDimension)
                .build();

        FieldType contentField = FieldType.newBuilder()
                .withName("content")
                .withDataType(DataType.VarChar)
                .withMaxLength(65535)
                .build();

        FieldType metadataField = FieldType.newBuilder()
                .withName("metadata")
                .withDataType(DataType.JSON)
                .build();

        // 创建collection
        CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withDatabaseName(databaseName)
                .withDescription("Collection for " + collectionName)
                .addFieldType(docIdField)
                .addFieldType(embeddingField)
                .addFieldType(contentField)
                .addFieldType(metadataField)
                .build();

        milvusClient.createCollection(createCollectionParam);

        // 创建索引
        CreateIndexParam createIndexParam = CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withDatabaseName(databaseName)
                .withFieldName("embedding")
                .withIndexType(IndexType.IVF_FLAT)
                .withMetricType(MetricType.COSINE)
                .withExtraParam("{\"nlist\":128}")
                .build();

        milvusClient.createIndex(createIndexParam);

        // 加载collection
        milvusClient.loadCollection(
                io.milvus.param.collection.LoadCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withDatabaseName(databaseName)
                        .build()
        );

        log.info("成功创建collection: {}", collectionName);
    }
}
