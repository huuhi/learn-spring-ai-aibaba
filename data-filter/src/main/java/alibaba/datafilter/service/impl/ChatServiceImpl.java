package alibaba.datafilter.service.impl;


import alibaba.datafilter.common.utils.MilvusVectorStoreUtils;
import alibaba.datafilter.model.domain.ResearchPlanStep;
import alibaba.datafilter.model.domain.ResearchQuestionDTO;
import alibaba.datafilter.model.dto.QuestionDTO;
import alibaba.datafilter.model.dto.RequestDTO;
import alibaba.datafilter.model.dto.StreamResponse;
import alibaba.datafilter.service.ChatService;
import alibaba.datafilter.service.CollectionService;
import alibaba.datafilter.tools.DataFilterTool;
import alibaba.datafilter.tools.ResearchTool;
import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/25
 * 说明:
 */
@Service
@Slf4j
public class ChatServiceImpl implements ChatService {
    private final Function<String, MilvusVectorStore> dynamicVectorStoreFactory;
    private final MilvusVectorStoreUtils milvusVectorStoreUtils;
    private final CollectionService collectionService;
    private final DataFilterTool dataFilterTool;
    private final ResearchTool researchTool;
    @Resource
    private   ChatClient chatClient;
    private final List<String> models=List.of("qwen-max","qwen-plus-latest","qwen3-max-2025-09-23","qwen3-max-preview",
            "qwen-plus-2025-07-28","qwen-turbo","Moonshot-Kimi-K2-Instruct","deepseek-r1","deepseek-v3");

    public ChatServiceImpl(Function<String, MilvusVectorStore> dynamicVectorStoreFactory, MilvusVectorStoreUtils milvusVectorStoreUtils, CollectionService collectionService, DataFilterTool dataFilterTool, ResearchTool researchTool) {
        this.dynamicVectorStoreFactory = dynamicVectorStoreFactory;
        this.milvusVectorStoreUtils = milvusVectorStoreUtils;
        this.collectionService = collectionService;
        this.dataFilterTool = dataFilterTool;
        this.researchTool = researchTool;
    }

    @Override
    public ResponseEntity<String> createConversation() {
        String string = UUID.fastUUID().toString();
        return ResponseEntity.ok(string);
    }

    @Override
    public Flux<StreamResponse> chat(RequestDTO requestDTO) {
        DashScopeChatOptions.DashscopeChatOptionsBuilder dashscopeChatOptionsBuilder = DashScopeChatOptions.builder()
                .withEnableSearch(requestDTO.getEnableSearch())
                .withEnableThinking(requestDTO.getEnableThinking());
        String question = requestDTO.getQuestion();
//        模型不为null，不为空，并且在模型列表中
        if(requestDTO.getModel()!=null&& !requestDTO.getModel().isEmpty() && models.contains(requestDTO.getModel())){
            dashscopeChatOptionsBuilder.withModel(requestDTO.getModel());
        }
//        知识库检索的内容
        String searchContent="";
//        获取知识库名称
        String collectionName = requestDTO.getRag();
//       判断用户是否开启了rag检索，如果开启需要想判断知识库是否存在
//        TODO 判断知识库是否存在，如果不存在不需要 检索。直接在数据库判断
        if(collectionName!=null&& !collectionName.isEmpty()&&collectionService.isContains(collectionName)){
            searchContent=ragSearch(question, requestDTO.getRag());
        }
        String prompt=String.format("用户的问题:%s,知识库检索的结果:%s,注意:知识库的内容可能为空，如果为空，说明用户没有开启知识库检索或者知识库没有检索的内容，需要你直接回答用户的问题",question,searchContent);
        Flux<ChatResponse> responseFlux = chatClient.prompt(prompt)
                .advisors(p -> p.param(CONVERSATION_ID, requestDTO.getConversationId()))
                .options(dashscopeChatOptionsBuilder.build())
                .stream()
                .chatResponse();
        return getStreamResponseFlux(responseFlux);
    }

    @Override
    public Flux<StreamResponse> dataFilterSearch(String query, String conversationId) {
        Flux<ChatResponse> chatResponseFlux = chatClient.prompt("""
                        你是一个智能助手，能够回答用户问题，并根据需要灵活调用工具。
                       **通用工具调用规则：** 仔细分析用户问题，如果回答需要外部信息或特定功能（如数据过滤），请调用相应的工具。
                       **关于时间工具的特殊规则：** 如果用户的问题涉及**当前日期、时间**（例如：“现在几点？”、“今天是什么日子？”、“当前时间？”），
                       或者为了准确回答用户问题需要**实时时间信息**（例如：“今天有什么新闻？”），而用户未提供明确的日期或时间信息时，请调用你可用的工具来获取当前时间。
                       """)
                .options(DashScopeChatOptions.builder().withEnableThinking(true).build())
                .advisors(p -> p.param(CONVERSATION_ID,conversationId))
                .user(query)
                .tools(dataFilterTool)
                .stream()
                .chatResponse();
        return getStreamResponseFlux(chatResponseFlux);
    }

    @Override
    public ResponseEntity<List<?>> developPlan(QuestionDTO question) {
        ChatClient.CallResponseSpec responseSpec = chatClient.prompt("""
                        你是一位资深的领域研究专家和研究方案制定者。你的任务是根据用户提出的原始研究问题，制定一份详细、可执行、有逻辑顺序的深度研究方案。
                        请将研究方案拆解为多个独立的步骤，并以JSON数组的形式返回。每个步骤应包含以下字段：
                        - `id`: 字符串类型，为该步骤生成的唯一标识符（例如："step-1"）。
                        - `priority`: 整数类型，表示该步骤在整体研究中的执行顺序（1为最高优先级）。
                        - `title`: 字符串类型，简洁明了的研究步骤标题。
                        - `description`: 字符串类型，详细说明该步骤的目的、需要完成的具体任务和需要关注的重点。
                        - `expectedOutcome`: 字符串类型，明确该步骤完成后应该得到什么具体的结果、信息或产出。
                        """)
                .user(u -> u.text("""
                        用户的原始问题：{question}
                        要求：
                        1.  研究方案应全面覆盖原始问题的各个方面，体现深度研究的特点，而不仅仅是表层搜索。
                        2.  步骤之间应有清晰的逻辑顺序和依赖关系（通过priority体现），确保前一步骤的结果能为后一步骤提供支撑。
                        3.  每个步骤的`description`和`expectedOutcome`要足够具体，能够指导后续的搜索和分析工作，并能够作为判断步骤是否完成的依据。
                        5.  请生成5-10个核心研究步骤，确保方案的深度和广度。
                        请直接以JSON数组格式输出，不要包含任何额外文字说明。
                        """).param("question", question.getQuestion()))
                .advisors(p->p.param(CONVERSATION_ID,question.getQuestionId()))
                .call();

        try {
            List<ResearchPlanStep> researchPlans = responseSpec.entity(new ParameterizedTypeReference<>() {});
            return ResponseEntity.ok(researchPlans);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of("生成方案失败"));
        }
    }

    @Override
    public Flux<StreamResponse> research(ResearchQuestionDTO researchQuestionDTO) {
        Flux<ChatResponse> chatResponseFlux = chatClient.prompt()
                .system("""
                你是一位专业且高效的研究助手，你的核心职责是根据提供的研究计划，**从头到尾执行所有研究步骤，并最终生成一份完整、结构化的综合研究报告。**
                            请严格按照以下要求执行：
                            1.  **理解任务：** 仔细分析每个研究步骤的要求和目标，理解原始研究问题。
                            2.  **利用工具：** 主动且准确地调用外部联网搜索工具（researchTool）来获取权威、可靠的信息。
                            3.  **信息处理：** 对搜索到的信息进行深度筛选、整理、分析和交叉验证，提取核心发现和论据。
                            4.  **综合撰写：** 在所有研究步骤和信息收集完成后，将所有获取的信息进行逻辑整合，并**立即**开始撰写最终的研究报告。
                            5.  **质量要求：** 确保研究结果与对应的步骤完全匹配，报告内容清晰、逻辑性强，直接回答原始研究问题，并达到预期的深度和广度。
                """)
                .user(u ->
                        u.text("""
                    **核心任务：** 你将作为一名研究专家，根据以下提供的研究计划执行所有研究活动，并直接产出最终的综合研究报告。你的本次响应**必须是**这份报告的开始部分或其主要内容，而不仅仅是关于任务进展的描述。
                      **原始研究问题：**
                      {question}
                      **研究计划（JSON格式）：**
                      {researchPlanStepsJson}
                      **你的职责与详细执行流程：**
                      1.  **解析研究计划：** 深入理解研究计划中的每个步骤，明确其具体要求和预期产出。
                      2.  **工具执行（必要时）：** 对于需要外部数据支持的步骤，你必须主动、准确地调用 `researchTool` 工具来执行。
                          *   请确保在调用工具时，参数 `search_key` 和 `data` 被正确填充。
                          *   **切勿仅仅描述你将要调用工具，而是要实际执行调用并获取结果。**
                      3.  **信息整合与分析：** 在所有研究步骤（包括所有 `researchTool` 调用）完成后，将所有收集到的信息进行全面、系统的整合、分析和总结。
                      4.  **撰写最终研究报告（立即开始）：** 基于整合分析后的所有研究成果，你必须**立即开始撰写**最终的研究报告。
                          *   **报告目标：** 报告应直接回应并深入探讨原始研究问题，全面覆盖研究计划中的所有步骤内容。
                          *   **报告结构：** 请提供一个清晰的报告结构（例如，引言、背景、研究方法、各研究步骤的详细发现、讨论、结论等）。
                          *   **字数指导：** 最终研究报告的期望字数在 10000-30000 字之间。**不要等待或声明，请直接开始报告内容。**
                          *   **禁止中间声明：** 在所有研究步骤完成后，请勿再进行任何“我已完成研究，现在将开始撰写报告”之类的中间性说明。你的输出必须直接是报告内容。

                      **重要提示：**
                      -   严格遵循上述流程，先完成所有工具调用和数据收集，再进行报告撰写。
                      -   你的本次输出的唯一目的是生成研究报告的内容。
                    """).param("question", researchQuestionDTO.getQuestion())
                                .param("researchPlanStepsJson", JSONUtil.toJsonStr(researchQuestionDTO.getResearchPlanSteps())))
                .advisors(p -> p.param(CONVERSATION_ID, researchQuestionDTO.getConversationId()))
                .options(DashScopeChatOptions.builder().withEnableThinking(true).build())
                .tools(researchTool)
                .stream()
                .chatResponse();
//        researchQuestionDTO.getResearchPlanSteps().


        return getStreamResponseFlux(chatResponseFlux);

    }

    @NotNull
    private Flux<StreamResponse> getStreamResponseFlux(Flux<ChatResponse> chatResponseFlux) {
        return chatResponseFlux.flatMap(chatResponse->{
            Flux<StreamResponse> eventFlux = Flux.empty();
            if(!chatResponse.getResults().isEmpty()){
                Map<String, Object> metadata = chatResponse.getResults().get(0).getOutput().getMetadata();
                if(metadata.containsKey("reasoningContent")){
                    Object reasoning = metadata.get("reasoningContent");
                    if(reasoning != null && !reasoning.toString().isEmpty()){
                        eventFlux = eventFlux.concatWith(Flux.just(new StreamResponse("THINKING", reasoning)));
                    }
                }
                String text = chatResponse.getResult().getOutput().getText();
                if (text != null && !text.isEmpty()){
                    eventFlux = eventFlux.concatWith(Flux.just(new StreamResponse("CONTENT", text)));
                }
            }
            return eventFlux;
        });
    }

    private String ragSearch(String query,String collectionName){
//        TODO 判断知识库是否存在，不存在直接返回""
        if(!milvusVectorStoreUtils.isValidCollectionName(collectionName)){
            log.info("知识库不存在啦！");
            return "";
        }
//        如果存在则检索，返回
        MilvusVectorStore vectorStore = dynamicVectorStoreFactory.apply(collectionName);
        List<Document> documents = vectorStore.similaritySearch(query);
        StringBuilder stringBuilder = new StringBuilder();
        for (Document doc : documents) {
            stringBuilder.append(doc.getText()).append("\n\n");
        }
        return stringBuilder.toString();
    }
}