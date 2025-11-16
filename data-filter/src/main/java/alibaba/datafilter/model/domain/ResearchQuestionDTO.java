package alibaba.datafilter.model.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/8
 * 说明:
 */
@Data
@AllArgsConstructor
public class ResearchQuestionDTO {
    @NotBlank(message = "问题不能为空")
    private String question;
    @NotNull(message = "研究计划不能为空")
    private List<ResearchPlanStep> researchPlanSteps;
    private String conversationId;
}
