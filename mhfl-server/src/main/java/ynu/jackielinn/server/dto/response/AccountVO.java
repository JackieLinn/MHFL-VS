package ynu.jackielinn.server.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ynu.jackielinn.server.common.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户信息响应对象
 * 包含 Account 的所有字段，但排除敏感信息(password、deleteTime、deleted)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户信息响应对象")
public class AccountVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "性别")
    private Gender gender;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "电话号码")
    private String telephone;

    @Schema(description = "头像路径")
    private String avatar;

    @Schema(description = "用户角色")
    private String role;

    @Schema(description = "生日")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate birthday;

    @Schema(description = "年龄（根据生日与当前日期计算，生日为空则为空）")
    private Integer age;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
