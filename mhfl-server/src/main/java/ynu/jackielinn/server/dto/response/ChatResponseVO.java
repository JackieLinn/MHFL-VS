package ynu.jackielinn.server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 聊天响应对象。Python 返回的 content 与 sources 封装。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "聊天响应对象")
public class ChatResponseVO {

    @Schema(description = "助手回复内容")
    private String content;

    @Schema(description = "参考文档来源列表")
    private List<String> sources;
}
