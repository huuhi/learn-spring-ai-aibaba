package alibaba.datafilter.config;

import alibaba.datafilter.node.FilterNode;
import alibaba.datafilter.node.RouterNode;
import alibaba.datafilter.node.SearchNode;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
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
 * 创造日期 2025/10/2
 * 说明: 信息过滤工作流
 */
@Configuration
@Slf4j
public class DataFilterConfig {
    @Bean
    public StateGraph dataFilterService(ChatClient.Builder builder) throws GraphStateException {
        //创建工作流
        KeyStrategyFactory keyStrategyFactory=()->{
            HashMap<String, KeyStrategy> keyStragegyHashMap = new HashMap<>();
//            需要搜索的内容
            keyStragegyHashMap.put("query", new ReplaceStrategy());
//            搜索的结果，需要让有联网搜索的模型去搜索，直接输出搜索的结果即可
            keyStragegyHashMap.put("search_result",new ReplaceStrategy());
//            决策模型
            keyStragegyHashMap.put("decision",new ReplaceStrategy());
//            将搜索的结果进行过滤，用AI模型进行判断哪些内容是不需要的，然后输出过滤之后的结果
            keyStragegyHashMap.put("filtered_search_result",new ReplaceStrategy());
//            最终根据过滤后的搜索结果进行回答
//            keyStragegyHashMap.put("answer",new ReplaceStrategy());
            return keyStragegyHashMap;
        };

        StateGraph stateGraph = new StateGraph(keyStrategyFactory)
                .addNode("search", node_async(new SearchNode(builder)))
//                .addNode("decision",node_async(new RouterNode(builder)))
                .addNode("filtered", node_async(new FilterNode(builder)))
                .addEdge(StateGraph.START, "search")
                .addConditionalEdges("search", AsyncEdgeAction.edge_async((new RouterNode(builder))), Map.of(
                        "CONTINUE", "search", "OK", "filtered"
                ))
                .addEdge("filtered", StateGraph.END);
        GraphRepresentation representation = stateGraph.getGraph(GraphRepresentation.Type.PLANTUML, "data filter flow");
        log.info("data filter flow: {}", representation.content());
        return stateGraph;

    }


}