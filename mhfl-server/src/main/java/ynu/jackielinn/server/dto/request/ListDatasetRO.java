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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "查询数据集列表请求对象")
public class ListDatasetRO {

    @Schema(description = "关键字（可选，支持数据集名字的模糊查询）")
    private String keyword;

    @Schema(description = "当前页码（默认1）")
    @Min(value = 1, message = "页码必须大于0")
    @Builder.Default
    private Long current = 1L;

    @Schema(description = "每页数量（默认10），如果 all=true 则忽略此参数")
    @Min(value = 1, message = "每页数量必须大于0")
    @Max(value = 100, message = "每页数量不能超过100")
    @Builder.Default
    private Long size = 10L;

    @Schema(description = "是否返回全部结果（不分页），默认 false")
    @Builder.Default
    private Boolean all = false;

    @Schema(description = "起始创建时间（可选，格式：yyyy-MM-dd）")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate startTime;

    @Schema(description = "终止创建时间（可选，格式：yyyy-MM-dd）")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate endTime;
}
