package ynu.jackielinn.server.common;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息反馈状态
 * 0: 正常（无反馈）
 * 1: 点赞
 * -1: 点踩
 */
@Getter
@AllArgsConstructor
public enum Feedback {

    NONE(0, "无反馈"),
    LIKED(1, "点赞"),
    DISLIKED(-1, "点踩");

    @EnumValue
    @JsonValue
    private final int code;

    private final String description;

    public static Feedback fromCode(int code) {
        for (Feedback f : Feedback.values()) {
            if (f.code == code) {
                return f;
            }
        }
        return NONE;
    }
}
