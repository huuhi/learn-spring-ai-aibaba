package alibaba.datafilter.config;

import alibaba.datafilter.node.EndNode;
import alibaba.datafilter.node.RouterNode;
import alibaba.datafilter.node.SearchNode;
import alibaba.datafilter.node.TaskNode;
import cn.hutool.core.lang.hash.Hash;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;


/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/7
 * 说明:深度研究这一块
 * 思路：先根据用户需要研究的方向，去制定一个研究方案
 * 然后将方案给用户，让用户自己修改，然后确定之后再开始研究！
 */
@Configuration
@Slf4j
public class ResearchConfig {


    @Bean
    public StateGraph ResearchGraph(ChatClient.Builder builder) throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory=()->{
            HashMap<String, KeyStrategy> map = new HashMap<>();
            map.put("plan",new ReplaceStrategy());
            map.put("query",new ReplaceStrategy());
            map.put("search_key",new ReplaceStrategy());
            map.put("search_result",new ReplaceStrategy());
            return map;
        };
        return new StateGraph(keyStrategyFactory)
                .addNode("task", node_async(new TaskNode(builder)))
                .addNode("research", node_async(new SearchNode(builder)))
                .addNode("end_node", node_async(new EndNode()))
                .addEdge(StateGraph.START, "task")
                .addEdge("task","research")
                .addConditionalEdges("research", AsyncEdgeAction.edge_async((new RouterNode(builder))), Map.of(
                        "CONTINUE", "task", "OK", "end_node"
                ))
                .addEdge("end_node",StateGraph.END);
    }

}
