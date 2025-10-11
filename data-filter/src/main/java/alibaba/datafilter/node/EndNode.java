package alibaba.datafilter.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/8
 * 说明:
 */
@Slf4j
public class EndNode implements NodeAction {
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String researchResult = state.value("research_result", "");
        log.debug("研究完毕。总共：{}个字符", researchResult.length());
        return Map.of();
    }
}
