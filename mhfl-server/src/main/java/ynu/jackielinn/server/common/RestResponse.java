package ynu.jackielinn.server.common;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * REST API 统一响应封装类。包含 code、data、message，与 SpringBoot/FastAPI 约定一致。
 *
 * @param code    状态码
 * @param data    响应体
 * @param message 响应信息
 * @param <T>     数据类型
 */
@Schema(description = "HTTP状态记录对象")
public record RestResponse<T>(int code, T data, String message) {

    /**
     * 成功响应，携带数据。
     *
     * @param data 响应体，可为 null
     * @param <T>  数据类型
     * @return code=200、message="请求成功" 的 RestResponse
     */
    public static <T> RestResponse<T> success(T data) {
        return new RestResponse<>(200, data, "请求成功");
    }

    /**
     * 成功响应，无数据。
     *
     * @param <T> 数据类型
     * @return code=200、data=null 的 RestResponse
     */
    public static <T> RestResponse<T> success() {
        return success(null);
    }

    /**
     * 未认证响应（401）。
     *
     * @param message 错误信息
     * @param <T>     数据类型
     * @return code=401 的 RestResponse
     */
    public static <T> RestResponse<T> unauthorized(String message) {
        return failure(401, message);
    }

    /**
     * 无权限响应（403）。
     *
     * @param message 错误信息
     * @param <T>     数据类型
     * @return code=403 的 RestResponse
     */
    public static <T> RestResponse<T> forbidden(String message) {
        return failure(403, message);
    }

    /**
     * 失败响应，指定状态码与消息。
     *
     * @param code    HTTP 状态码
     * @param message 错误信息
     * @param <T>     数据类型
     * @return data=null 的 RestResponse
     */
    public static <T> RestResponse<T> failure(int code, String message) {
        return new RestResponse<>(code, null, message);
    }

    /**
     * 将当前响应序列化为 JSON 字符串，null 字段会写出。
     *
     * @return JSON 字符串
     */
    public String asJsonString() {
        return JSONObject.toJSONString(this, JSONWriter.Feature.WriteNulls);
    }
}
