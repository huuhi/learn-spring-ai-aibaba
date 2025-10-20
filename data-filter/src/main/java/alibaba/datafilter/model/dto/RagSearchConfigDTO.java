package alibaba.datafilter.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/20
 * 说明:
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RagSearchConfigDTO {
    @Max(value = 30,message = "topK不能大于30")
    @Min(value = 1,message = "topK不能小于1")
    @NotNull
    private Short topK=5;
    @Max(value = 0,message = "score")
    @Max(value = 1,message = "score不能大于1")
    @NotNull
    private Double score=0.5;
    @Max(value = 0,message = "instance不能大于1")
    @Max(value = 2,message = "instance不能大于2")
    @NotNull
    private Double instance=0.6;

}
