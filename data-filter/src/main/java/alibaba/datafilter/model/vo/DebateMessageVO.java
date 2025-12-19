package alibaba.datafilter.model.vo;

import alibaba.datafilter.model.em.DebateRoles;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/12/19
 * 说明: 辩论的会话消息
 */
@Data
@Builder
public class DebateMessageVO {
    private String content;
    private DebateRoles role;


}
