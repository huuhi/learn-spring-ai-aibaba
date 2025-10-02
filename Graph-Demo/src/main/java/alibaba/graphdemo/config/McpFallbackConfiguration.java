package alibaba.graphdemo.config;

import org.springframework.ai.mcp.client.autoconfigure.McpClientAutoConfiguration;
import org.springframework.ai.mcp.client.autoconfigure.McpToolCallbackAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;

import java.util.Collections;

/**
 * MCP降级配置类，当MCP服务器不可用时提供降级处理
 */
@Configuration
@AutoConfigureBefore({McpClientAutoConfiguration.class, McpToolCallbackAutoConfiguration.class})
public class McpFallbackConfiguration {

    @Bean
    @ConditionalOnMissingBean(SyncMcpToolCallbackProvider.class)
    public SyncMcpToolCallbackProvider mcpToolCallbacks() {
        return new SyncMcpToolCallbackProvider(Collections.emptyList());
    }
}