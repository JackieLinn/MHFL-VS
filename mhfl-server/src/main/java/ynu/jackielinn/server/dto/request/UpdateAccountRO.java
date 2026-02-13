package ynu.jackielinn.server.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ynu.jackielinn.server.common.Gender;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "更新用户信息对象")
public class UpdateAccountRO {

    @Schema(description = "用户名（可选，不传则不更新）")
    private String username;

    @Schema(description = "性别（可选，不传则不更新）")
    private Gender gender;

    @Schema(description = "电话号码（可选，不传则不更新）")
    private String telephone;

    @Schema(description = "生日（可选，不传则不更新）")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate birthday;
}
