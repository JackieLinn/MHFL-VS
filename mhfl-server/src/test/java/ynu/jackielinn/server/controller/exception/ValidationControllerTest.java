package ynu.jackielinn.server.controller.exception;

import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ynu.jackielinn.server.common.RestResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ValidationController 单元测试：ValidationException 处理返回 400 及固定文案。
 */
class ValidationControllerTest {

    private ValidationController controller;

    @BeforeEach
    void setUp() {
        controller = new ValidationController();
    }

    @Test
    void validateErrorShouldReturn400WithFixedMessage() {
        ValidationException ex = new ValidationException("field invalid");

        RestResponse<Void> result = controller.validateError(ex);

        assertNotNull(result);
        assertEquals(400, result.code());
        assertEquals("请求参数有误", result.message());
        assertNull(result.data());
    }

    @Test
    void validateErrorShouldReturn400ForAnyValidationExceptionMessage() {
        RestResponse<Void> result = controller.validateError(new ValidationException("another message"));

        assertEquals(400, result.code());
        assertEquals("请求参数有误", result.message());
    }
}
