package ynu.jackielinn.server.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "创建任务请求对象")
public class CreateTaskRO {

    @NotNull(message = "数据集 ID 不能为空")
    @Schema(description = "数据集 ID")
    private Long did;

    @NotNull(message = "算法 ID 不能为空")
    @Schema(description = "算法 ID")
    private Long aid;

    @NotNull(message = "客户端数量不能为空")
    @Schema(description = "客户端数量 num_nodes")
    private Integer numNodes;

    @NotNull(message = "每轮参与比例不能为空")
    @Schema(description = "每轮参与比例 fraction")
    private Double fraction;

    @NotNull(message = "每节点类别数不能为空")
    @Schema(description = "每节点类别数 classes_per_node")
    private Integer classesPerNode;

    @NotNull(message = "low_prob 不能为空")
    @Schema(description = "low_prob")
    private Double lowProb;

    @NotNull(message = "总轮次数不能为空")
    @Schema(description = "总轮次数 num_steps")
    private Integer numSteps;

    @NotNull(message = "每客户端训练轮数不能为空")
    @Schema(description = "每客户端训练轮数 epochs")
    private Integer epochs;
}
