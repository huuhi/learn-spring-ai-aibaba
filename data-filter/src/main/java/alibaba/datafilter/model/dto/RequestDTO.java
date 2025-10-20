package alibaba.datafilter.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/25
 * 说明:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestDTO {
//    用户的问题
    @NotBlank
    private String question;
//    记忆ID,需要根据这个来获取历史会话

    private String conversationId;
//    指定的默认，默认是qwen-plus-latest
    private String model;
//    是否开启搜索
    private Boolean enableSearch;
//    是否开启思考
    private Boolean enableThinking;
    // rag 知识库的名称！
    private String rag;

//    自动查询知识库
    private Boolean autoRag;

    private RagSearchConfigDTO  ragSearchConfig;

}
