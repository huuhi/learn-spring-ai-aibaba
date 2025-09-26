package alibaba.toolstructured.config;

import alibaba.toolstructured.tools.JobTools;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/23
 * 说明:
 */
@Configuration

public class ToolCallAutoConfiguration {

    @Bean
    public JobTools jobTools(){
        return new JobTools();
    }
}
