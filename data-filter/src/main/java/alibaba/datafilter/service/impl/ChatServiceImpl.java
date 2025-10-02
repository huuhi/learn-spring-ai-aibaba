package alibaba.datafilter.service.impl;


import alibaba.datafilter.common.utils.MilvusVectorStoreUtils;
import alibaba.datafilter.model.dto.RequestDTO;
import alibaba.datafilter.model.dto.StreamResponse;
import alibaba.datafilter.service.ChatService;
import alibaba.datafilter.service.CollectionService;
import alibaba.datafilter.tools.DataFilterTool;
import cn.hutool.core.lang.UUID;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
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
    @Resource
    private   ChatClient chatClient;
    private final List<String> models=List.of("qwen-max","qwen-plus-latest","qwen3-max-2025-09-23","qwen3-max-preview",
            "qwen-plus-2025-07-28","qwen-turbo","Moonshot-Kimi-K2-Instruct","deepseek-r1","deepseek-v3");

    public ChatServiceImpl(Function<String, MilvusVectorStore> dynamicVectorStoreFactory, MilvusVectorStoreUtils milvusVectorStoreUtils, CollectionService collectionService, DataFilterTool dataFilterTool) {
        this.dynamicVectorStoreFactory = dynamicVectorStoreFactory;
        this.milvusVectorStoreUtils = milvusVectorStoreUtils;
        this.collectionService = collectionService;
        this.dataFilterTool = dataFilterTool;
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
                        回答用户问题，根据用户问题决定需不需要调用工具
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
