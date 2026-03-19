package ynu.jackielinn.server.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ynu.jackielinn.server.common.Feedback;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息响应对象。用于会话详情中的消息列表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "消息响应对象")
public class MessageVO {

    @Schema(description = "消息 ID")
    private Long id;

    @Schema(description = "角色：user / assistant")
    private String role;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "会话内顺序号")
    private Integer sequenceNum;

    @Schema(description = "参考文档来源（解析自 sources_json，Python 返回的字符串列表）")
    private List<String> sources;

    @Schema(description = "反馈：0 正常 1 点赞 -1 点踩")
    private Feedback feedback;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
