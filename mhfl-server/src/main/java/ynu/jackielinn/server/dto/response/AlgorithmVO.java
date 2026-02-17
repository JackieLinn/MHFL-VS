package ynu.jackielinn.server.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 算法信息响应对象
 * 包含 Algorithm 的所有字段，但排除敏感信息(deleteTime、deleted)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "算法信息响应对象")
public class AlgorithmVO {

    @Schema(description = "算法ID")
    private Long id;

    @Schema(description = "算法名字")
    private String algorithmName;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
