package ynu.jackielinn.server.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话列表项。用于侧边栏展示，仅包含有消息的会话（message_count > 0）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会话列表项")
public class ConversationVO {

    @Schema(description = "会话 ID")
    private Long id;

    @Schema(description = "会话标题")
    private String title;

    @Schema(description = "最后一条消息预览")
    private String preview;

    @Schema(description = "最后更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    @Schema(description = "消息总数")
    private Integer messageCount;
}
