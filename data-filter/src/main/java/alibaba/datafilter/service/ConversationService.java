package alibaba.datafilter.service;

import alibaba.datafilter.model.domain.Conversation;
import alibaba.datafilter.model.vo.ConversationVO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.http.ResponseEntity;
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

    ResponseEntity<List<ConversationVO>> getListByUserId();

}
