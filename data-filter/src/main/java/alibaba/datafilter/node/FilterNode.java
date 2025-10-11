package alibaba.datafilter.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;

import java.util.Map;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/2
 * 说明: 对数据继续过滤
 */
@Slf4j
public class FilterNode implements NodeAction {
    private final ChatClient chatClient;

    public FilterNode(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultAdvisors(
                        new SimpleLoggerAdvisor()
                ).build();
    }


    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
//        需要对数据继续过滤！
        String result=state.value("search_result","");
        String query=state.value("query","");
        String content = chatClient.prompt("""
                        你是一个严谨的信息审查员和数据提纯专家。
                        你的任务是根据用户的原始问题，仔细审阅以下提供的搜索结果，并将其中所有与问题**不直接相关**、**过时**、**重复**、**广告宣传性质**，或**任何被判断为低质量、不准确或干扰性**的信息彻底删除。
                        **请你完成以下步骤:**
                        1.  逐条审阅每个搜索结果。
                        2.  删除任何不符合上述标准的信息。
                        3.  对保留下来的信息，如果包含过多冗余的背景叙述或不必要的细节，请将其精简到只剩核心内容。
                        4.  将所有被你判断为有效且精简后的信息，按其重要性或逻辑顺序重新组织，并以清晰的列表或段落形式输出。
                        5.  **严格禁止**添加任何你自己的观点、总结或额外信息。你的输出应该只是经过净化的原始数据。
                        """)
                .user(u -> u.text("""
                                用户的问题：{query}
                                搜索结果:{search_result}
                                """).param("query", query)
                        .param("search_result", result)).call().content();
        log.info("原始数据:{}",result);
        log.info("过滤结果：{}",content);

        assert content != null;
        return Map.of("filtered_search_result",content);
    }
}
