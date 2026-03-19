package ynu.jackielinn.server.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ynu.jackielinn.server.common.Feedback;

/**
 * 消息反馈请求对象。用于点赞/点踩/取消反馈。
 * feedback: 0 正常（取消反馈）、1 点赞、-1 点踩
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "消息反馈请求对象")
public class FeedbackRO {

    @NotNull(message = "反馈类型不能为空")
    @Schema(description = "反馈类型：0 正常 1 点赞 -1 点踩", allowableValues = {"0", "1", "-1"})
    private Integer feedback;

    /**
     * 转换为 Feedback 枚举，非法值返回 NONE。
     */
    public Feedback toFeedback() {
        return Feedback.fromCode(feedback != null ? feedback : 0);
    }
}
