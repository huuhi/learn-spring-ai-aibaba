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
    // name 非必须。
    private String name;

    /**
     * 必需
     */
    private String email;

    /**
     *非必须
     */
    private String password;

    /**
     * 非必须
     */
    private String avatar;
}
