package ynu.jackielinn.server.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

/**
 * 邮箱重置密码请求对象
 * 包含邮箱、验证码、新密码，用于 /auth/reset-password。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "重置邮件对象")
public class EmailResetRO {

    @Email
    @Schema(description = "邮箱")
    private String email;

    @Length(min = 6, max = 6)
    @Schema(description = "邮箱验证码")
    private String code;

    @Length(min = 6, max = 20)
    @Schema(description = "密码")
    private String password;
}
