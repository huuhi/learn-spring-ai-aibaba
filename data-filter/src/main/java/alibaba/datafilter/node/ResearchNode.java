package alibaba.datafilter.node;

import alibaba.datafilter.model.domain.ResearchPlanStep;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/9
 * 说明:
 */
@Slf4j
public class ResearchNode implements NodeAction {
    private final ChatClient chatClient;

    public ResearchNode(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultAdvisors(
                        new SimpleLoggerAdvisor()
                )
                .defaultOptions(
                         DashScopeChatOptions.builder()
                                 .withEnableSearch(true)
                                 .withSearchOptions(DashScopeApi.SearchOptions.builder().searchStrategy("pro").build())
                                 .build()
                )
                .build();
    }
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
//        String searchKey = state.value("search_key", "");
        String searchValue = state.value("research_result", "");
        List<ResearchPlanStep> plan = state.value("plan",List.of());
//        获取前两个 完成为false
        List<ResearchPlanStep> list = plan.stream().filter(p->p.getCompleted()==null || !p.getCompleted()).limit(3).toList();
        log.debug("开始搜集资料, 历史搜索资料: {}", searchValue.length());
        
        // 检查列表是否为空
        if (list.isEmpty()) {
            log.debug("没有需要处理的研究计划步骤");
            return Map.of("research_result", searchValue);
        }
        
        String content = chatClient.prompt()
                .system("""
                        你是一位专业的研究助手，负责执行研究计划中的具体步骤并收集相关信息。你的任务是根据提供的研究步骤进行深入研究，并将结果清晰、有条理地总结出来。

                        请严格按照以下要求执行：
                        1. 仔细分析每个研究步骤的要求和目标
                        2. 利用联网搜索功能获取权威、可靠的信息
                        3. 对搜索到的信息进行筛选和整理，提取关键要点
                        4. 以清晰、逻辑性强的方式总结研究成果
                        5. 确保研究结果与对应的步骤完全匹配
                        """)
                .user(u -> u.text("""
                        这是截至目前已经完成的研究内容：
                        {data}
                        请执行以下研究步骤：
                        {search_key}
                        要求：
                        1. 对每个步骤进行独立研究，确保覆盖所有要点
                        2. 如果某些步骤不需要额外搜索（如总结类步骤），请基于已有知识和之前的研究结果进行分析
                        3. 提供具体、详实的信息，避免空泛的表述
                        4. 保持专业、严谨的学术风格
                        5. 仅输出研究结果，不要添加其他说明或格式化内容
                        """).param("search_key", JSONUtil.toJsonStr(list)).param("data", searchValue))
                .call()
                .content();
        
        // 创建要标记为完成的步骤ID集合
        Set<String> completedIds = list.stream()
                .map(ResearchPlanStep::getId)
                .collect(Collectors.toSet());
        
        // 更新计划步骤状态
        plan.forEach(p -> {
            if (completedIds.contains(p.getId())) {
                p.setCompleted(true);
            }
        });
        
        log.info("搜集资料完成, 搜索结果: {}", content != null ? content.substring(0, Math.min(50, content.length())) : "无内容");
        String result = searchValue + "\n\n" + (content != null ? content : "");
        state.updateState(Map.of("research_result", result));
        return Map.of("research_result", result);
    }
}