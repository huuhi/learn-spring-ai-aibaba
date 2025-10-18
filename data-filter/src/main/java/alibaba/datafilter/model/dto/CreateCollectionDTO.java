package alibaba.datafilter.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/18
 * 说明: 创建集合的参数
 */
@Data
@AllArgsConstructor
public class CreateCollectionDTO {
    private String name;
    @NotBlank
    private String collectionName;
    private String description;
    private Boolean isSystem;
//    默认简体中文
    private String language;
}
