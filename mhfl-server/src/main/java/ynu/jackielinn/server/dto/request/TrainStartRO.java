package ynu.jackielinn.server.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 调用 Python FastAPI /api/train/start 的请求体，字段名与 FastAPI TrainStartRequest 一致（snake_case）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainStartRO {

    @JsonProperty("task_id")
    private Integer taskId;

    @JsonProperty("data_name")
    private String dataName;

    @JsonProperty("algorithm_name")
    private String algorithmName;

    @JsonProperty("num_nodes")
    private Integer numNodes;

    @JsonProperty("fraction")
    private Double fraction;

    @JsonProperty("classes_per_node")
    private Integer classesPerNode;

    @JsonProperty("low_prob")
    private Double lowProb;

    @JsonProperty("num_steps")
    private Integer numSteps;

    @JsonProperty("epochs")
    private Integer epochs;
}
