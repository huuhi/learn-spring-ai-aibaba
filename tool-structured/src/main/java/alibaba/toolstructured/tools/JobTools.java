package alibaba.toolstructured.tools;

import alibaba.toolstructured.controller.ChatController;
import alibaba.toolstructured.entity.Jobs;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/23
 * 说明:
 */
public class JobTools {

    @Tool(description = "获取职位列表")
    public List<Jobs> getJobs(){
        return ChatController.jobs;
    }
}
