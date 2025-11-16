package alibaba.datafilter.service.impl;

import alibaba.datafilter.model.dto.DebateDTO;
import alibaba.datafilter.model.dto.StreamResponse;
import alibaba.datafilter.model.em.DebateRoles;
import alibaba.datafilter.service.DebateService;
import cn.hutool.core.lang.UUID;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/11/16
 * 说明:
 */
@Service
public class DebateServiceImpl implements DebateService {
    private final ChatClient chatClient;
    private final MessageWindowChatMemory messageWindowChatMemory;


    public DebateServiceImpl(ChatClient.Builder chatClientBuilder, MessageWindowChatMemory messageWindowChatMemory) {
        this.chatClient = chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
        this.messageWindowChatMemory = messageWindowChatMemory;
    }

    @Override
    public String startDebate(String topic) {
        String conversationId = UUID.fastUUID().toString();
        Message systemMessage= SystemMessage.builder().text("以%s为主题开始辩论！".formatted(topic)).build();
        messageWindowChatMemory.add(conversationId, List.of(systemMessage));
        return conversationId;
    }

    @Override
    public Flux<StreamResponse> debate(DebateDTO debateDTO) {
        String conversationId = debateDTO.getConversationId();
        String systemMessage;
        boolean isPro=debateDTO.getRole()== DebateRoles.DEBATER_PRO;
        if (isPro){
            systemMessage= "你是正方辩手，你的任务是根据对话历史，进行立论、反驳或总结。输的条件：你认为你无法进行立论、反驳你可以认输，然后再输出自己的观点或者，裁判认定你输了。你的发言将作为 'USER' 角色记录。";
        }else{
            systemMessage= "你是反方辩手，你的任务是根据对话历史，进行立论、反驳或总结。输的条件：你认为你无法进行立论、反驳你可以认输，然后再输出自己的观点或者，裁判认定你输了。你的发言将作为 'ASSISTANT' 角色记录。";
        }
        List<Message> messages = messageWindowChatMemory.get(conversationId);
        messages.add(SystemMessage.builder().text(systemMessage).build());
        Prompt prompt = new Prompt(messages, ChatOptions.builder().model(debateDTO.getModel()).build());
        StringBuilder answer=new StringBuilder();
        Flux<ChatResponse> chatResponseFlux= chatClient
                .prompt(prompt)
                .stream()
                .chatResponse();
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
                    }else if(signal.isOnComplete()){
//                        添加记忆！
                        messageWindowChatMemory.add(conversationId, List.of(isPro?new UserMessage(answer.toString()):new AssistantMessage(answer.toString())));
                        return Flux.concat(
                                Flux.just(new StreamResponse("NEXT_ROLE", isPro?DebateRoles.DEBATER_OPP:DebateRoles.DEBATER_PRO)),
                                Flux.just(new StreamResponse("END", "流式传输完成"))
                        );
                    }
                    return Flux.empty();
                });
    }

    @Override
    public void judgeSpeaks(String conversationId, String message) {
        Message system= SystemMessage.builder().text(message).build();
        messageWindowChatMemory.add(conversationId, List.of(system));
    }
}
