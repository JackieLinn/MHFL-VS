package ynu.jackielinn.server.common;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Controller 基类
 */
public abstract class BaseController {

    /**
     * 针对返回值为 String 作为错误信息的方法进行统一处理
     *
     * @param action 具体操作
     * @return 响应结果
     */
    protected ApiResponse<Void> messageHandle(Supplier<String> action) {
        String message = action.get();
        return message == null ? ApiResponse.success() : ApiResponse.failure(400, message);
    }

    /**
     * 处理函数式接口 Function<T, String> 定义的操作，并返回 ApiResponse<Void> 类型的结果
     *
     * @param ro       请求对象
     * @param function 函数式接口，接受请求对象并返回一个字符串（null=成功，非null=错误信息）
     * @return 处理结果
     */
    protected <T> ApiResponse<Void> messageHandle(T ro, Function<T, String> function) {
        return messageHandle(() -> function.apply(ro));
    }
}
