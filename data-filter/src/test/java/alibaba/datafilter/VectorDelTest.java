package alibaba.datafilter;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.function.Function;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/21
 * 说明:
 */
@SpringBootTest
public class VectorDelTest {
    @Autowired
    private  Function<String, MilvusVectorStore> dynamicVectorStoreFactory;

    @Test
    public void test() {
        MilvusVectorStore vectorStore = dynamicVectorStoreFactory.apply("test");
        vectorStore.delete("source_description == 'Linux-常用命令'");
    }

}
