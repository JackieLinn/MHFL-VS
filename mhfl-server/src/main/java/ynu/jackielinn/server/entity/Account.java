package ynu.jackielinn.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Data
@TableName("account")
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Account extends BaseEntity {

    @TableField("username")
    private String username;

    @TableField("password")
    private String password;

    @TableField("gender")
    private Integer gender;

    @TableField("email")
    private String email;

    @TableField("telephone")
    private String telephone;

    @TableField("avatar")
    private String avatar;

    @TableField("role")
    private String role;

    @TableField("birthday")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate birthday;
}
