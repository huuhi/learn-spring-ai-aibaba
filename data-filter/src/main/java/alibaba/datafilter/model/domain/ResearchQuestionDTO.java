package alibaba.datafilter.model.domain;

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
    private String question;
    private List<ResearchPlanStep> researchPlanSteps;
    private String conversationId;
}
