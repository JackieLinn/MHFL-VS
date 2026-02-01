package ynu.jackielinn.server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户验证的对象")
public class AuthorizeVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户角色")
    private String role;

    @Schema(description = "生成的token")
    private String token;

    @Schema(description = "token过期时间")
    private Date expire;
}
