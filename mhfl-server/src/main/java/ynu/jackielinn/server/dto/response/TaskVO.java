package ynu.jackielinn.server.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ynu.jackielinn.server.common.Status;

import java.time.LocalDateTime;

/**
 * 任务信息响应对象
 * did/aid 替换为 dataName、algorithmName；保留 uid 并增加 username。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "任务信息响应对象")
public class TaskVO {

    @Schema(description = "任务ID")
    private Long id;

    @Schema(description = "创建者用户 ID")
    private Long uid;

    @Schema(description = "数据集名称（对应 did）")
    private String dataName;

    @Schema(description = "算法名称（对应 aid）")
    private String algorithmName;

    @Schema(description = "创建者用户名")
    private String username;

    @Schema(description = "客户端数量")
    private Integer numNodes;

    @Schema(description = "每轮参与比例")
    private Double fraction;

    @Schema(description = "每节点类别数")
    private Integer classesPerNode;

    @Schema(description = "low_prob")
    private Double lowProb;

    @Schema(description = "总轮次数")
    private Integer numSteps;

    @Schema(description = "每客户端训练轮数")
    private Integer epochs;

    @Schema(description = "loss")
    private Double loss;

    @Schema(description = "accuracy")
    private Double accuracy;

    @Schema(description = "precision")
    private Double precision;

    @Schema(description = "recall")
    private Double recall;

    @Schema(description = "f1Score")
    private Double f1Score;

    @Schema(description = "任务状态")
    private Status status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
