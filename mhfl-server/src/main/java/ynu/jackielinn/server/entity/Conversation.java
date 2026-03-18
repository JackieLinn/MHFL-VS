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
@TableName("conversation")
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Conversation extends BaseEntity {

    @TableField("uid")
    private Long uid;

    @TableField("title")
    private String title;

    @TableField("summary")
    private String summary;

    @TableField("message_count")
    private Integer messageCount;
}
