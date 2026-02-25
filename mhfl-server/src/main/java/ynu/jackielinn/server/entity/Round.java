package ynu.jackielinn.server.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import ynu.jackielinn.server.common.BaseEntity;

@Data
@TableName("round")
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Round extends BaseEntity {

    @TableField("tid")
    private Long tid;

    @TableField("round_num")
    private Integer roundNum;

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
}
