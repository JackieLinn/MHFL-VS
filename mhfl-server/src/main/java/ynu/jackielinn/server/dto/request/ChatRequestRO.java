package ynu.jackielinn.server.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天请求对象。cid 为空时由后端自动创建或复用空会话。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "聊天请求对象")
public class ChatRequestRO {

    @Schema(description = "会话 ID，为空时自动创建或复用空会话")
    private Long cid;

    @NotBlank(message = "消息内容不能为空")
    @Size(min = 1, max = 16000)
    @Schema(description = "用户消息内容")
    private String message;
}
