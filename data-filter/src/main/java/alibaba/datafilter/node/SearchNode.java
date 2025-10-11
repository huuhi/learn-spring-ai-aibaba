package alibaba.datafilter.node;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/2
 * 说明:
 */
@Slf4j
public class SearchNode implements NodeAction {
    private final ChatClient chatClient;

    public SearchNode(ChatClient.Builder builder) {
        this.chatClient = builder.defaultOptions(
                DashScopeChatOptions.builder()
                        .withSearchOptions(DashScopeApi.SearchOptions.builder().searchStrategy("pro").build())
                        .withEnableSearch(true)
                        .build()
        ).build();
    }


    @Override
    public Map<String, Object> apply(OverAllState state) {
//        获取用户的问题
        String query = state.value("query","");
        String searchResult = state.value("search_result", "");
        log.info("query:{}",query);
        log.info("searchResult:{}",searchResult);
        String content = chatClient.prompt()
                .user(u->u.text("""
                        你需要根据用户的问题，进行全网搜索，优先搜索权威网站确保搜索数据的准确性！多组数据进行对比！
                        如果之前搜索过了就不需要再次搜索了！最后输出你的搜索结果！
                        用户的问题:{query}
                        之前搜索过的数据:{searchResult}
                        """).param("query",query)
                        .param("searchResult",searchResult))
                .call()
                .content();
//        追加新的搜索结果！
        String updatedSearchResult = content + searchResult;
        state.updateState(Map.of("search_result", updatedSearchResult));
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("search_result", updatedSearchResult);
        return hashMap;
    }
}
