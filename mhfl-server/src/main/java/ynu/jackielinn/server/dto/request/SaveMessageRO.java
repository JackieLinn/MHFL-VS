package ynu.jackielinn.server.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ynu.jackielinn.server.common.Feedback;

/**
 * 保存消息请求对象。用于内部保存 user/assistant 消息，不直接使用数据库实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "保存消息请求对象（内部使用）")
public class SaveMessageRO {

    @NotNull(message = "会话 ID 不能为空")
    @Schema(description = "会话 ID")
    private Long cid;

    @NotNull(message = "角色不能为空")
    @Schema(description = "角色：user / assistant")
    private String role;

    @NotNull(message = "内容不能为空")
    @Schema(description = "消息内容")
    private String content;

    @NotNull(message = "顺序号不能为空")
    @Schema(description = "会话内顺序号")
    private Integer sequenceNum;

    @Schema(description = "参考来源 JSON，assistant 消息时使用")
    private String sourcesJson;

    @Builder.Default
    @Schema(description = "反馈状态，默认 NONE")
    private Feedback feedback = Feedback.NONE;
}
