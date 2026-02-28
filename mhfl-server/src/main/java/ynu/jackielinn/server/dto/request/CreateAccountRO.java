package ynu.jackielinn.server.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员导入用户请求对象
 * 包含用户名、邮箱、电话号码；默认密码 123456、角色 user。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理员导入用户对象")
public class CreateAccountRO {

    @NotBlank
    @Schema(description = "用户名")
    private String username;

    @Email
    @NotBlank
    @Schema(description = "邮箱")
    private String email;

    @NotBlank
    @Schema(description = "电话号码")
    private String telephone;
}
