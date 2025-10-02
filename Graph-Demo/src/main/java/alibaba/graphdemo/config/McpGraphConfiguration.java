package alibaba.graphdemo.config;

import alibaba.graphdemo.node.DeepSearchNode;
import alibaba.graphdemo.node.McpNode;
import alibaba.graphdemo.tool.McpClientToolCallbackProvider;
import alibaba.graphdemo.tool.WorkflowControlTool;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import io.modelcontextprotocol.client.McpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/2
 * 说明:
 */
@Configuration
@EnableConfigurationProperties({McpNodeProperties.class})
@Slf4j
public class McpGraphConfiguration {

    @Autowired
    private McpClientToolCallbackProvider mcpClientToolCallbackProvider;

    @Bean
    public WorkflowControlTool workflowControlTool() {
        return new WorkflowControlTool();
    }

    @Bean
    public StateGraph mcpGraph(ChatClient.Builder builder) throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> keyStragegyHashMap = new HashMap<>();

            keyStragegyHashMap.put("query", new ReplaceStrategy());
            keyStragegyHashMap.put("mcp_content", new ReplaceStrategy());
            keyStragegyHashMap.put("search_depth", new ReplaceStrategy());
            return keyStragegyHashMap;
        };

        StateGraph stateGraph = new StateGraph(keyStrategyFactory)
                .addNode("deep_search_1", node_async(new DeepSearchNode(builder, mcpClientToolCallbackProvider)))
                .addNode("deep_search_2", node_async(new DeepSearchNode(builder, mcpClientToolCallbackProvider)))
                .addNode("deep_search_3", node_async(new DeepSearchNode(builder, mcpClientToolCallbackProvider)))
                .addEdge(StateGraph.START, "deep_search_1")
                .addEdge("deep_search_1", "deep_search_2")
                .addEdge("deep_search_2", "deep_search_3")
                .addEdge("deep_search_3", StateGraph.END);

        GraphRepresentation representation = stateGraph.getGraph(GraphRepresentation.Type.PLANTUML, "mcp flow");

        log.info("mcp flow: {}", representation.content());
        return stateGraph;
    }
}