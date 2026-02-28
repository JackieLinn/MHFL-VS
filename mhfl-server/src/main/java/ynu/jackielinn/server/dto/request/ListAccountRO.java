package ynu.jackielinn.server.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 用户列表查询请求对象
 * 支持关键字（用户名、邮箱、电话）模糊查询、分页及创建时间范围。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "查询用户列表请求对象")
public class ListAccountRO {

    @Schema(description = "关键字（可选，支持用户名、邮箱、电话号码的模糊查询）")
    private String keyword;

    @Schema(description = "当前页码（默认1）")
    @Min(value = 1, message = "页码必须大于0")
    @Builder.Default
    private Long current = 1L;

    @Schema(description = "每页数量（默认10）")
    @Min(value = 1, message = "每页数量必须大于0")
    @Max(value = 100, message = "每页数量不能超过100")
    @Builder.Default
    private Long size = 10L;

    @Schema(description = "起始创建时间（可选，格式：yyyy-MM-dd）")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate startTime;

    @Schema(description = "终止创建时间（可选，格式：yyyy-MM-dd）")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate endTime;
}
