package ynu.jackielinn.server.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新会话请求对象。用于内部更新 conversation 的 message_count、title 等字段。
 * 仅更新非 null 字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "更新会话请求对象（内部使用）")
public class UpdateConversationRO {

    @NotNull(message = "会话 ID 不能为空")
    @Schema(description = "会话 ID")
    private Long id;

    @Schema(description = "消息总数")
    private Integer messageCount;

    @Schema(description = "会话标题")
    private String title;

    @Schema(description = "历史摘要（步骤 15）")
    private String summary;
}
