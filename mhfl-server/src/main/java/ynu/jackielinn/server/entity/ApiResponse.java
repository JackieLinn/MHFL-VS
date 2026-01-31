package ynu.jackielinn.server.entity;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * REST API 响应封装类
 *
 * @param code
 * @param data
 * @param message
 * @param <T>
 */
@Schema(description = "HTTP状态记录对象")
public record ApiResponse<T>(int code, T data, String message) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, data, "请求成功");
    }

    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> unauthorized(String message) {
        return failure(401, message);
    }

    public static <T> ApiResponse<T> forbidden(String message) {
        return failure(403, message);
    }

    public static <T> ApiResponse<T> failure(int code, String message) {
        return new ApiResponse<>(code, null, message);
    }

    public String asJsonString() {
        return JSONObject.toJSONString(this, JSONWriter.Feature.WriteNulls);
    }
}
