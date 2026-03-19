package ynu.jackielinn.server.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新会话标题请求对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "更新会话标题请求对象")
public class UpdateTitleRO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 255)
    @Schema(description = "会话标题")
    private String title;
}
