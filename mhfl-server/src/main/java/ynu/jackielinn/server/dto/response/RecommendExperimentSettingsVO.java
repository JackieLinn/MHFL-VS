package ynu.jackielinn.server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 推荐展示页-实验设置信息响应对象。
 * settings 从指定推荐任务中任选一条搬运；algorithmNames 返回算法表全部名称（按 id 升序）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "推荐展示页实验设置响应对象")
public class RecommendExperimentSettingsVO {

    @Schema(description = "数据集ID")
    private Long datasetId;

    @Schema(description = "用于搬运设置的来源任务ID，找不到时为 null")
    private Long sourceTaskId;

    @Schema(description = "客户端数量")
    private Integer numNodes;

    @Schema(description = "每轮参与比例")
    private Double fraction;

    @Schema(description = "每节点类别数")
    private Integer classesPerNode;

    @Schema(description = "Low Prob")
    private Double lowProb;

    @Schema(description = "总轮次数")
    private Integer numSteps;

    @Schema(description = "每客户端训练轮数")
    private Integer epochs;

    @Schema(description = "算法名称列表（按算法表 id 升序）")
    private List<String> algorithmNames;
}
