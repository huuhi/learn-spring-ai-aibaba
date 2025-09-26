package alibaba.toolstructured.entity;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 职位信息表
 * @TableName jobs
 */
@Data
public class Jobs implements Serializable {

    private Long id;

    /**
     * 职位名称
     */
    private String title;

    /**
     * 职位描述
     */
    private String description;

    /**
     * 职业前景
     */
    private String outlook;


    private Short type;


    /**
     * 薪资范围(如:15k-30k)
     */
    private String salary;

        /**
         * 所需技能(JSON数组格式)
         */
//    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> skills;

//    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> personalityTraits;
//    日常工作内容
    private String dailyTasks;
//    职业发展方向
    private String  careerGrowth;
//    *被AI/自动化替代的 风险*
    private String automationRisk;
    //风险解释
    private String riskExplanation;

    @Serial
    private static final long serialVersionUID = 1L;
}