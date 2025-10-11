package alibaba.graphdemo.node;

import alibaba.graphdemo.tool.McpClientToolCallbackProvider;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/2
 * 说明:
 */
@Slf4j
public class McpNode implements NodeAction {

    private final ChatClient chatClient;
    private static final String NODE_NAME = "mcp-node";

    public McpNode(ChatClient.Builder builder, McpClientToolCallbackProvider mcpClientToolCallbackProvider) {
        Set<ToolCallback> toolCallbacks = mcpClientToolCallbackProvider.findToolCallbacks(NODE_NAME);
        for (ToolCallback toolCallback : toolCallbacks) {
            log.info("加载的工具:{}", toolCallback);
        }
        this.chatClient = builder
                .defaultToolCallbacks(toolCallbacks.toArray(new ToolCallback[0]))
                .build();
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String query = state.value("query", "");
        
        // 处理查询并返回结果
        try {
            Flux<String> content = chatClient.prompt(query).stream().content();
            String result = content.reduce("", (acc, item) -> acc + item).block();
            HashMap<String, Object> resultMap = new HashMap<>();
            resultMap.put("mcp_content", result);
            return resultMap;
        } catch (Exception e) {
            log.error("MCP节点处理出错: ", e);
            HashMap<String, Object> resultMap = new HashMap<>();
            resultMap.put("mcp_content", "MCP服务暂时不可用，请稍后再试。");
            return resultMap;
        }
    }
}