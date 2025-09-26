// StreamResponse.java
package alibaba.datafilter.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

// 这个 DTO 代表了我们流中的一个“事件”
@JsonInclude(JsonInclude.Include.NON_NULL) // 关键：如果字段是null，JSON序列化时就忽略它
public class StreamResponse {

    // 事件类型：THINKING, SEARCH_RESULT, CONTENT, ERROR 等
    private String type;
    
    // 事件承载的数据
    private Object data;

    // 构造函数
    public StreamResponse(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
