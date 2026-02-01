package ynu.jackielinn.server.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 性别枚举
 * 0: 未知性别
 * 1: 男
 * 2: 女
 */
@Getter
@AllArgsConstructor
public enum Gender {

    UNKNOWN(0, "Unknown"),
    MALE(1, "Male"),
    FEMALE(2, "Female");

    @EnumValue
    @JsonValue
    private final int code;

    private final String description;

    /**
     * 根据 code 获取对应的 Gender 枚举
     *
     * @param code 性别代码
     * @return 对应的 Gender 枚举，如果不存在则返回 UNKNOWN
     */
    public static Gender fromCode(int code) {
        for (Gender gender : Gender.values()) {
            if (gender.code == code) {
                return gender;
            }
        }
        return UNKNOWN;
    }
}
