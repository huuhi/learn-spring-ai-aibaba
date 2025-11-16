package alibaba.datafilter.utils;

import alibaba.datafilter.model.dto.StreamResponse;
import alibaba.datafilter.service.ConversationService;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static alibaba.datafilter.common.content.RedisConstant.TEMP_USER_ID;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/31
 * 说明:
 */
@Component
public class FluxUtils {
    private final ConversationService conversationService;

    public FluxUtils(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @NotNull
    public  Flux<StreamResponse> getStreamResponseFlux(Flux<ChatResponse> chatResponseFlux, String question, String conversationId, boolean isNewConversation) {
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

                    }
                    else if(signal.isOnComplete()){
                        return Flux.just(new StreamResponse("END", "流式传输完成"));
                    }
                    return Flux.empty();
                });
    }
}
