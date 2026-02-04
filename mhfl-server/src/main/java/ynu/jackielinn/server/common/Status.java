package ynu.jackielinn.server.common;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务状态枚举
 * 0: 未知
 * 1: 进行中
 * 2: 成功
 * 3: 推荐
 * 4: 失败
 */
@Getter
@AllArgsConstructor
public enum Status {

    UNKNOWN(0, "Unknown"),
    IN_PROGRESS(1, "Task is currently running"),
    SUCCESS(2, "Task completed successfully"),
    RECOMMENDED(3, "Recommended as a built-in method"),
    FAILED(4, "Task failed to complete");

    @EnumValue
    @JsonValue
    private final int code;

    private final String description;

    /**
     * 根据 code 获取对应的 Status 枚举
     *
     * @param code 状态代码
     * @return 对应的 Status 枚举，如果不存在则返回 UNKNOWN
     */
    public static Status fromCode(int code) {
        for (Status status : Status.values()) {
            if (status.code == code) {
                return status;
            }
        }
        return UNKNOWN;
    }
}
