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
@TableName("algorithm")
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Algorithm extends BaseEntity {

    @TableField("algorithm_name")
    private String algorithmName;
}
