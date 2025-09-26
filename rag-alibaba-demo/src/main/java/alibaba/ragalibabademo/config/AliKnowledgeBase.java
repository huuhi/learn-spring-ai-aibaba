package alibaba.ragalibabademo.config;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionProperties;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/23
 * 说明:
 */
//这个注解告诉SpringBoot，这是一个自动配置类，SpringBoot会根据这个类中的配置来创建相应的Bean
@AutoConfiguration
@EnableAutoConfiguration
public class AliKnowledgeBase {
    @Bean
    public DashScopeApi dashScopeApi(DashScopeConnectionProperties connectionProperties) {

//        通过DashScopeConnectionProperties对象创建DashScopeApi对象
        return DashScopeApi.builder().apiKey(connectionProperties.getApiKey()).build();
    }

}
