package ynu.jackielinn.server.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RestResponse 单元测试：静态工厂方法及 asJsonString。
 */
class RestResponseTest {

    @Test
    void successWithDataShouldReturnCode200AndMessage() {
        RestResponse<String> res = RestResponse.success("data");
        assertEquals(200, res.code());
        assertEquals("data", res.data());
        assertEquals("请求成功", res.message());
    }

    @Test
    void successWithNullDataShouldReturnCode200() {
        RestResponse<Object> res = RestResponse.success((Object) null);
        assertEquals(200, res.code());
        assertNull(res.data());
        assertEquals("请求成功", res.message());
    }

    @Test
    void successNoArgShouldReturnCode200AndNullData() {
        RestResponse<Void> res = RestResponse.success();
        assertEquals(200, res.code());
        assertNull(res.data());
        assertEquals("请求成功", res.message());
    }

    @Test
    void unauthorizedShouldReturnCode401() {
        RestResponse<Void> res = RestResponse.unauthorized("未登录");
        assertEquals(401, res.code());
        assertNull(res.data());
        assertEquals("未登录", res.message());
    }

    @Test
    void forbiddenShouldReturnCode403() {
        RestResponse<Void> res = RestResponse.forbidden("无权限");
        assertEquals(403, res.code());
        assertNull(res.data());
        assertEquals("无权限", res.message());
    }

    @Test
    void failureShouldReturnGivenCodeAndMessage() {
        RestResponse<Void> res = RestResponse.failure(400, "参数错误");
        assertEquals(400, res.code());
        assertNull(res.data());
        assertEquals("参数错误", res.message());
    }

    @Test
    void asJsonStringShouldIncludeCodeDataMessage() {
        RestResponse<String> res = RestResponse.success("hello");
        String json = res.asJsonString();
        assertTrue(json.contains("200"));
        assertTrue(json.contains("hello"));
        assertTrue(json.contains("请求成功"));
    }

    @Test
    void asJsonStringWithNullDataShouldWriteNull() {
        RestResponse<Void> res = RestResponse.success();
        String json = res.asJsonString();
        assertTrue(json.contains("null"));
    }
}
