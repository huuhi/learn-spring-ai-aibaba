package alibaba.datafilter.tools;

import alibaba.datafilter.config.DataFilterConfig;
import alibaba.datafilter.model.domain.ResearchPlanStep;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static alibaba.datafilter.service.impl.ChatServiceImpl.dabateMap;

/**
 * @author 胡志坚
 * @version 1.0
 * 创建日期 2025/10/2
 * 说明: 数据过滤工作流工具类
 */
@Component
@Slf4j
public class DataFilterTool {

    private final CompiledGraph compiledGraph;

    public DataFilterTool(ChatClient.Builder builder) throws GraphStateException {
        this.compiledGraph = new DataFilterConfig()
                .dataFilterService(builder)
                .compile();
    }
    @Tool(name = "data_filter_search", description = "执行数据过滤搜索工作流，根据用户问题进行联网搜索并过滤结果")
    public String dataFilterSearch(String query) {
        try {
            RunnableConfig runnableConfig = RunnableConfig.builder().threadId("data-filter-thread").build();
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("query", query);

            Optional<OverAllState> result = this.compiledGraph.invoke(inputs, runnableConfig);
            if (result.isPresent()) {
                return result.get().value("filtered_search_result", "未找到过滤后的搜索结果");
            } else {
                return "工作流执行未返回结果";
            }
        } catch (Exception e) {
            log.error("执行数据过滤工作流出错", e);
            return "执行数据过滤工作流出错: " + e.getMessage();
        }
    }
    @Tool(name="get_now_data",description = "获取当前时间")
    public String getNowData(){
        return "当前时间是：" + LocalDateTime.now();
    }
}