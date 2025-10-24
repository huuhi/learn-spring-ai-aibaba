package alibaba.datafilter.model.em;

import lombok.Getter;

import static alibaba.datafilter.common.content.PromptConstant.*;

public enum QuestionType {
    // 1. 知识解答型 - 需要准确性和深度
    KNOWLEDGE_QUESTIONS(KNOWLEDGE_PROMPT),      // "什么是量子计算？" "明朝历史概况"
    
    // 2. 操作指导型 - 需要步骤清晰和实用性  
    GUIDANCE_QUESTIONS(GUIDANCE_PROMPT),       // "如何配置Python环境？" "怎样修复这个错误？"
    
    // 3. 创意生成型 - 需要创造性和启发性
    CREATIVE_QUESTIONS(CREATIVE_PROMPT),       // "写个产品介绍文案" "帮我想个活动策划"
    
    // 4. 个人交互型 - 需要温度和同理心
    PERSONAL_QUESTIONS(PERSONAL_PROMPT)        // "我今天做了什么？" "我该选哪个方案？"
    ;

    @Getter
    private final String value;
    QuestionType(String value) {
        this.value = value;
    }
}