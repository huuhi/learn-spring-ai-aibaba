package alibaba.datafilter.tools;

import alibaba.datafilter.config.ResearchConfig;
import alibaba.datafilter.model.domain.ResearchPlanStep;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author 你的名字
 * @version 1.0
 * 创建日期 2025/10/8
 * 说明: 研究工作流工具类
 */
@Component
@Slf4j
public class ResearchTool {

    private final CompiledGraph compiledGraph;

    public ResearchTool(ChatClient.Builder builder) throws GraphStateException {
        // 初始化你的研究工作流
        this.compiledGraph = new ResearchConfig()
                .ResearchGraph(builder)
                .compile();
    }

    
    // 你可以根据需要添加更多工具方法
    @Tool(name = "research_with_plan", description = "根据研究计划执行研究工作流")
    public String executeResearchWithPlan(List<ResearchPlanStep> planSteps) {
        try {
            RunnableConfig runnableConfig = RunnableConfig.builder().threadId("research-with-plan-thread").build();
            
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("plan", planSteps);
            // 添加其他需要的输入参数

            Optional<OverAllState> result = this.compiledGraph.invoke(inputs, runnableConfig);
            
            if (result.isPresent()) {
                return result.get().value("research", "未找到研究结果");
            } else {
                return "工作流执行未返回结果";
            }
        } catch (Exception e) {
            log.error("执行研究工作流出错", e);
            return "执行研究工作流出错: " + e.getMessage();
        }
    }
}
