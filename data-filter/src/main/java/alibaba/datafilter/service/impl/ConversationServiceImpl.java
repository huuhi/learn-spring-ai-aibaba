package alibaba.datafilter.service.impl;

import alibaba.datafilter.model.vo.ConversationVO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import alibaba.datafilter.model.domain.Conversation;
import alibaba.datafilter.service.ConversationService;
import alibaba.datafilter.mapper.ConversationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static alibaba.datafilter.common.content.RedisConstant.TEMP_USER_ID;

/**
* @author windows
* @description 针对表【conversation】的数据库操作Service实现
* @createDate 2025-10-20 16:37:20
*/
@Service
@Slf4j
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation>
    implements ConversationService{
    private final ChatClient chatClient;
    private final MessageWindowChatMemory messageWindowChatMemory;
    private final ConversationMapper conversationMapper;

    public ConversationServiceImpl(ChatClient.Builder chatClient, MessageWindowChatMemory messageWindowChatMemory, ConversationMapper conversationMapper) {
        this.chatClient = chatClient.defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultSystem("请根据对话内容生成简洁标题，最多100个字符")
                .build();
        this.messageWindowChatMemory = messageWindowChatMemory;
        this.conversationMapper = conversationMapper;
    }


    @Override
    public String createTitle(String question, String answer) {
        try {
            return chatClient.prompt()
                    .user("""
                            问题：%s
                            回答：%s
                            """.formatted(truncateText(question,200), truncateText(answer,1000)))
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("创建标题失败:{}", e.getMessage());
//            将问题的前20个字符作为标题返回
            return truncateTitle(question);
        }
    }

    @Override
    @Async
    public void createConversation(String title, String conversationId, Integer userId) {
        try {
            if (title == null || conversationId == null || userId == null) {
                log.warn("创建会话参数不能为空: title={}, conversationId={}, userId={}",
                        title, conversationId, userId);
                return;
            }

            Conversation conversation = Conversation.builder()
                    .title(title)
                    .userId(userId)
                    .id(conversationId)
                    .build();
            save(conversation);
        } catch (Exception e) {
            log.error("创建会话失败: title={}, conversationId={}, userId={}",
                    title, conversationId, userId, e);
        }
    }

    @Override
    public List<ConversationVO> getListByUserId() {
//        TODO 之后用户id需要在线程中获取
        List<ConversationVO> list = lambdaQuery()
                .eq(Conversation::getUserId, TEMP_USER_ID)
                .orderByDesc(Conversation::getUpdatedAt)
                .list()
                .stream()
                .map(c -> ConversationVO.builder()
                        .id(c.getId()).title(c.getTitle()).updateTime(dateToString(c.getUpdatedAt())).build())
                .toList();
        if (list.isEmpty()) {
            return List.of();
        }
        return list;
    }

    @Override
    @Transactional
    public void deleteByIds(String[] conversationIds) {
        try {
            conversationMapper.deleteBatchIds(Arrays.asList(conversationIds));
        } catch (Exception e) {
            log.error("删除会话失败:{}", e.getMessage());
        }
        try {
            for (String conversationId : conversationIds) {
                log.info("id长度：{}",conversationId.length());
                messageWindowChatMemory.clear(conversationId);
            }
        } catch (Exception e) {
            throw new RuntimeException("聊天记录删除失败");
        }
    }

    @Override
    public List<Message> getMessageById(String conversationId) {
//        判断会话是否属于当前用户
        Conversation one = lambdaQuery()
                .eq(Conversation::getId, conversationId)
                .eq(Conversation::getUserId, TEMP_USER_ID)
                .one();
        if (one==null){
            return List.of();
        }
        return messageWindowChatMemory.get(conversationId);
    }

    private String truncateTitle(String title) {
        return title.length() > 20 ? title.substring(0, 20) : title;
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
    private String dateToString(LocalDate date){
//        如果是今天返回今天
        if(date.isEqual(LocalDate.now())){
            return "今天";
        }
//        如果是昨天
        if(date.isEqual(LocalDate.now().minusDays(1))){
            return "昨天";
        }
//        如果是七天内
        if(date.isAfter(LocalDate.now().minusDays(7))){
            return "7天内";
        }
        if(date.isAfter(LocalDate.now().minusDays(30))){
            return "30天内";
        }
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

}




