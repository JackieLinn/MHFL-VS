package ynu.jackielinn.server.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Status 枚举单元测试：fromCode、getCode、getDescription。
 */
class StatusTest {

    @Test
    void fromCodeShouldReturnCorrectStatusForEachCode() {
        assertEquals(Status.NOT_STARTED, Status.fromCode(0));
        assertEquals(Status.IN_PROGRESS, Status.fromCode(1));
        assertEquals(Status.SUCCESS, Status.fromCode(2));
        assertEquals(Status.RECOMMENDED, Status.fromCode(3));
        assertEquals(Status.FAILED, Status.fromCode(4));
        assertEquals(Status.CANCELLED, Status.fromCode(5));
    }

    @Test
    void fromCodeShouldReturnNotStartedForUnknownCode() {
        assertEquals(Status.NOT_STARTED, Status.fromCode(-1));
        assertEquals(Status.NOT_STARTED, Status.fromCode(99));
    }

    @Test
    void getCodeShouldMatchEnumValue() {
        assertEquals(0, Status.NOT_STARTED.getCode());
        assertEquals(1, Status.IN_PROGRESS.getCode());
        assertEquals(5, Status.CANCELLED.getCode());
    }

    @Test
    void getDescriptionShouldBeNonEmpty() {
        assertNotNull(Status.SUCCESS.getDescription());
        assertFalse(Status.SUCCESS.getDescription().isEmpty());
    }

    @Test
    void valuesShouldReturnAllSixStatuses() {
        Status[] values = Status.values();
        assertEquals(6, values.length);
    }
}
