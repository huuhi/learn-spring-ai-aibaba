package alibaba.datafilter.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/18
 * 说明: 上传文件到知识库的配置DTO实体类
 */
@Data
@AllArgsConstructor
public class UploadFileConfigDTO {
    @NotBlank
    private String collectionName;
    private String description;
//    最大4000
    @NotNull
    @Max(value = 4000, message = "chunkSize最大值不能超过4000")
    private Integer chunkSize;
//    最大800
    @NotNull
    @Max(value = 800, message = "chunkOverlap最大值不能超过800")
    private Integer chunkOverlap;
//    分割符列表
    @NotEmpty
    private String[] separators;


}
