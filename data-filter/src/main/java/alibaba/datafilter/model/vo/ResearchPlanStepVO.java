package alibaba.datafilter.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/9
 * 说明:
 */
@Data
@AllArgsConstructor
public class ResearchPlanStepVO implements Serializable {
    private String id; // 新增：唯一标识符，方便在前端或后续流程中识别和操作某个步骤
    private Boolean completed;
    @Serial
    private static final long serialVersionUID = 1L;
}
