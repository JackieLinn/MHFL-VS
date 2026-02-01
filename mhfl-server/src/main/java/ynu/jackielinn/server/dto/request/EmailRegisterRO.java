package ynu.jackielinn.server.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "邮箱注册对象")
public class EmailRegisterRO {

    @Length(min = 11, max = 11)
    @Schema(description = "电话号码")
    private String telephone;

    @Email
    @Schema(description = "邮箱")
    private String email;

    @Length(min = 6, max = 6)
    @Schema(description = "邮箱验证码")
    private String code;

    @Pattern(regexp = "^[a-zA-Z0-9\\u4e00-\\u9fa5]+$")
    @Length(min = 1, max = 30)
    @Schema(description = "用户名")
    private String username;

    @Length(min = 6, max = 20)
    @Schema(description = "密码")
    private String password;
}
