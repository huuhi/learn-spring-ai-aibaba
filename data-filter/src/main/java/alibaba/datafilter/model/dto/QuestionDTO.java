package alibaba.datafilter.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/7
 * 说明:
 */
@Data
@AllArgsConstructor
public class QuestionDTO {
    private String conversationId;
    @NotBlank(message = "问题不能为空")
    private String question;
}
