package alibaba.datafilter.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/26
 * 说明:
 */
@Data
@AllArgsConstructor
public class UserDTO {
    private Integer id;
    // name 非必须。
    private String name;
}
