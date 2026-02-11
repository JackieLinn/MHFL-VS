package ynu.jackielinn.server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统资源信息响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "系统资源信息")
public class SystemResourcesVO {

    @Schema(description = "CPU信息")
    private CPUInfo cpu;

    @Schema(description = "内存信息")
    private MemoryInfo memory;

    @Schema(description = "GPU信息（可选）")
    private GPUInfo gpu;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "CPU信息")
    public static class CPUInfo {
        @Schema(description = "CPU使用率(%)")
        private Double usagePercent;

        @Schema(description = "物理核心数")
        private Integer cores;

        @Schema(description = "逻辑核心数")
        private Integer coresLogical;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "内存信息")
    public static class MemoryInfo {
        @Schema(description = "总内存(GB)")
        private Double total;

        @Schema(description = "已用内存(GB)")
        private Double used;

        @Schema(description = "剩余内存(GB)")
        private Double free;

        @Schema(description = "内存使用率(%)")
        private Double usagePercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "GPU信息")
    public static class GPUInfo {
        @Schema(description = "总显存(GB)")
        private Double total;

        @Schema(description = "已用显存(GB)")
        private Double used;

        @Schema(description = "剩余显存(GB)")
        private Double free;

        @Schema(description = "显存使用率(%)")
        private Double usagePercent;
    }
}
