package ynu.jackielinn.server.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "密码重置确认对象")
public class ConfirmResetRO {

    @Email
    @Schema(description = "邮箱")
    private String email;

    @Length(min = 6, max = 6)
    @Schema(description = "邮箱验证码")
    private String code;
}
