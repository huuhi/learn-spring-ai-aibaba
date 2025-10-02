package alibaba.graphdemo.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class WorkflowControlTool {

    @Tool(name = "stop_search", description = "当搜索到足够信息或确定答案时，调用此工具来结束搜索流程")
    public String stopSearch(String reason) {
        return "搜索已结束，原因: " + reason;
    }
}