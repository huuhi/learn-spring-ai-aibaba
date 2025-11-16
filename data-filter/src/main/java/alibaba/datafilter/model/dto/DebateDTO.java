package alibaba.datafilter.model.dto;

import alibaba.datafilter.model.em.DebateRoles;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/31
 * 说明:
 */
@Data
public class DebateDTO {
    @NotNull(message = "conversationId不能为空")
    private String  conversationId;

    private String model="qwen-turbo";
    @NotNull(message = "role不能为空")
    private DebateRoles role;

}
