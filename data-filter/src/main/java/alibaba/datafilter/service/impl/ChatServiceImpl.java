package alibaba.datafilter.service.impl;


import alibaba.datafilter.model.domain.ResearchPlanStep;
import alibaba.datafilter.model.domain.ResearchQuestionDTO;
import alibaba.datafilter.model.dto.QuestionDTO;
import alibaba.datafilter.model.dto.RequestDTO;
import alibaba.datafilter.model.dto.StreamResponse;
import alibaba.datafilter.service.ChatService;
import alibaba.datafilter.service.CollectionService;
import alibaba.datafilter.service.ConversationService;
import alibaba.datafilter.tools.DataFilterTool;
import alibaba.datafilter.tools.RagTool;
import alibaba.datafilter.tools.ResearchTool;
import alibaba.datafilter.tools.YtDlpHelper;
import alibaba.datafilter.utils.RagUtils;
import cn.hutool.core.lang.UUID;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import static alibaba.datafilter.common.content.RedisConstant.TEMP_USER_ID;
import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/25
 * 说明:
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final CollectionService collectionService;
    private final DataFilterTool dataFilterTool;
    private final ResearchTool researchTool;
    private final RagTool ragTool;
    private final ConversationService conversationService;
    private final RagUtils ragUtils;
    private final YtDlpHelper YtDleTool;
    @Resource
    private   ChatClient chatClient;
    private final List<String> models=List.of("qwen-max","qwen-plus-latest","qwen3-max-2025-09-23","qwen3-max-preview",
            "qwen-plus-2025-07-28","qwen-turbo","Moonshot-Kimi-K2-Instruct","deepseek-r1","deepseek-v3");


//    垃圾接口，直接报废！
    public String createConversation() {
        return UUID.fastUUID().toString();
    }

    @Override
    public Flux<StreamResponse> chat(RequestDTO requestDTO) {
//        如果会话ID为null则是新会话
        boolean isNewConversation = requestDTO.getConversationId() == null || requestDTO.getConversationId().isEmpty();
        final String conversationId  =isNewConversation ? createConversation():requestDTO.getConversationId();

        DashScopeChatOptions.DashscopeChatOptionsBuilder dashscopeChatOptionsBuilder = DashScopeChatOptions.builder()
                .withEnableSearch(requestDTO.getEnableSearch())
                .withEnableThinking(requestDTO.getEnableThinking());
        String question = requestDTO.getQuestion();
//        模型不为null，不为空，并且在模型列表中
        if(requestDTO.getModel()!=null&& !requestDTO.getModel().isEmpty() && models.contains(requestDTO.getModel())){
            dashscopeChatOptionsBuilder.withModel(requestDTO.getModel());
        }
        String collectionName = requestDTO.getRag();
//        如果是自动检索知识库

//        TODO 用户id需要在线程中获取
        //        知识库检索的内容
        String searchContent="";
//        获取知识库名称
//       判断用户是否开启了rag检索，如果开启需要想判断知识库是否存在
//        TODO 判断知识库是否存在，如果不存在不需要 检索。直接在数据库判断
        if(collectionName!=null&& !collectionName.isEmpty()&&collectionService.isContains(collectionName)!=null){
            searchContent=ragUtils.ragSearch(question, collectionName,requestDTO.getRagSearchConfig());
        }
        String prompt=String.format("""
                你是一个智能的AI小助手
                你可以按需使用系统提供的工具，如果工具需要使用浏览器，默认使用edge
                知识库检索的结果:%s,注意:知识库的内容可能为空，如果为空并且提供了工具则说明，需要你自主决定调用工具获取知识库内容
                用户的id：%s,知识库检索配置：%s
                如知识库内容为空并且没有工具则说明：用户没有开启知识库检索或者知识库没有检索的内容，需要你直接回答用户的问题""",searchContent,TEMP_USER_ID,requestDTO.getRagSearchConfig());
        ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                .system(prompt)
                .user(question)
                .advisors(p -> p.param(CONVERSATION_ID,conversationId ))
                .tools(YtDleTool)
                .options(dashscopeChatOptionsBuilder.build());
        if(collectionName==null&&requestDTO.getAutoRag()){
//            注册一个工具给AI使用
            spec = spec.tools(ragTool);
            log.info("用户开启了自动检索知识库");
        }
        Flux<ChatResponse> responseFlux = spec.stream().chatResponse();
        return getStreamResponseFlux(responseFlux,question,conversationId,isNewConversation);
    }

    @Override
    public Flux<StreamResponse> dataFilterSearch(String query, String conversationId) {
        boolean isNewConversation = conversationId == null || conversationId.isEmpty();
        final String isConversationId  =isNewConversation ? createConversation():conversationId;
        Flux<ChatResponse> chatResponseFlux = chatClient.prompt()
                .system("""
                        你是一个智能助手，能够回答用户问题，并根据需要灵活调用工具。
                       **通用工具调用规则：** 仔细分析用户问题，如果回答需要外部信息或特定功能（如数据过滤），请调用相应的工具。
                       **关于时间工具的特殊规则：** 如果用户的问题涉及**当前日期、时间**（例如：“现在几点？”、“今天是什么日子？”、“当前时间？”），
                       或者为了准确回答用户问题需要**实时时间信息**（例如：“今天有什么新闻？”），而用户未提供明确的日期或时间信息时，请调用你可用的工具来获取当前时间。
                       """)
//                .options(DashScopeChatOptions.builder().withEnableThinking(true).build())
                .advisors(p -> p.param(CONVERSATION_ID,isConversationId))
                .user(query)
                .tools(dataFilterTool)
                .stream()
                .chatResponse();
        return getStreamResponseFlux(chatResponseFlux,query , isConversationId, isNewConversation);
    }

    @Override
    public ResponseEntity<List<?>> developPlan(QuestionDTO question) {
        boolean isNewConversation = question.getConversationId() == null || question.getConversationId().isEmpty();
        final String conversationId  =isNewConversation ? createConversation(): question.getConversationId();
        ChatClient.CallResponseSpec responseSpec = chatClient.prompt()
                .system("""
                         你是一位资深的领域研究专家和研究方案制定者。你的任务是根据用户提出的原始研究问题，制定一份详细、可执行、有逻辑顺序的深度研究方案。
                        请将研究方案拆解为多个独立的步骤，并以JSON数组的形式返回。每个步骤应包含以下字段：
                        - `id`: 字符串类型，为该步骤生成的唯一标识符（例如："step-1"）。
                        - `priority`: 整数类型，表示该步骤在整体研究中的执行顺序（1为最高优先级）。
                        - `title`: 字符串类型，简洁明了的研究步骤标题。
                        - `description`: 字符串类型，详细说明该步骤的目的、需要完成的具体任务和需要关注的重点。
                        - `expectedOutcome`: 字符串类型，明确该步骤完成后应该得到什么具体的结果、信息或产出。
                        """)
                .user(u -> u.text(question.getQuestion()))
                .advisors(p->p.param(CONVERSATION_ID,conversationId))
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
        boolean isNewConversation = researchQuestionDTO.getConversationId() == null || researchQuestionDTO.getConversationId().isEmpty();
        final String conversationId  =isNewConversation ? createConversation(): researchQuestionDTO.getConversationId();
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
//                存储多个数据，长度可变
                .user(researchQuestionDTO.getResearchPlanSteps().toString())
                .advisors(p -> p.param(CONVERSATION_ID, researchQuestionDTO.getConversationId()))
                .options(DashScopeChatOptions.builder().withEnableThinking(true).build())
                .tools(researchTool)
                .stream()
                .chatResponse();
//        researchQuestionDTO.getResearchPlanSteps().
        return getStreamResponseFlux(chatResponseFlux,researchQuestionDTO.getQuestion(), conversationId, isNewConversation);

    }

    @NotNull
    private Flux<StreamResponse> getStreamResponseFlux(Flux<ChatResponse> chatResponseFlux,String question, String conversationId, boolean isNewConversation) {
//        首先
        StringBuilder answer=new StringBuilder();

        return chatResponseFlux.flatMap(chatResponse->{
            Flux<StreamResponse> eventFlux = Flux.empty();
            if(!chatResponse.getResults().isEmpty()){
                Map<String, Object> metadata = chatResponse.getResults().get(0).getOutput().getMetadata();
                Flux<StreamResponse> updatedEventFlux = eventFlux;
                if(metadata.containsKey("reasoningContent")){
                    Object reasoning = metadata.get("reasoningContent");
                    if(reasoning != null && !reasoning.toString().isEmpty()){
                        updatedEventFlux = updatedEventFlux.concatWith(Flux.just(new StreamResponse("THINKING", reasoning)));
                    }
                }
                String text = chatResponse.getResult().getOutput().getText();
                if (text != null && !text.isEmpty()){
                    updatedEventFlux = updatedEventFlux.concatWith(Flux.just(new StreamResponse("CONTENT", text)));
                    answer.append(text);
                }
                return updatedEventFlux;
            }
            return eventFlux;
        }).materialize()
                .flatMap(signal->{
                    if (signal.isOnNext()){
                        return Flux.just(Objects.requireNonNull(signal.get()));
                    }else if(signal.isOnComplete()&& isNewConversation && !answer.isEmpty()){
                        Flux<StreamResponse> endEvents = Flux.just(
                                new StreamResponse("END", "流式传输完成")
                        );
                        String title = conversationService.createTitle(question, answer.toString());
                        conversationService.createConversation(title,conversationId,TEMP_USER_ID);
                        return Flux.concat(
                                Flux.just(new StreamResponse("TITLE", title)),
                                Flux.just(new StreamResponse("CONVERSATION_ID", conversationId)),
                                endEvents
                        );

                    }else if(signal.isOnComplete()){
                        return Flux.just(new StreamResponse("END", "流式传输完成"));
                    }
                    return Flux.empty();
                });
    }

}