package alibaba.graphdemo.tool;

import alibaba.graphdemo.config.McpNodeProperties;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/2
 * 说明:
 */
@Service
public class McpClientToolCallbackProvider {
    private final ToolCallbackProvider toolCallbackProvider;
    private final McpClientCommonProperties commonProperties;
    private final McpNodeProperties nodeProperties;
    private final McpNodeProperties mcpNodeProperties;


    public McpClientToolCallbackProvider(ToolCallbackProvider toolCallbackProvider, 
                                         McpClientCommonProperties commonProperties, 
                                         McpNodeProperties nodeProperties, 
                                         McpNodeProperties mcpNodeProperties) {
        this.toolCallbackProvider = toolCallbackProvider;
        this.commonProperties = commonProperties;
        this.nodeProperties = nodeProperties;
        this.mcpNodeProperties = mcpNodeProperties;
    }

    public Set<ToolCallback> findToolCallbacks(String nodeName){
//        存储匹配工具回调的集合
        Set<ToolCallback> defineCallback = Sets.newHashSet();

//        根据节点名称获取对应的mcp服务
        Set<String> mcpClients = mcpNodeProperties.getNode2servers().get(nodeName);
        if(mcpClients==null||mcpClients.isEmpty()){
            return defineCallback;
        }
//        名称列表
        List<String> exceptMcpClientNames = Lists.newArrayList();
        for (String mcpClient : mcpClients) {
//            获取mcp客户端的名称
            String name = commonProperties.getName();
//            加个前缀
            String prefixedToolName = McpToolUtils.prefixedToolName(name, mcpClient);

            exceptMcpClientNames.add(prefixedToolName);
        }
//        获取所有已注册的工具
        ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();

        // 如果没有工具回调，直接返回空集合
        if (toolCallbacks == null) {
            return defineCallback;
        }

        for (ToolCallback toolCallback : toolCallbacks) {
            ToolDefinition toolDefinition = toolCallback.getToolDefinition();
//            获取工具名称
            String toolName = toolDefinition.name();
            for (String exceptMcpClientName : exceptMcpClientNames) {
                if(toolName.startsWith(exceptMcpClientName)){
//                    如果前缀匹配则添加
                    defineCallback.add(toolCallback);
                }
            }
        }
//        返回匹配的工具回调
        return defineCallback;
    }
}