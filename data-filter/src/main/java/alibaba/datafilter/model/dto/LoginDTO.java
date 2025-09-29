package alibaba.datafilter.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/27
 * 说明:
 */
@Data
@AllArgsConstructor
public class LoginDTO {
    @NotBlank(message = "邮箱格式不正确")
    @Email(message = "邮箱不能为空")
    private String email;
    @NotBlank(message = "登录类型不能为空")
    private String type;// 登录方式 code/password
    private String authCode;
    private String password;
}
