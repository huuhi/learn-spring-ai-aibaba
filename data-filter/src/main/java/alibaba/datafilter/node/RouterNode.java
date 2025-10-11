package alibaba.datafilter.node;


import alibaba.datafilter.model.domain.ResearchPlanStep;
import alibaba.datafilter.model.vo.ResearchPlanStepVO;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;

import java.util.List;
import java.util.Objects;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/2
 * 说明:
 */
@Slf4j
public class RouterNode implements EdgeAction {
    private final ChatClient chatClient;

    public RouterNode(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultAdvisors(
                new SimpleLoggerAdvisor()
        ).build();
    }

    @Override
    public String  apply(OverAllState state){
//        获取搜索的结果
        String result=state.value("search_result","");
//        获取用户的问题
        String query=state.value("query","");
        List<ResearchPlanStep> plan= state.value("plan",List.of());
        if(!plan.isEmpty()){
//            如果没有一个false，则返回OK
            if (plan.stream().allMatch(ResearchPlanStep::getCompleted)) {
                log.debug("所有步骤已经完成，返回OK");
                return "OK";
            }else{
                log.debug("有步骤没有完成，返回CONTINUE");
                return "CONTINUE";
            }
        }

        String content = chatClient.prompt()
                .user(u -> u.text("""
                        你是一个严谨的决策专家。你的任务是根据用户提出的原始问题和当前提供的搜索结果，判断这些搜索结果是否**在语义上高度匹配原始问题，并且涵盖了问题中提及的所有关键实体、概念和属性，从而足以构建一个直接的答案。**
                        请注意：你不需要判断信息的实时性或绝对真伪，只需评估搜索结果与问题的**语义关联度和信息覆盖度**。
                        原始问题: {query}
                        当前搜索结果:
                        {results}
                        请遵循以下决策规则：
                        1.  **如果**当前搜索结果在语义上直接关联原始问题，并且包含了回答问题所需的所有关键实体和概念，没有明显的信息缺失，那么信息是足够的。
                        2.  **否则**（即搜索结果与问题关联度不高，或者明显遗漏了问题中的关键信息、实体或概念），信息是不足够的。
                        **请严格遵守输出格式：**
                        如果信息已经足够，请只输出 "OK"。
                        如果信息不足，请只输出 "CONTINUE"。
                        """).param("query", query)
                        .param("results", result))
                .call()
                .content();
        log.debug("决策的结果：{}", Objects.equals(content, "OK") ?"结束":"继续！");
        assert content != null;
        return content;
    }
}
