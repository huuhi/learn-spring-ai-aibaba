package alibaba.datafilter.service;

import alibaba.datafilter.model.domain.Conversation;
import alibaba.datafilter.model.vo.ConversationVO;
import alibaba.datafilter.model.vo.DebateMessageVO;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.ai.chat.messages.Message;
import org.springframework.scheduling.annotation.Async;

import java.util.List;

/**
* @author windows
* @description 针对表【conversation】的数据库操作Service
* @createDate 2025-10-20 16:37:20
*/
public interface ConversationService extends IService<Conversation> {
    String createTitle(String question,String answer);

    @Async
    void createConversation(String title,String conversationId,Integer userId);

    List<ConversationVO> getListByUserId();

    void deleteByIds(String[] conversationIds);

    List<Message> getMessageById(String conversationId);

    List<DebateMessageVO> getDebateMessages(@NotBlank(message = "会话id不能为空") String conversationId);
}
