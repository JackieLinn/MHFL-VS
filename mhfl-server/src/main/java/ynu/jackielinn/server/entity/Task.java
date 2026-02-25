package ynu.jackielinn.server.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import ynu.jackielinn.server.common.BaseEntity;
import ynu.jackielinn.server.common.Status;

@Data
@TableName("task")
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Task extends BaseEntity {

    @TableField("uid")
    private Long uid;

    @TableField("did")
    private Long did;

    @TableField("aid")
    private Long aid;

    @TableField("num_nodes")
    private Integer numNodes;

    @TableField("fraction")
    private Double fraction;

    @TableField("classes_per_node")
    private Integer classesPerNode;

    @TableField("low_prob")
    private Double lowProb;

    @TableField("num_steps")
    private Integer numSteps;

    @TableField("epochs")
    private Integer epochs;

    @TableField("loss")
    private Double loss;

    @TableField("accuracy")
    private Double accuracy;

    @TableField("`precision`")
    private Double precision;

    @TableField("recall")
    private Double recall;

    @TableField("f1_score")
    private Double f1Score;

    @TableField("status")
    private Status status;
}
