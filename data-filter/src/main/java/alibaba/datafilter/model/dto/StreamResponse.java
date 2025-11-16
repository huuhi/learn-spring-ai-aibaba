// StreamResponse.java
package alibaba.datafilter.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 这个 DTO 代表了我们流中的一个“事件”
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // 关键：如果字段是null，JSON序列化时就忽略它
public class StreamResponse {

    // Getters and Setters
    // 事件类型：THINKING, SEARCH_RESULT, CONTENT, ERROR 等
    private String type;
    
    // 事件承载的数据
    private Object data;
}
