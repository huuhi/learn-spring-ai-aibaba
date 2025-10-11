package alibaba.datafilter.node;

import alibaba.datafilter.model.domain.ResearchPlanStep;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;

import java.util.*;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/8
 * 说明: 这个节点来制定搜索计划，写搜索关键字
 */
@Slf4j
public class TaskNode implements NodeAction {
    private final ChatClient chatClient;

    public TaskNode(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultAdvisors(new SimpleLoggerAdvisor()).build();
    }
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        List<ResearchPlanStep> plan = state.value("plan",List.of());
        log.debug("研究进度：{}",JSONUtil.toJsonStr(plan));
        String content = chatClient.prompt("""
                        你需要根据研究步骤，去制定每个步骤的搜索词，也就是去互联网搜索资料的关键词，需要确保可以搜索出有效数据！
                        你一次只为一个步骤制定关键词，必须严格按照优先级来制定！
                        如果completed状态为true。则表示已经完成
                        返回要求，需要用;分开：
                        步骤id;搜索关键字
                        """)
                .user(u -> u.text("""
                        这是研究步骤：
                        {plan}
                        """).param("plan", JSONUtil.toJsonStr(plan)))
                .call().content();
        assert content != null;
        String[] split = content.split(";");
        String id= split[0];
        log.info("id:{}",id);
        log.info("search_key:{}",split[1]);
        plan.stream().filter(p->p.getId().equals(id)).findFirst().get().setCompleted(true);
        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("search_key",split[1]);
        return hashMap;
    }
}
