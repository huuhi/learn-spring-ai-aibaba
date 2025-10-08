package alibaba.datafilter.model.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/7
 * 说明: 研究方案实体类
 */
@Data
@AllArgsConstructor
public class ResearchPlanStep {
    private String id; // 新增：唯一标识符，方便在前端或后续流程中识别和操作某个步骤
    // 可以由AI生成或后端生成（如UUID.randomUUID().toString()）
    private Integer priority; // 优先级，表示该步骤在整体研究中的执行顺序（1为最高优先级）

    private String title; // 建议修改：将 'plan' 改为 'title'，更清晰地表示步骤的名称
    // 例如："理解市场背景", "分析主要竞争者"

    private String description; // 详细说明：该步骤的目的、需要完成的具体任务、需要关注的重点或难点。

    // 新增字段：
    private String expectedOutcome; // 预期结果或产出：该步骤完成后，应该得到什么具体的信息、数据、结论或文档。
    // 例如："一份总结了市场规模、增长率及主要趋势的报告", "一份包含主要竞品及其核心优势的列表"

//    是否 完成
    private Boolean completed;


}
