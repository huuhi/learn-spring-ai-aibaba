package alibaba.graphdemo.node;

import alibaba.graphdemo.tool.McpClientToolCallbackProvider;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
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
 * 深度搜索节点，用于实现判断是否继续搜索的逻辑
 */
@Slf4j
public class DeepSearchNode implements NodeAction {

    private final ChatClient chatClient;
    private static final String NODE_NAME = "mcp-node";

    public DeepSearchNode(ChatClient.Builder builder, McpClientToolCallbackProvider mcpClientToolCallbackProvider) {
        Set<ToolCallback> toolCallbacks = mcpClientToolCallbackProvider.findToolCallbacks(NODE_NAME);
        for (ToolCallback toolCallback : toolCallbacks) {
            log.info("加载的工具:{}", toolCallback);
        }
        this.chatClient = builder
                .build();
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String query = state.value("query", "");
        Integer searchDepth = state.value("search_depth", 0);
        String previousContent = state.value("mcp_content", "");
        
        // 增加搜索深度
        searchDepth++;
        
        // 构造带有深度搜索提示的查询
        String deepQuery = String.format(
            "请分析以下问题并使用可用工具搜索答案: %s\n\n" +
            "这是第 %d 轮搜索。\n" +
            "如果已有足够信息回答问题，请使用 'stop_search' 工具结束搜索流程。\n" +
            "如果需要更多信息，请继续使用其他工具进行搜索。\n" +
            "之前的搜索结果: %s", 
            query, searchDepth, previousContent);

        try {
            Flux<String> content = chatClient.prompt(deepQuery).stream().content();
            String result = content.reduce("", (acc, item) -> acc + item).block();
            
            HashMap<String, Object> resultMap = new HashMap<>();
            resultMap.put("mcp_content", result);
            resultMap.put("search_depth", searchDepth);
            return resultMap;
        } catch (Exception e) {
            log.error("深度搜索节点处理出错: ", e);
            HashMap<String, Object> resultMap = new HashMap<>();
            resultMap.put("mcp_content", "MCP服务暂时不可用，请稍后再试。");
            resultMap.put("search_depth", searchDepth);
            return resultMap;
        }
    }
}