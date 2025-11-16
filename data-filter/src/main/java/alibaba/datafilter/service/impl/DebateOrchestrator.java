package alibaba.datafilter.service.impl;

import alibaba.datafilter.model.dto.StreamResponse;
import alibaba.datafilter.model.em.DebateRoles;
import alibaba.datafilter.utils.FluxUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DebateOrchestrator {
    private final FluxUtils fluxUtils;
    private final ChatClient.Builder chatClientBuilder;
    private final MessageWindowChatMemory chatMemory; // 注入你配置好的、带SQLite后端的ChatMemory

    /**
     * 开始一场新的辩论
     * @param topic 辩题
     * @param proSidePersona 正方的人设 Prompt
     * @param conSidePersona 反方的人设 Prompt
     * @return 新的辩论 conversationId
     */
    public String startDebate(String topic, String proSidePersona, String conSidePersona) {
        String conversationId = UUID.randomUUID().toString();
        
        // 我们可以用第一条 SYSTEM 消息来存储辩题和人设，方便回顾
        String initialSystemMessage = String.format(
            "辩论开始！辩题是：%s\n\n正方人设：%s\n\n反方人设：%s\n\n现在由正方首先发言。",
            topic, proSidePersona, conSidePersona
        );
        
        // 将初始指令存入记忆，角色为裁判（SYSTEM）
        chatMemory.add(conversationId, new SystemMessage(initialSystemMessage));
        log.info("新辩论 [{}] 已创建，辩题: {}", conversationId, topic);
        return conversationId;
    }

    /**
     * 裁判发言
     * @param conversationId 辩论ID
     * @param judgeSpeech 裁判的发言内容
     */
    public void judgeSpeaks(String conversationId, String judgeSpeech) {
        log.info("辩论 [{}]: 裁判发言: {}", conversationId, judgeSpeech);
        // 将裁判的发言，以 SYSTEM 角色存入记忆
        chatMemory.add(conversationId, new SystemMessage("【裁判指示】：" + judgeSpeech));
    }

    /**
     * 驱动辩论进行下一回合
     * @param conversationId 辩论ID
     * @param nextSpeakerRole 下一个发言者的角色 (DebateRoles.DEBATER_PRO 或 DebateRoles.DEBATER_CON)
     * @return AI的流式回答
     */
    public Flux<StreamResponse> nextTurn(String conversationId, String nextSpeakerRole) {
        log.info("辩论 [{}]: 轮到 {} 发言", conversationId, nextSpeakerRole);
        
        // 1. 获取完整的、包含所有角色发言的历史记录
        List<Message> fullHistory = chatMemory.get(conversationId); // 获取所有历史记录

        // 2. 构建本次请求的 ChatClient
        // 我们需要为每个辩手动态设定 System Prompt，告诉它“你是谁”
        String systemPrompt;
        if (nextSpeakerRole.equals(DebateRoles.DEBATER_PRO.getValue())) {
            systemPrompt = "你现在是正方辩手。你的任务是根据对话历史，进行立论、反驳或总结。你的发言将作为 'USER' 记录。";
        } else { // DEBATER_CON
            systemPrompt = "你现在是反方辩手。你的任务是根据对话历史，进行立论、反驳或总结。你的发言将作为 'ASSISTANT' 记录。";
        }
        
        ChatClient chatClient = chatClientBuilder.build();
        
        // 3. 构建 Prompt
        // 我们把完整的历史记录都喂给AI，让它自己理解上下文
        // Spring AI 会自动处理 List<Message>，将它们转换成模型需要的格式
        Prompt prompt = new Prompt(fullHistory,
            ChatOptions.builder().model("model").build()
        );
//       我靠，这个插件是真的好用啊！



        Flux<ChatResponse> chatResponseFlux = chatClient.prompt(prompt)
                .system(systemPrompt)
                .stream().chatResponse();
        return fluxUtils.getStreamResponseFlux(chatResponseFlux,null,conversationId,false);
    }
}