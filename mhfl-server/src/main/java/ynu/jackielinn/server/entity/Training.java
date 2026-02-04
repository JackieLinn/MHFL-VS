package ynu.jackielinn.server.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import ynu.jackielinn.server.common.BaseEntity;

import java.time.LocalDateTime;

@Data
@TableName("training")
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Training extends BaseEntity {

    @TableField("tid")
    private Long tid;

    @TableField("client_id")
    private Long clientId;

    @TableField("round_num")
    private Long roundNum;

    @TableField("loss")
    private Double loss;

    @TableField("accuracy")
    private Double accuracy;

    @TableField("precision")
    private Double precision;

    @TableField("recall")
    private Double recall;

    @TableField("f1_score")
    private Double f1Score;

    @TableField("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime timestamp;
}
