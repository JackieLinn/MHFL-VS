package ynu.jackielinn.server.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import ynu.jackielinn.server.common.BaseEntity;
import ynu.jackielinn.server.common.Feedback;

@Data
@TableName("message")
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Message extends BaseEntity {

    @TableField("cid")
    private Long cid;

    @TableField("role")
    private String role;

    @TableField("content")
    private String content;

    @TableField("sequence_num")
    private Integer sequenceNum;

    @TableField("sources_json")
    private String sourcesJson;

    @TableField("feedback")
    private Feedback feedback;
}
