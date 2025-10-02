package alibaba.graphdemo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Set;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/2
 * 说明:
 */
@Setter
@Getter
@ConfigurationProperties(prefix = McpNodeProperties.PREFIX)
public class McpNodeProperties {
    // 配置前缀
    public static final String PREFIX = "spring.ai.graph.nodes";

    private Map<String, Set<String>> node2servers;

}
