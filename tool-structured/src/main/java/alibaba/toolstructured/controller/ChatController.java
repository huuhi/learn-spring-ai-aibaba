package alibaba.toolstructured.controller;

import alibaba.toolstructured.entity.Jobs;
import alibaba.toolstructured.tools.JobTools;
import com.alibaba.cloud.ai.dashscope.api.DashScopeResponseFormat;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/23
 * 说明:
 */
@RestController
@RequestMapping("/tool-structure")
public class ChatController {

    private final ChatClient chatClient;
    private final DashScopeResponseFormat responseFormat;
//    可变集合
    public static final  List<Jobs> jobs=new CopyOnWriteArrayList<>();;
    private final JobTools jobTools;

    @Autowired
    public ChatController(ChatClient.Builder builder, JobTools jobTools) {
        DashScopeResponseFormat responseFormat = new DashScopeResponseFormat();
        responseFormat.setType(DashScopeResponseFormat.Type.JSON_OBJECT);
        this.jobTools=jobTools;
        this.responseFormat = responseFormat;

        this.chatClient=builder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @GetMapping("/job")
    public void job() {
        String JOB_PROMPT = """
                生成一个随机职位
                返回格式:
                {
                    "title": "软件工程师",
                    "salary": "年薪 20-40万 💰",
                    "matchScore": "95",
                    "type":4,
                    "description": "软件工程师主要负责从需求分析到代码实现的全流程开发工作，需要将业务需求转化为高效可靠的技术解决方案。日常工作中，他们需要设计系统架构、编写和优化代码、进行单元测试和调试，并与产品、测试等团队紧密协作。随着云计算、人工智能等技术的快速发展，软件工程师不仅要掌握Java、Python等编程语言和Spring等主流框架，还需要不断学习新技术，比如云原生架构或AI编程工具的应用。这个岗位是数字经济的核心基础，技术迭代快，经验丰富的工程师在金融科技、智能制造等高价值领域尤其抢手。虽然AI可以辅助基础编码，但复杂的系统设计和架构仍然依赖工程师的专业判断。",
                    "outlook": "作为数字经济时代的核心基建岗位，软件工程师需求持续领跑全行业。在政策层面，'十四五'规划明确将云计算、人工智能等列为重点产业，催生大量高端开发岗位。技术迭代方面，随着云原生、AI编程助手（如GitHub Copilot）的普及，开发效率提升的同时也创造了更复杂的系统架构需求。就业市场呈现两极分化：初级岗位竞争加剧，但具备分布式系统设计能力或垂直领域（如金融科技、智能驾驶）经验的中高级工程师严重短缺。未来5年，物联网（IoT）和元宇宙相关开发岗位预计增长200%，同时绿色计算、量子编程等新兴领域正在创造高溢价岗位。🌐 资深工程师可向解决方案架构师、CTO等方向发展，或选择在Web3.0、AI基础设施等赛道创业。",
                    "skills": ["Java", "Python", "数据结构", "算法", "Spring框架"],
                    "personalityTraits": ["逻辑性强 🧠", "耐心 ⏳", "团队协作 👥", "持续学习 📚"],
                    "dailyTasks": "1. 参与需求评审\\n2. 编写和优化代码\\n3. 单元测试与调试\\n4. 技术文档编写\\n5. 与产品/测试团队协作",
                    "careerGrowth": "初级开发 → 高级开发 → 技术专家 → 架构师 或 转管理方向 📈",
                    "automationRisk": "较低风险 ✅",
                    "riskExplanation": "基础编码可被AI辅助，但系统设计和架构仍需人类决策。🤖"
                }
                """;
        jobs.add(chatClient.prompt(JOB_PROMPT)
                .options(
                        DashScopeChatOptions.builder()
                                .withTopP(1.0)//温度，越大输出结果越随机
                                .withResponseFormat(responseFormat)
                                .build()
                                )
                .call()
                .entity(Jobs.class));
    }

    @GetMapping("/analysis")
    public Flux<String> analysis(@RequestParam(defaultValue = "帮我分析一下职位表有什么特征")String query) {
        return chatClient
                .prompt(query)
                .tools(jobTools)
                .stream()
                .content();
    }

}
