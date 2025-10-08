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
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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
        return responseFlux.flatMap(chatResponse -> {
            // flatMap 可以将一个元素转变为 0 到 N 个新元素
            // 完美适配我们的场景：一个 ChatResponse 块可能同时包含思考和内容

            // 1. 准备一个容器来存放这个块产生的所有事件
            Flux<StreamResponse> eventFlux = Flux.empty();
            if (!chatResponse.getResults().isEmpty()) {
                Map<String, Object> metadata = chatResponse.getResults().get(0).getOutput().getMetadata();
//                log.info("Full metadata received: {}", metadata);
                // 2. 检查是否有思考内容
                if (metadata.containsKey("reasoningContent")) {
                    Object reasoning = metadata.get("reasoningContent");
                    if(reasoning != null && !reasoning.toString().isEmpty()){
                        eventFlux = eventFlux.concatWith(Flux.just(new StreamResponse("THINKING", reasoning)));
                    }
                }
            }
            // 4. 检查是否有真正的回答内容
            chatResponse.getResult();
            chatResponse.getResult();
            String content = chatResponse.getResult().getOutput().getText();
            if (content != null && !content.isEmpty()) {
                eventFlux = eventFlux.concatWith(Flux.just(new StreamResponse("CONTENT", content)));
            }
            return eventFlux;
        });
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
                .user(u ->
                        u.text("""
                    你是一个顶级深度研究助手，负责监督和协调一项复杂的深度研究任务。我将为你提供一份详细的研究计划，其中包含多个按优先级排列的研究步骤。
                       **你的主要职责是：**
                       1.  **理解**用户提出的原始研究问题。
                       2.  **理解**我为你提供的每一个研究步骤的目的、描述和预期结果。
                       3.  **确认**你已经准备好根据这份研究计划，开始指导后续的信息收集、分析和报告生成过程。
                       4.  在整个研究过程中，作为幕后的智能大脑，为每个子任务提供必要的智能支持（例如生成搜索查询、总结信息、评估结果等），但具体的执行将由外部系统（Java工作流）协调。
                       **原始研究问题：**
                       {question}
                       **以下是为你准备的详细研究计划（JSON格式，仅供参考，无需重复或评论每个步骤）：**
                       {researchPlanStepsJson}
                       你可以借助已经准备好的工具来进行研究！
                       你需要确保最后的研究报告的严谨、准确和全面！
                    """).param("question", researchQuestionDTO.getQuestion())
                                .param("researchPlanStepsJson", JSONUtil.toJsonStr(researchQuestionDTO.getResearchPlanSteps())))
                .advisors(p -> p.param(CONVERSATION_ID, researchQuestionDTO.getConversationId()))
                .tools(researchTool)
                .stream()
                .chatResponse();
//        researchQuestionDTO.getResearchPlanSteps().


        return   chatResponseFlux.flatMap(chatResponse -> {
            // flatMap 可以将一个元素转变为 0 到 N 个新元素
            // 完美适配我们的场景：一个 ChatResponse 块可能同时包含思考和内容

            // 1. 准备一个容器来存放这个块产生的所有事件
            Flux<StreamResponse> eventFlux = Flux.empty();
            chatResponse.getResult();
            String content = chatResponse.getResult().getOutput().getText();
            if (content != null && !content.isEmpty()) {
                eventFlux = eventFlux.concatWith(Flux.just(new StreamResponse("CONTENT", content)));
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