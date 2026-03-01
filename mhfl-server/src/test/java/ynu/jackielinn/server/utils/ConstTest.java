package ynu.jackielinn.server.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Const 常量类测试，确保常量值符合预期并触发类加载以覆盖所有字段。
 */
class ConstTest {

    @Test
    void shouldExposeExpectedJwtAndCorsConstants() {
        assertEquals("jwt:blacklist:", Const.JWT_BLACK_LIST);
        assertEquals(-102, Const.ORDER_CORS);
        assertEquals(-101, Const.ORDER_LIMIT);
    }

    @Test
    void shouldExposeExpectedEmailVerifyConstants() {
        assertEquals("verify:email:limit:", Const.VERIFY_EMAIL_LIMIT);
        assertEquals("verify:email:data:", Const.VERIFY_EMAIL_DATA);
    }

    @Test
    void shouldExposeExpectedFlowLimitConstants() {
        assertEquals("flow:counter:", Const.FLOW_LIMIT_COUNTER);
        assertEquals("flow:block:", Const.FLOW_LIMIT_BLOCK);
    }

    @Test
    void shouldExposeExpectedCaptchaConstants() {
        assertEquals("verify:captcha:limit:", Const.VERIFY_CAPTCHA_LIMIT);
        assertEquals("verify:captcha:data:", Const.VERIFY_CAPTCHA_DATA);
        assertEquals("verify:captcha:fail:", Const.VERIFY_CAPTCHA_FAIL_COUNT);
    }

    @Test
    void shouldExposeExpectedTaskExperimentConstants() {
        assertEquals("task:experiment:round:", Const.TASK_EXPERIMENT_ROUND);
        assertEquals("task:experiment:client:", Const.TASK_EXPERIMENT_CLIENT);
        assertEquals("task:experiment:status:", Const.TASK_EXPERIMENT_STATUS);
    }
}
